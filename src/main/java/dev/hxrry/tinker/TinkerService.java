package dev.hxrry.tinker;

import dev.hxrry.tinker.property.PropertyResolver;
import dev.hxrry.tinker.property.TinkerProperty;
import dev.hxrry.tinker.session.PlayerSession;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.List;

public final class TinkerService {

    private static final int TARGET_RANGE = 6;

    private final TinkerPlugin plugin;

    public TinkerService(TinkerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean tinkerMode(Player player) {
        PlayerSession session = plugin.sessions().peek(player);
        return session != null && session.tinkerMode();
    }

    public boolean setTinkerMode(Player player, boolean enabled) {
        if (enabled && (!player.hasPermission(Permissions.USE) || !plugin.channel().isReady(player))) {
            return false;
        }
        PlayerSession session = plugin.sessions().get(player);
        session.tinkerMode(enabled);
        publish(player);
        return enabled;
    }

    public boolean toggleTinkerMode(Player player) {
        return setTinkerMode(player, !tinkerMode(player));
    }

    public TinkerProperty cycleSelection(Player player, boolean forward) {
        if (!player.hasPermission(Permissions.USE) || !tinkerMode(player)) {
            return null;
        }
        Block block = player.getTargetBlockExact(TARGET_RANGE);
        if (block == null || block.getType().isAir()) {
            return null;
        }
        BlockData data = block.getBlockData();
        List<TinkerProperty> editable = plugin.resolver().editable(data);
        if (editable.isEmpty()) {
            return null;
        }

        PlayerSession session = plugin.sessions().get(player);
        int current = PropertyResolver.indexOf(editable, session.selected(block.getType()));
        int step = forward ? 1 : -1;
        TinkerProperty next = editable.get(Math.floorMod(current + step, editable.size()));
        session.select(block.getType(), next.key());
        publish(player);
        return next;
    }

    public String selectedProperty(Player player, Material material) {
        PlayerSession session = plugin.sessions().peek(player);
        return session == null ? null : session.selected(material);
    }

    public void publish(Player player) {
        plugin.channel().sendState(player);
    }
}
