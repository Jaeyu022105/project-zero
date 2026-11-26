package com.projectzero.domain;

import com.projectzero.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Reawakening sea: a dark mirror floor floating in the starry void of the library
 * dimension, far from the library halls (z = {@link #Z_OFFSET}). Glowing motes hang above it,
 * each with a twin below the surface at the mirrored height, so the translucent floor reads
 * as a reflection. Built once per slot, like {@link LibraryArena}.
 */
public final class VoidSeaArena {
    private VoidSeaArena() {}

    /** Bump to rebuild existing seas after changing the layout. */
    public static final int VERSION = 1;
    public static final int Z_OFFSET = -10_000;
    public static final int SLOT_SPACING = 256;
    /** The floor block layer. The player stands on SEA_Y + 1. */
    public static final int SEA_Y = 100;
    public static final int RADIUS = 64;
    private static final int MOTES = 260;

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Where the player stands, on top of the sea. */
    public static BlockPos center(int slot) {
        return new BlockPos(slot * SLOT_SPACING, SEA_Y + 1, Z_OFFSET);
    }

    /** Height of the mirror surface (top of the floor blocks). */
    public static float mirrorY() {
        return SEA_Y + 1;
    }

    public static void ensureBuilt(ServerLevel level, int slot) {
        BlockPos c = center(slot);
        BlockPos marker = new BlockPos(c.getX(), 1, c.getZ());
        BlockState done = Blocks.LODESTONE.defaultBlockState();
        // Marker height encodes the version so old seas get rebuilt.
        BlockPos versionMarker = marker.above(VERSION);
        if (level.getBlockState(versionMarker).is(Blocks.LODESTONE)) return;
        build(level, c, RandomSource.create(slot * 53L + VERSION));
        level.setBlock(versionMarker, done, FLAGS);
    }

    private static void build(ServerLevel level, BlockPos c, RandomSource random) {
        BlockState sea = ModBlocks.VOID_SEA.get().defaultBlockState();
        int r2 = RADIUS * RADIUS;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > r2) continue;
                level.setBlock(new BlockPos(c.getX() + dx, SEA_Y, c.getZ() + dz), sea, FLAGS);
            }
        }

        // Motes above, twins below at the mirrored height. The floor top is SEA_Y + 1, so a
        // mote at SEA_Y + 1 + h reflects to SEA_Y - h (block coordinates).
        for (int i = 0; i < MOTES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 3 + Math.sqrt(random.nextDouble()) * (RADIUS - 6);
            int x = c.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = c.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int h = 1 + random.nextInt(dist < 10 ? 6 : 26);
            int size = random.nextInt(10) < 6 ? 0 : (random.nextInt(3) == 0 ? 2 : 1);
            BlockState mote = ModBlocks.VOID_MOTE.get().defaultBlockState().setValue(VoidMoteBlock.SIZE, size);
            BlockPos above = new BlockPos(x, SEA_Y + 1 + h, z);
            BlockPos below = new BlockPos(x, SEA_Y - h, z);
            if (below.getY() < level.getMinBuildHeight() + 3) continue;
            level.setBlock(above, mote, FLAGS);
            level.setBlock(below, mote, FLAGS);
        }
    }
}
