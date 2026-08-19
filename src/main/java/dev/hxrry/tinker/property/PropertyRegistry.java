package dev.hxrry.tinker.property;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Barrel;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.block.data.type.SeaPickle;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.type.TurtleEgg;
import org.bukkit.block.data.type.Wall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PropertyRegistry {

        public static final Set<Material> LIT_MATERIALS = materials("FURNACE", "BLAST_FURNACE", "SMOKER", "CAMPFIRE",
                        "SOUL_CAMPFIRE");

        private static final List<BlockFace> CONNECTING_FACES = List.of(BlockFace.NORTH, BlockFace.EAST,
                        BlockFace.SOUTH, BlockFace.WEST,
                        BlockFace.UP, BlockFace.DOWN);

        private static final List<BlockFace> WALL_FACES = List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
                        BlockFace.WEST);

        private static final List<TinkerProperty> PROPERTIES = build();

        private PropertyRegistry() {
        }

        public static List<TinkerProperty> all() {
                return PROPERTIES;
        }

        private static List<TinkerProperty> build() {
                List<TinkerProperty> properties = new ArrayList<>();

                properties.add(SimpleProperty.enums(Category.STAIRS, "shape",
                                Stairs.class, Stairs.Shape.class,
                                data -> true, Stairs::getShape, Stairs::setShape));

                properties.add(SimpleProperty.bool(Category.DOORS, "open",
                                Door.class, data -> true, Door::isOpen, Door::setOpen));
                properties.add(SimpleProperty.enums(Category.DOORS, "hinge",
                                Door.class, Door.Hinge.class,
                                data -> true, Door::getHinge, Door::setHinge));

                properties.add(SimpleProperty.bool(Category.TRAPDOORS, "open",
                                TrapDoor.class, data -> true, TrapDoor::isOpen, TrapDoor::setOpen));

                properties.add(SimpleProperty.bool(Category.FENCE_GATES, "open",
                                Gate.class, data -> true, Gate::isOpen, Gate::setOpen));

                properties.add(SimpleProperty.bool(Category.BARRELS, "open",
                                Barrel.class, data -> true, Barrel::isOpen, Barrel::setOpen));

                properties.add(SimpleProperty.bool(Category.LIT_BLOCKS, "lit",
                                Lightable.class,
                                data -> LIT_MATERIALS.contains(data.getMaterial()),
                                Lightable::isLit, Lightable::setLit));

                for (final BlockFace face : CONNECTING_FACES) {
                        properties.add(SimpleProperty.bool(Category.CONNECTING, name(face),
                                        MultipleFacing.class,
                                        data -> data.getAllowedFaces().contains(face),
                                        data -> data.hasFace(face),
                                        (data, value) -> data.setFace(face, value)));
                }

                for (final BlockFace face : WALL_FACES) {
                        properties.add(SimpleProperty.enums(Category.WALLS, name(face),
                                        Wall.class, Wall.Height.class,
                                        data -> true,
                                        data -> data.getHeight(face),
                                        (data, value) -> data.setHeight(face, value)));
                }
                properties.add(SimpleProperty.bool(Category.WALLS, "up",
                                Wall.class, data -> true, Wall::isUp, Wall::setUp));

                // op based stuff

                properties.add(SimpleProperty.ints(Category.CROPS, "age",
                                Ageable.class, data -> true,
                                data -> 0, Ageable::getMaximumAge, Ageable::getAge, Ageable::setAge));

                properties.add(SimpleProperty.ints(Category.COMPOSTERS, "level",
                                Levelled.class,
                                data -> data.getMaterial() == Material.COMPOSTER,
                                data -> 0, Levelled::getMaximumLevel, Levelled::getLevel, Levelled::setLevel));

                properties.add(SimpleProperty.ints(Category.CAULDRONS, "level",
                                Levelled.class,
                                data -> data.getMaterial().name().endsWith("CAULDRON"),
                                data -> 1, Levelled::getMaximumLevel, Levelled::getLevel, Levelled::setLevel));

                properties.add(SimpleProperty.ints(Category.BEEHIVES, "honey_level",
                                Beehive.class, data -> true,
                                data -> 0, Beehive::getMaximumHoneyLevel,
                                Beehive::getHoneyLevel, Beehive::setHoneyLevel));

                properties.add(SimpleProperty.ints(Category.CANDLES, "count",
                                Candle.class, data -> true,
                                data -> 1, Candle::getMaximumCandles, Candle::getCandles, Candle::setCandles));

                properties.add(SimpleProperty.ints(Category.SEA_PICKLES, "count",
                                SeaPickle.class, data -> true,
                                data -> 1, SeaPickle::getMaximumPickles,
                                SeaPickle::getPickles, SeaPickle::setPickles));

                properties.add(SimpleProperty.ints(Category.SNOW, "layers",
                                Snow.class, data -> true,
                                data -> 1, Snow::getMaximumLayers, Snow::getLayers, Snow::setLayers));

                properties.add(SimpleProperty.ints(Category.TURTLE_EGGS, "count",
                                TurtleEgg.class, data -> true,
                                data -> 1, TurtleEgg::getMaximumEggs, TurtleEgg::getEggs, TurtleEgg::setEggs));

                properties.add(SimpleProperty.ints(Category.RESPAWN_ANCHORS, "charges",
                                RespawnAnchor.class, data -> true,
                                data -> 0, RespawnAnchor::getMaximumCharges,
                                RespawnAnchor::getCharges, RespawnAnchor::setCharges));

                properties.add(SimpleProperty.bool(Category.PISTONS, "extended",
                                Piston.class, data -> true, Piston::isExtended, Piston::setExtended));

                // powerable

                properties.add(SimpleProperty.bool(Category.REDSTONE, "powered",
                                Powerable.class, data -> true, Powerable::isPowered, Powerable::setPowered));
                properties.add(SimpleProperty.ints(Category.REDSTONE, "power",
                                AnaloguePowerable.class, data -> true,
                                data -> 0, AnaloguePowerable::getMaximumPower,
                                AnaloguePowerable::getPower, AnaloguePowerable::setPower));
                properties.add(SimpleProperty.enums(Category.REDSTONE, "mode",
                                Comparator.class, Comparator.Mode.class,
                                data -> true, Comparator::getMode, Comparator::setMode));
                properties.add(SimpleProperty.ints(Category.REDSTONE, "delay",
                                Repeater.class, data -> true,
                                Repeater::getMinimumDelay, Repeater::getMaximumDelay,
                                Repeater::getDelay, Repeater::setDelay));

                return List.copyOf(properties);
        }

        private static String name(BlockFace face) {
                return face.name().toLowerCase(Locale.ROOT);
        }

        private static Set<Material> materials(String... names) {
                Set<Material> resolved = EnumSet.noneOf(Material.class);
                for (String name : names) {
                        Material material = Material.getMaterial(name);
                        if (material != null) {
                                resolved.add(material);
                        }
                }
                return Collections.unmodifiableSet(resolved);
        }
}
