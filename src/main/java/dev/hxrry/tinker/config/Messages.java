package dev.hxrry.tinker.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String ROOT = "messages.";

    private final Plugin plugin;
    private final FileConfiguration config;
    private final Component prefix;

    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    Messages(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.prefix = MINI_MESSAGE.deserialize(config.getString(ROOT + "prefix", ""));
    }

    public Component render(String key, TagResolver... placeholders) {
        String raw = config.getString(ROOT + key);
        if (raw == null) {
            if (warned.add(key)) {
                plugin.getLogger().warning("Missing message key 'messages." + key + "' in config.yml.");
            }
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(raw, placeholders);
    }

    public void actionBar(Player player, String key, TagResolver... placeholders) {
        player.sendActionBar(render(key, placeholders));
    }

    public void send(CommandSender recipient, String key, TagResolver... placeholders) {
        recipient.sendMessage(prefix.append(render(key, placeholders)));
    }

}
