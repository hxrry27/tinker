package dev.hxrry.tinker.session;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SessionManager implements Listener {

    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    public PlayerSession get(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), id -> new PlayerSession());
    }

    public PlayerSession peek(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void clear() {
        sessions.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }
}
