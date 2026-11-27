package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

/**
 * A lumpy boulder half sunk into the ground: two or three overlapping squashed blobs of the
 * configured block. Works on grass, sand, snow or stone, unlike vanilla's forest rock.
 */
public class BoulderFeature extends Feature<BlockStateConfiguration> {
    public BoulderFeature() {
        super(BlockStateConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();
        BlockState below = level.getBlockState(origin.below());
        if (below.isAir() || !below.getFluidState().isEmpty() || below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS)) return false;
        BlockState rock = ctx.config().state;
        int blobs = 2 + r.nextInt(2);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int b = 0; b < blobs; b++) {
            double rx = 1.2 + r.nextDouble() * 1.3, ry = rx * (0.6 + r.nextDouble() * 0.3), rz = 1.2 + r.nextDouble() * 1.3;
            double cx = (r.nextDouble() - 0.5) * 2.0, cz = (r.nextDouble() - 0.5) * 2.0, cy = -0.4 + r.nextDouble() * 0.6;
            int ix = (int) Math.ceil(rx), iy = (int) Math.ceil(ry), iz = (int) Math.ceil(rz);
            for (int dx = -ix - 1; dx <= ix + 1; dx++) for (int dy = -iy; dy <= iy; dy++) for (int dz = -iz - 1; dz <= iz + 1; dz++) {
                double fx = (dx - cx) / rx, fy = (dy - cy) / ry, fz = (dz - cz) / rz;
                if (fx * fx + fy * fy + fz * fz > 1.0) continue;
                p.setWithOffset(origin, dx, dy, dz);
                BlockState cur = level.getBlockState(p);
                if (cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.DIRT) || cur.is(BlockTags.SAND)) level.setBlock(p, rock, 2);
            }
        }
        return true;
    }
}
