package dev.hxrry.tinker.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;

import dev.hxrry.tinker.Permissions;
import dev.hxrry.tinker.TinkerPlugin;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.logging.Level;

// no real commands, just a reload command for admin config stuff, it's all keybind derived
public final class TinkerCommand {

    private final TinkerPlugin plugin;

    public TinkerCommand(TinkerPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(TinkerPlugin plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            event.registrar().register(build(), "reload Tinker's config", List.of("tk")));
    }

    private LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("tinker")
            .requires(source -> source.getSender().hasPermission(Permissions.RELOAD))
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    reload(ctx.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                }))
            .build();
    }

    private void reload(CommandSender sender) {
        try {
            plugin.reload();
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload Tinker's config.", e);
            plugin.config().messages().send(sender, "reload-failed");
            return;
        }
        plugin.config().messages().send(sender, "reloaded");
    }
}
