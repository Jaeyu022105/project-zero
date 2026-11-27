package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A termite mound: a lumpy earth chimney 3 to 8 blocks tall, sometimes with a second spire. */
public class TermiteMoundFeature extends Feature<NoneFeatureConfiguration> {
    public TermiteMoundFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos base = ctx.origin();
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) return false;
        spire(level, base, 3 + r.nextInt(6), 1.6 + r.nextDouble(), r);
        if (r.nextBoolean()) spire(level, base.offset(r.nextInt(3) - 1, 0, r.nextInt(3) - 1), 2 + r.nextInt(4), 1.1, r);
        return true;
    }

    private static void spire(WorldGenLevel level, BlockPos base, int h, double r0, RandomSource r) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = -1; y < h; y++) {
            double rad = r0 * (1 - Math.max(0, y) / (double) (h + 1)) + 0.3;
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                if (dx * dx + dz * dz > rad * rad + r.nextDouble() * 0.6) continue;
                p.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
                BlockState cur = level.getBlockState(p);
                if (!(cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.DIRT))) continue;
                float f = r.nextFloat();
                level.setBlock(p, (f < 0.5F ? Blocks.TERRACOTTA : f < 0.8F ? Blocks.COARSE_DIRT : Blocks.PACKED_MUD).defaultBlockState(), 2);
            }
        }
    }
}
