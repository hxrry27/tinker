package dev.hxrry.tinker.listener;

import dev.hxrry.tinker.Permissions;
import dev.hxrry.tinker.TinkerPlugin;
import dev.hxrry.tinker.config.Messages;
import dev.hxrry.tinker.config.TinkerConfig;
import dev.hxrry.tinker.property.Category;
import dev.hxrry.tinker.property.PropertyResolver;
import dev.hxrry.tinker.property.TinkerProperty;
import dev.hxrry.tinker.session.PlayerSession;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class TinkerToolListener implements Listener {

    private final TinkerPlugin plugin;

    public TinkerToolListener(TinkerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType().isAir()) {
            return;
        }

        Player player = event.getPlayer();
        if (!isToolActive(player)) {
            return;
        }

        event.setCancelled(true);

        BlockData data = block.getBlockData();
        Messages messages = plugin.config().messages();

        if (isSpatial(data)) {
            if (action == Action.RIGHT_CLICK_BLOCK) {
                editFace(player, block, data, event, messages);
            }
            return;
        }

        List<TinkerProperty> editable = plugin.resolver().editable(data);
        if (editable.isEmpty()) {
            messages.actionBar(player, "not-editable", blockName(block.getType()));
            return;
        }

        PlayerSession session = plugin.sessions().get(player);
        if (action == Action.LEFT_CLICK_BLOCK) {
            select(player, session, block, data, editable, messages);
        } else {
            cycle(player, session, block, data, editable, messages);
        }
    }

    // blocks whose state maps onto the block's geometry, so you click the thing you want
    private static boolean isSpatial(BlockData data) {
        return data instanceof MultipleFacing || data instanceof Wall || data instanceof Stairs;
    }

    private void editFace(Player player, Block block, BlockData data, PlayerInteractEvent event,
            Messages messages) {
        Location hit = event.getInteractionPoint();

        // stairs are set rather than cycled - tried cycle for a little and it felt shit
        if (data instanceof Stairs stairs) {
            editStairs(player, block, stairs, event.getBlockFace(), hit, messages);
            return;
        }

        TinkerProperty property = hit == null
                ? faceProperty(data, event.getBlockFace())          
                : hitProperty(data, event.getBlockFace(),
                        hit.getX() - block.getX(),
                        hit.getY() - block.getY(),
                        hit.getZ() - block.getZ());
        if (property == null) {
            return;
        }
        apply(player, block, data, property, messages);
    }

    private void editStairs(Player player, Block block, Stairs data, BlockFace clicked,
            Location hit, Messages messages) {
        if (!plugin.config().isCategoryEnabled(Category.STAIRS) || hit == null) {
            return;
        }
        BlockData updated = StairVoxel.toggle(data, block.getType(), block.getLocation(), clicked,
                hit.getX() - block.getX(), hit.getY() - block.getY(), hit.getZ() - block.getZ());
        if (updated == null) {
            return;     
        }
        Stairs stairs = (Stairs) updated;
        write(player, block, updated, "stairs",
                stairs.getFacing().name() + "/" + stairs.getHalf().name()
                        + "/" + stairs.getShape().name(), messages);
    }

    // a fence post is 4px wide, so the middle eighth of the top face is post, not arm
    private static final double POST_RADIUS = 0.125D;
    // a wall post is wider, and its centre is the `up` pillar rather than a dead zone
    private static final double WALL_POST_RADIUS = 0.25D;

    private TinkerProperty hitProperty(BlockData data, BlockFace clicked,
            double relX, double relY, double relZ) {
        double offX = Math.abs(relX - 0.5D);
        double offY = Math.abs(relY - 0.5D);
        double offZ = Math.abs(relZ - 0.5D);
        boolean fromTopOrBottom = clicked == BlockFace.UP || clicked == BlockFace.DOWN;

        if (data instanceof Wall) {
            if (fromTopOrBottom && Math.max(offX, offZ) < WALL_POST_RADIUS) {
                return plugin.resolver().find(data, Category.WALLS, "up");
            }
            return plugin.resolver().find(data, Category.WALLS, horizontal(offX, offZ, relX, relZ));
        }

        MultipleFacing facing = (MultipleFacing) data;
        if (facing.getAllowedFaces().contains(BlockFace.UP)) {
            // connects vertically too (chorus plant, mushroom stem) - all three axes compete ffs
            if (offY > offX && offY > offZ) {
                return plugin.resolver().find(data, Category.CONNECTING, relY > 0.5D ? "up" : "down");
            }
            return plugin.resolver().find(data, Category.CONNECTING, horizontal(offX, offZ, relX, relZ));
        }
        if (fromTopOrBottom && Math.max(offX, offZ) < POST_RADIUS) {
            return null;
        }
        return plugin.resolver().find(data, Category.CONNECTING, horizontal(offX, offZ, relX, relZ));
    }

    // walls with no side or post look dumb so no have those
    private static void sanitise(BlockData data) {
        if (!(data instanceof Wall wall) || wall.isUp()) {
            return;
        }
        for (BlockFace face : WALL_FACES) {
            if (wall.getHeight(face) != Wall.Height.NONE) {
                return;
            }
        }
        wall.setUp(true);
    }

    private static final List<BlockFace> WALL_FACES =
            List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    private static String horizontal(double offX, double offZ, double relX, double relZ) {
        if (offX > offZ) {
            return relX > 0.5D ? "east" : "west";
        }
        return relZ > 0.5D ? "south" : "north";
    }

    private TinkerProperty faceProperty(BlockData data, BlockFace face) {
        Category category = data instanceof Wall ? Category.WALLS : Category.CONNECTING;
        if (face == BlockFace.UP && data instanceof Wall) {
            return plugin.resolver().find(data, Category.WALLS, "up");
        }
        if (!face.isCartesian() || face.getModY() != 0) {
            return null;
        }
        return plugin.resolver().find(data, category, face.name().toLowerCase(Locale.ROOT));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isToolActive(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private void select(Player player,
            PlayerSession session,
            Block block,
            BlockData data,
            List<TinkerProperty> editable,
            Messages messages) {
        int current = PropertyResolver.indexOf(editable, session.selected(block.getType()));

        TinkerProperty next = editable.get(Math.floorMod(current + 1, editable.size()));
        session.select(block.getType(), next.key());

        plugin.service().publish(player);
        messages.actionBar(player, "selected",
                blockName(block.getType()),
                Placeholder.unparsed("property", next.id()),
                Placeholder.unparsed("value", next.render(data)));
    }

    private void cycle(Player player,
            PlayerSession session,
            Block block,
            BlockData data,
            List<TinkerProperty> editable,
            Messages messages) {
        int index = PropertyResolver.indexOf(editable, session.selected(block.getType()));
        TinkerProperty property = editable.get(index < 0 ? 0 : index);
        if (index < 0) {
            session.select(block.getType(), property.key());
        }
        apply(player, block, data, property, messages);
    }

    private void apply(Player player, Block block, BlockData data, TinkerProperty property,
            Messages messages) {
        BlockData updated;
        try {
            updated = property.cycle(data, 1);
        } catch (IllegalArgumentException e) {

            plugin.getLogger().log(Level.FINE, "Refused an invalid value for " + property.key(), e);
            messages.actionBar(player, "not-editable", blockName(block.getType()));
            return;
        }

        write(player, block, updated, property.id(), property.render(updated), messages);
    }

    // the single write path for every tinker edit, cycled or set
    private void write(Player player, Block block, BlockData updated,
            String property, String value, Messages messages) {
        sanitise(updated);
        block.setBlockData(updated, false);
        messages.actionBar(player, "cycled",
                blockName(block.getType()),
                Placeholder.unparsed("property", property),
                Placeholder.unparsed("value", value));
    }

    private boolean isToolActive(Player player) {
        PlayerSession session = plugin.sessions().peek(player);
        if (session == null || !session.tinkerMode()) {
            return false;
        }
        // tinker mode can only be on via the mod, but re-check rather than assume
        if (!plugin.channel().isReady(player)) {
            return false;
        }
        if (!player.hasPermission(Permissions.USE)) {
            return false;
        }
        TinkerConfig config = plugin.config();
        if (!config.requireToolItem()) {
            return true;
        }
        return player.getInventory().getItemInMainHand().getType() == config.toolItem();
    }

    private static TagResolver blockName(Material material) {
        return Placeholder.unparsed("block", material.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }
}
