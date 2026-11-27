package com.projectzero.worldgen;

import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A baobab: a fat bottle-shaped trunk 12 to 22 blocks tall that swells in the middle, flares into
 * the ground at the base, and ends in a flat crown of short, crooked branches with leaf pads.
 */
public class BaobabFeature extends Feature<NoneFeatureConfiguration> {
    public BaobabFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos base = ctx.origin();
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) return false;
        int h = 12 + r.nextInt(11);
        double r0 = 2.0 + r.nextDouble() * 1.4;
        BlockState log = ModNature.BAOBAB_LOG.get().defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = -3; y <= h; y++) {
            double t = Math.max(0, y) / (double) h;
            // Bottle: swells to about 1.35x a third of the way up, narrows toward the crown.
            double rad = r0 * (1 + 0.38 * Math.sin(Math.PI * Math.min(1, t * 1.3))) * (1 - 0.35 * t);
            if (y <= 1) rad += 0.8 - y * 0.25;
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                if (dx * dx + dz * dz > rad * rad) continue;
                p.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
                BlockState cur = level.getBlockState(p);
                if (y < 0 && !cur.is(BlockTags.DIRT) && !cur.isAir() && !cur.canBeReplaced()) continue;
                if (y >= 0 && !(cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.LEAVES))) continue;
                level.setBlock(p, log, 2);
            }
        }
        BlockState leaves = Blocks.ACACIA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockPos top = base.above(h);
        int branches = 5 + r.nextInt(4);
        for (int i = 0; i < branches; i++) {
            double ang = i * (Math.PI * 2 / branches) + r.nextDouble() * 0.6;
            int len = 3 + r.nextInt(5);
            double x = 0, z = 0, y = 0;
            BlockPos end = top;
            for (int k = 0; k < len; k++) {
                x += Math.cos(ang); z += Math.sin(ang);
                if (r.nextFloat() < 0.45F) y += 1;
                end = top.offset((int) Math.round(x), (int) y, (int) Math.round(z));
                Direction.Axis axis = Math.abs(Math.cos(ang)) > Math.abs(Math.sin(ang)) ? Direction.Axis.X : Direction.Axis.Z;
                if (level.getBlockState(end).isAir() || level.getBlockState(end).is(BlockTags.LEAVES))
                    level.setBlock(end, log.setValue(RotatedPillarBlock.AXIS, axis), 2);
            }
            // Flat leaf pad on the branch tip.
            int pr = 1 + r.nextInt(2);
            for (int dx = -pr - 1; dx <= pr + 1; dx++) for (int dz = -pr - 1; dz <= pr + 1; dz++) for (int dy = 0; dy <= 1; dy++) {
                if (dx * dx + dz * dz > (pr + 0.6) * (pr + 0.6) + dy * 3) continue;
                BlockPos lp = end.offset(dx, dy, dz);
                if (level.getBlockState(lp).isAir()) level.setBlock(lp, leaves, 2);
            }
        }
        return true;
    }
}
