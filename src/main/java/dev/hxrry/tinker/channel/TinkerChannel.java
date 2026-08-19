package dev.hxrry.tinker.channel;

import dev.hxrry.tinker.Permissions;
import dev.hxrry.tinker.TinkerPlugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TinkerChannel implements PluginMessageListener, Listener {

    public static final String CHANNEL = "tinker:main";

    public static final int PROTOCOL_VERSION = 1;

    private static final byte HELLO = 0x01;
    private static final byte TOGGLE_TINKER = 0x02;
    private static final byte CYCLE_PROPERTY = 0x03;

    private static final byte HELLO_ACK = (byte) 0x81;
    private static final byte STATE = (byte) 0x82;

    private static final byte MODE_OFF = 0;
    private static final byte MODE_ON = 1;
    private static final byte MODE_TOGGLE = 2;

    private static final int MAX_MESSAGES_PER_SECOND = 20;
    private static final int MAX_PAYLOAD_BYTES = 64;
    private static final int TARGET_RANGE = 6;

    private final TinkerPlugin plugin;

    private final Map<UUID, Boolean> handshaken = new HashMap<>();

    private final Map<UUID, RateLimit> limits = new HashMap<>();

    public TinkerChannel(TinkerPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        var messenger = plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void unregister() {
        var messenger = plugin.getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL);
        handshaken.clear();
        limits.clear();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        if (message.length == 0 || message.length > MAX_PAYLOAD_BYTES) {
            reject(player, "payload length " + message.length);
            return;
        }
        if (!limit(player).allow()) {
            reject(player, "rate limit");
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            switch (in.readByte()) {
                case HELLO -> handleHello(player, in.readInt());
                case TOGGLE_TINKER -> handleToggle(player, in.readByte());
                case CYCLE_PROPERTY -> handleCycle(player, in.readByte());
                default -> reject(player, "unknown message id");
            }
        } catch (IOException | RuntimeException e) {
            reject(player, "malformed payload: " + e.getClass().getSimpleName());
        }
    }

    private void handleHello(Player player, int clientVersion) {
        boolean supported = clientVersion == PROTOCOL_VERSION;
        handshaken.put(player.getUniqueId(), supported);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(HELLO_ACK);
            out.writeInt(PROTOCOL_VERSION);
            out.writeBoolean(supported);
        } catch (IOException e) {
            return;
        }
        player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());

        if (supported) {
            sendState(player);
        } else {
            plugin.getLogger().fine(() -> "Tinker client v" + clientVersion + " from "
                    + player.getName() + " is not supported (server speaks v" + PROTOCOL_VERSION + ")");
        }
    }

    // permission is re-checked here rather than trusted from the client's last known state
    private void handleToggle(Player player, byte mode) {
        if (!isReady(player) || !player.hasPermission(Permissions.USE)) {
            sendState(player);
            return;
        }
        switch (mode) {
            case MODE_OFF -> plugin.service().setTinkerMode(player, false);
            case MODE_ON -> plugin.service().setTinkerMode(player, true);
            case MODE_TOGGLE -> plugin.service().toggleTinkerMode(player);
            default -> reject(player, "unknown toggle mode " + mode);
        }
    }

    private void handleCycle(Player player, byte direction) {
        if (!isReady(player) || !player.hasPermission(Permissions.USE)) {
            sendState(player);
            return;
        }
        if (direction != 0 && direction != 1) {
            reject(player, "unknown cycle direction " + direction);
            return;
        }
        plugin.service().cycleSelection(player, direction == 1);
    }

    public void sendState(Player player) {
        if (!isReady(player)) {
            return;
        }
        String selected = selectedProperty(player);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(STATE);
            out.writeBoolean(plugin.service().tinkerMode(player));
            out.writeBoolean(selected != null);
            if (selected != null) {
                out.writeUTF(selected);
            }
            out.writeBoolean(player.hasPermission(Permissions.USE));
        } catch (IOException e) {
            return;
        }
        player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
    }

    private String selectedProperty(Player player) {
        Block block = player.getTargetBlockExact(TARGET_RANGE);
        if (block == null || block.getType().isAir()) {
            return null;
        }
        String key = plugin.service().selectedProperty(player, block.getType());
        if (key == null) {
            return null;
        }
        int dot = key.indexOf('.');
        return dot < 0 ? key : key.substring(dot + 1);
    }

    // yoinked from armor poser
    public boolean isReady(Player player) {
        return Boolean.TRUE.equals(handshaken.get(player.getUniqueId()));
    }

    private RateLimit limit(Player player) {
        return limits.computeIfAbsent(player.getUniqueId(), id -> new RateLimit());
    }

    private void reject(Player player, String reason) {
        plugin.getLogger().fine(() -> "Rejected " + CHANNEL + " message from "
                + player.getName() + ": " + reason);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handshaken.remove(event.getPlayer().getUniqueId());
        limits.remove(event.getPlayer().getUniqueId());
    }

    private static final class RateLimit {
        private long windowStart;
        private int count;

        boolean allow() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 1000L) {
                windowStart = now;
                count = 0;
            }
            return ++count <= MAX_MESSAGES_PER_SECOND;
        }
    }
}
