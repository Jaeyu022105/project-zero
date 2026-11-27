package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Sunken Ruins Marsh: an abandoned fisher's hut on stilts. Mangrove log stilts sunk into the mud, a
 * plank floor about three blocks above the water, walls with gaps where boards rotted away, a
 * sagging stair roof with holes, a lantern, and a jetty of slabs. Stays within about 7 blocks of the
 * origin.
 */
public class StiltHutFeature extends Feature<NoneFeatureConfiguration> {
    public StiltHutFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int water = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        int floorY = water + 2 + r.nextInt(2);
        int hw = 2;   // half width: a 5x5 hut
        BlockState plank = Blocks.MANGROVE_PLANKS.defaultBlockState();
        BlockState stilt = Blocks.MANGROVE_LOG.defaultBlockState();
        // Stilts at the corners, down into the mud.
        for (int sx = -hw; sx <= hw; sx += 2 * hw) for (int sz = -hw; sz <= hw; sz += 2 * hw) {
            int x = o.getX() + sx, z = o.getZ() + sz;
            int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            if (floorY - ground > 10) return false;   // too deep, skip the hut
            for (int y = ground - 1; y < floorY + 3; y++) level.setBlock(new BlockPos(x, y, z), stilt, 2);
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -hw; dx <= hw; dx++) for (int dz = -hw; dz <= hw; dz++) {
            boolean corner = Math.abs(dx) == hw && Math.abs(dz) == hw;
            boolean wall = Math.abs(dx) == hw || Math.abs(dz) == hw;
            p.set(o.getX() + dx, floorY, o.getZ() + dz);
            if (!corner && r.nextFloat() > 0.08F) level.setBlock(p, plank, 2);   // a rotten board here and there
            if (wall && !corner) for (int y = 1; y <= 2; y++) {
                p.set(o.getX() + dx, floorY + y, o.getZ() + dz);
                boolean door = dz == hw && dx == 0;
                if (!door && r.nextFloat() > 0.2F) level.setBlock(p, plank, 2);
            }
        }
        // Roof: stairs sloping down to the sides along X, with holes.
        for (int dx = -hw - 1; dx <= hw + 1; dx++) for (int dz = -hw - 1; dz <= hw + 1; dz++) {
            if (r.nextFloat() < 0.12F) continue;
            int rise = hw + 1 - Math.abs(dx);
            p.set(o.getX() + dx, floorY + 3 + rise / 2, o.getZ() + dz);
            if (!level.getBlockState(p).isAir()) continue;
            BlockState s = dx == 0 ? Blocks.MANGROVE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                    : Blocks.MANGROVE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, dx < 0 ? Direction.EAST : Direction.WEST);
            level.setBlock(p, s, 2);
        }
        // A lantern inside, and a jetty out the door.
        level.setBlock(o.above(floorY + 1 - o.getY()), Blocks.LANTERN.defaultBlockState(), 2);
        int len = 3 + r.nextInt(4);
        for (int k = hw + 1; k <= hw + len; k++) {
            p.set(o.getX(), floorY, o.getZ() + k);
            if (level.getBlockState(p).isAir()) level.setBlock(p, Blocks.MANGROVE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), 2);
            if (k == hw + len) {
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, p.getX(), p.getZ());
                for (int y = ground - 1; y < floorY; y++) level.setBlock(new BlockPos(p.getX(), y, p.getZ()), Blocks.MANGROVE_FENCE.defaultBlockState(), 2);
            }
        }
        return true;
    }
}
