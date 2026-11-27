package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Cloud Forest: a tree fern. A slim, shaggy trunk 4 to 8 tall (jungle wood, moss at the foot), and a
 * crown of 6 to 9 long fronds that arch up and out and droop at the tips. Stays within about 6
 * blocks of the origin.
 */
public class TreeFernFeature extends Feature<NoneFeatureConfiguration> {
    public TreeFernFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        if (!level.getBlockState(o.below()).is(BlockTags.DIRT)) return false;
        int h = 4 + r.nextInt(5);
        for (int y = 0; y < h; y++) {
            BlockPos p = o.above(y);
            if (!(level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced())) return y > 0;
            level.setBlock(p, (y == 0 ? Blocks.STRIPPED_JUNGLE_WOOD : Blocks.JUNGLE_WOOD).defaultBlockState(), 2);
        }
        level.setBlock(o.below(), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        BlockPos top = o.above(h);
        BlockState leaf = (r.nextBoolean() ? Blocks.JUNGLE_LEAVES : Blocks.AZALEA_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        put(level, top, leaf);
        int fronds = 6 + r.nextInt(4);
        for (int i = 0; i < fronds; i++) {
            double a = i * Math.PI * 2 / fronds + r.nextDouble() * 0.4;
            int len = 3 + r.nextInt(3);
            for (int k = 1; k <= len; k++) {
                double t = k / (double) len;
                // Rises at first, then droops: a shallow arch.
                double y = 1.2 * Math.sin(Math.PI * t * 0.8) - (t > 0.7 ? (t - 0.7) * 4 : 0);
                BlockPos p = top.offset((int) Math.round(Math.cos(a) * k), (int) Math.round(y), (int) Math.round(Math.sin(a) * k));
                put(level, p, leaf);
            }
        }
        return true;
    }

    private static void put(WorldGenLevel level, BlockPos p, BlockState s) {
        BlockState cur = level.getBlockState(p);
        if (cur.isAir() || cur.is(BlockTags.LEAVES) || cur.canBeReplaced()) level.setBlock(p, s, 2);
    }
}
