package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Bloodmoon Steppe: a dead, leafless tree with crooked reaching branches, sometimes snapped off short. */
public class DeadTreeFeature extends Feature<NoneFeatureConfiguration> {
    public DeadTreeFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        if (!level.getBlockState(o.below()).is(BlockTags.DIRT)) return false;
        BlockState log = (r.nextBoolean() ? Blocks.DARK_OAK_LOG : Blocks.STRIPPED_DARK_OAK_LOG).defaultBlockState();
        boolean snapped = r.nextFloat() < 0.25F;
        int h = snapped ? 2 + r.nextInt(3) : 6 + r.nextInt(7);
        BlockPos.MutableBlockPos p = o.mutable();
        for (int y = 0; y < h; y++) {
            if (y > 2 && r.nextFloat() < 0.2F) p.move(Direction.Plane.HORIZONTAL.getRandomDirection(r));
            set(level, p, log);
            p.move(Direction.UP);
        }
        if (snapped) return true;
        int branches = 3 + r.nextInt(3);
        for (int b = 0; b < branches; b++) {
            BlockPos.MutableBlockPos q = o.above(h / 2 + r.nextInt(Math.max(1, h / 2))).mutable();
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            int len = 2 + r.nextInt(4);
            for (int k = 0; k < len; k++) {
                q.move(d);
                if (r.nextFloat() < 0.5F) q.move(Direction.UP);
                set(level, q, log.setValue(RotatedPillarBlock.AXIS, d.getAxis()));
            }
        }
        return true;
    }

    private static void set(WorldGenLevel level, BlockPos p, BlockState s) {
        BlockState cur = level.getBlockState(p);
        if (cur.isAir() || cur.canBeReplaced()) level.setBlock(p, s, 2);
    }
}
