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

/**
 * Baobab Savanna: a kopje, a hill of huge rounded granite boulders piled on each other, 6 to 15
 * blocks tall, with a few balanced on top, grass and shrubs in the cracks and a dusty skirt of
 * coarse dirt. Stays within about 10 blocks of the origin.
 */
public class KopjeFeature extends Feature<NoneFeatureConfiguration> {
    public KopjeFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        if (!level.getBlockState(o.below()).is(BlockTags.DIRT)) return false;
        // Base layer: three to five big boulders around the middle, sunk a little.
        int base = 3 + r.nextInt(3);
        double topY = 0;
        for (int i = 0; i < base; i++) {
            double ang = i * Math.PI * 2 / base + r.nextDouble() * 0.6;
            double d = 2 + r.nextDouble() * 2.5;
            double rad = 2.6 + r.nextDouble() * 1.6;
            // Sit each base boulder on the ground under it, so on a slope none hangs over the drop.
            BlockPos at = o.offset((int) Math.round(Math.cos(ang) * d), 0, (int) Math.round(Math.sin(ang) * d));
            int dy = groundOffset(level, at);
            boulder(level, at.above(Math.min(dy, 0) - 1), rad, rad * 0.8, r);
            topY = Math.max(topY, rad * 0.8 * 2 - 2);
        }
        // Middle: one or two big ones riding on the base.
        int mid = 1 + r.nextInt(2);
        double y = topY * 0.75;
        for (int i = 0; i < mid; i++) {
            double rad = 2.4 + r.nextDouble() * 1.4;
            boulder(level, o.offset(r.nextInt(3) - 1, (int) y, r.nextInt(3) - 1), rad, rad * 0.85, r);
            y += rad * 0.85 * 1.4;
        }
        // Sometimes a smaller rock balanced on the very top.
        if (r.nextFloat() < 0.6F) boulder(level, o.offset(r.nextInt(3) - 1, (int) y + 1, r.nextInt(3) - 1), 1.3 + r.nextDouble(), 1.2, r);
        // Coarse dirt skirt, grass and a shrub or two in the cracks.
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) {
            if (dx * dx + dz * dz > 81 || r.nextFloat() > 0.35F) continue;
            for (int dy = (int) y + 4; dy >= -3; dy--) {
                p.set(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
                BlockState below = level.getBlockState(p.below());
                if (!level.getBlockState(p).isAir()) continue;
                if (below.is(Blocks.GRASS_BLOCK) && dy <= 1) { level.setBlock(p.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2); break; }
                if (isRock(below) && dy > 1) {
                    float f = r.nextFloat();
                    if (f < 0.12F) level.setBlock(p, Blocks.ACACIA_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 2);
                    else if (f < 0.4F) {
                        level.setBlock(p.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
                        level.setBlock(p, Blocks.SHORT_GRASS.defaultBlockState(), 2);
                    }
                    break;
                }
                if (!below.isAir()) break;
            }
        }
        return true;
    }

    /** How far the ground under this column is above (+) or below (-) the origin's ground, from -6 to +4. */
    private static int groundOffset(WorldGenLevel level, BlockPos at) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = 4; dy >= -6; dy--) {
            p.set(at.getX(), at.getY() + dy - 1, at.getZ());
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.TERRACOTTA)) return dy;
        }
        return -6;
    }

    private static boolean isRock(BlockState s) {
        return s.is(Blocks.GRANITE) || s.is(Blocks.POLISHED_GRANITE) || s.is(Blocks.TERRACOTTA);
    }

    /** A squashed sphere of granite with a rough surface. */
    private static void boulder(WorldGenLevel level, BlockPos c, double rxz, double ry, RandomSource r) {
        int ri = (int) Math.ceil(rxz), rj = (int) Math.ceil(ry);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -ri; dx <= ri; dx++) for (int dy = -rj; dy <= rj; dy++) for (int dz = -ri; dz <= ri; dz++) {
            double d = (dx * dx + dz * dz) / (rxz * rxz) + (dy * dy) / (ry * ry);
            if (d > 1 + (r.nextDouble() - 0.5) * 0.15) continue;
            p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
            BlockState cur = level.getBlockState(p);
            if (!(cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.DIRT) || cur.is(BlockTags.LEAVES))) continue;
            float f = r.nextFloat();
            BlockState s = f < 0.7F ? Blocks.GRANITE.defaultBlockState() : f < 0.9F ? Blocks.POLISHED_GRANITE.defaultBlockState() : Blocks.TERRACOTTA.defaultBlockState();
            level.setBlock(p, s, 2);
        }
    }
}
