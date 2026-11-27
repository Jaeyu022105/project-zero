package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Small loose clusters of floating stone fragments at Skyfall Isles island heights. */
public class SkyFragmentFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 16;

    public SkyFragmentFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();
        BlockPos centre = origin.offset(r.nextInt(11) - 5, 35 + r.nextInt(76), r.nextInt(11) - 5);
        int fragments = 3 + r.nextInt(6);
        for (int i = 0; i < fragments; i++) {
            BlockPos at = centre.offset(r.nextInt(9) - 4, r.nextInt(7) - 3, r.nextInt(9) - 4);
            int size = 1 + r.nextInt(4);
            if (at.getY() < level.getMinBuildHeight() || at.getY() + 1 >= level.getMaxBuildHeight()) continue;
            BlockState rock = r.nextFloat() < 0.5F ? Blocks.STONE.defaultBlockState()
                    : r.nextBoolean() ? Blocks.ANDESITE.defaultBlockState() : Blocks.TUFF.defaultBlockState();
            BlockPos previous = at;
            for (int k = 0; k < size; k++) {
                BlockPos pos = k == 0 ? at : previous.relative(net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(r));
                if (Math.abs(pos.getX() - origin.getX()) > REACH || Math.abs(pos.getZ() - origin.getZ()) > REACH) break;
                if (level.getBlockState(pos).isAir()) level.setBlock(pos, rock, 2);
                previous = pos;
            }
            // A little soil and cover on some fragments suggests broken pieces of larger isles.
            if (r.nextFloat() < 0.35F) {
                BlockPos top = previous.above();
                if (!level.getBlockState(previous).isAir()
                        && Math.abs(top.getX() - origin.getX()) <= REACH && Math.abs(top.getZ() - origin.getZ()) <= REACH
                        && level.getBlockState(top).isAir())
                    level.setBlock(top, r.nextBoolean() ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        return true;
    }
}
