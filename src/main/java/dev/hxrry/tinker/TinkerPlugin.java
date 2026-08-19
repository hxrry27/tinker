package dev.hxrry.tinker;

import dev.hxrry.tinker.channel.TinkerChannel;
import dev.hxrry.tinker.command.TinkerCommand;
import dev.hxrry.tinker.config.TinkerConfig;
import dev.hxrry.tinker.listener.TinkerToolListener;
import dev.hxrry.tinker.property.PropertyResolver;
import dev.hxrry.tinker.session.SessionManager;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public final class TinkerPlugin extends JavaPlugin {

    private TinkerConfig config;
    private SessionManager sessions;
    private PropertyResolver resolver;
    private TinkerService service;
    private TinkerChannel channel;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        config = TinkerConfig.load(this);

        sessions = new SessionManager();
        resolver = new PropertyResolver(this::config);
        service = new TinkerService(this);
        channel = new TinkerChannel(this);

        getServer().getPluginManager().registerEvents(sessions, this);
        getServer().getPluginManager().registerEvents(new TinkerToolListener(this), this);
        getServer().getPluginManager().registerEvents(channel, this);

        channel.register();
        registerPermissions();
        new TinkerCommand(this).register(this);
    }

    // here because i chose paper plugin yml, don't start thinking you're crazy 
    private void registerPermissions() {
        var pm = getServer().getPluginManager();
        for (var perm : new Permission[] {
            new Permission(Permissions.USE, PermissionDefault.FALSE),
            new Permission(Permissions.RELOAD, PermissionDefault.FALSE)
        }) {
            if (pm.getPermission(perm.getName()) == null) {
                pm.addPermission(perm);   // something something survives reloads nice 6 hour debug ya weeb
            }
        }
    }

    @Override
    public void onDisable() {
        if (channel != null) {
            channel.unregister();
        }
        if (sessions != null) {
            sessions.clear();
        }
    }

    public void reload() {
        reloadConfig();
        config = TinkerConfig.load(this);
        sessions.clear();
        getServer().getOnlinePlayers().forEach(channel::sendState);
    }

    public TinkerConfig config() {
        return config;
    }

    public SessionManager sessions() {
        return sessions;
    }

    public PropertyResolver resolver() {
        return resolver;
    }

    public TinkerService service() {
        return service;
    }

    public TinkerChannel channel() {
        return channel;
    }
}
