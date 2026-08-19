package dev.hxrry.tinker.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;

// stair editing as voxel sculpting, moulberry inspired 2x2x2 grid of cubes to carve from

// only stair states go in the lookup table, so it can never become slab or block
final class StairVoxel {

    private static final double NEAR = 0.25D;
    private static final double FAR = 0.75D;

    private static final int BITS = 8;

    private static final Map<Material, Map<Integer, BlockData>> CACHE = new HashMap<>();

    private StairVoxel() {
    }

    static BlockData toggle(Stairs current, Material material, Location at, BlockFace clicked,
                            double relX, double relY, double relZ) {
        Map<Integer, BlockData> patterns = patterns(material, at);
        int voxel = pattern(current, at);

        BlockData carved = patterns.get(voxel & ~(1 << octant(clicked, relX, relY, relZ, -1)));
        if (carved != null) {
            return preserve(current, carved);
        }
        // nothing valid to remove, so grow into the octant on the other side of the face
        BlockData grown = patterns.get(voxel | (1 << octant(clicked, relX, relY, relZ, 1)));
        return grown == null ? null : preserve(current, grown);
    }

    private static int octant(BlockFace clicked, double relX, double relY, double relZ, int sign) {
        double nudgeX = clicked.getModX() * 0.25D * sign;
        double nudgeY = clicked.getModY() * 0.25D * sign;
        double nudgeZ = clicked.getModZ() * 0.25D * sign;

        int index = 0;
        if (relZ + nudgeZ <= 0.5D) {
            index |= 1;
        }
        if (relX + nudgeX <= 0.5D) {
            index |= 2;
        }
        if (relY + nudgeY <= 0.5D) {
            index |= 4;
        }
        return index;
    }

    // hopefully retains waterlogging status (felt finnicky, maybe review)
    private static BlockData preserve(Stairs from, BlockData to) {
        BlockData result = to.clone();
        if (from instanceof Waterlogged wet && result instanceof Waterlogged dry) {
            dry.setWaterlogged(wet.isWaterlogged());
        }
        return result;
    }

    // definitive state list of all stair states (i think - if bug its probably here)
    private static Map<Integer, BlockData> patterns(Material material, Location at) {
        Map<Integer, BlockData> cached = CACHE.get(material);
        if (cached != null) {
            return cached;
        }

        Map<Integer, BlockData> built = new HashMap<>();
        for (BlockFace facing : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST,
                BlockFace.SOUTH, BlockFace.WEST}) {
            for (Bisected.Half half : Bisected.Half.values()) {
                for (Stairs.Shape shape : Stairs.Shape.values()) {
                    Stairs candidate = (Stairs) material.createBlockData();
                    candidate.setFacing(facing);
                    candidate.setHalf(half);
                    candidate.setShape(shape);
                    built.putIfAbsent(pattern(candidate, at), candidate);
                }
            }
        }
        CACHE.put(material, Map.copyOf(built));
        return CACHE.get(material);
    }

    private static int pattern(BlockData data, Location at) {
        var boxes = data.getCollisionShape(at).getBoundingBoxes();
        int voxel = 0;
        for (int index = 0; index < BITS; index++) {
            double x = (index & 2) != 0 ? NEAR : FAR;
            double y = (index & 4) != 0 ? NEAR : FAR;
            double z = (index & 1) != 0 ? NEAR : FAR;
            for (BoundingBox box : boxes) {
                if (box.contains(x, y, z)) {
                    voxel |= 1 << index;
                    break;
                }
            }
        }
        return voxel;
    }
}
