package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A giant elven-style "elder tree": a tall pale trunk with a gentle sway, buttress roots that
 * flare out and dive into the ground, upward-reaching branches ending in broad leaf clouds, a crown
 * on top, and glowing berry vines hanging from the canopy.
 *
 * Everything stays within {@link #REACH} blocks of the origin so the whole tree fits in the
 * area a feature may write to during world generation.
 */
public class ElderTreeFeature extends Feature<NoneFeatureConfiguration> {
    /** Furthest any block is placed from the trunk centre, horizontally. */
    private static final int REACH = 15;

    private static final BlockState LOG = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
    private static final BlockState BARK = Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState();
    private static final BlockState ROOT = Blocks.BIRCH_WOOD.defaultBlockState();
    private static final BlockState LEAVES = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

    public ElderTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos base = ctx.origin();
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) return false;

        int height = 26 + r.nextInt(14);
        if (base.getY() + height + 8 >= level.getMaxBuildHeight()) return false;
        double r0 = 2.4 + r.nextDouble() * 0.8;
        double phase = r.nextDouble() * Math.PI * 2, swayAmp = 0.8 + r.nextDouble() * 0.8;

        // Trunk: discs that taper and drift a little, so it isn't a perfect pillar.
        double[] cx = new double[height + 1], cz = new double[height + 1];
        for (int y = 0; y <= height; y++) {
            double t = y / (double) height;
            cx[y] = Math.sin(t * 2.4 + phase) * swayAmp * t;
            cz[y] = Math.cos(t * 2.0 + phase) * swayAmp * t;
            double rad = Mth.lerp(t, r0, 0.9);
            disc(level, base, cx[y], y, cz[y], rad, LOG);
        }

        // Buttress roots: flare out from the base and dive under the ground.
        int roots = 5 + r.nextInt(3);
        for (int i = 0; i < roots; i++) {
            double a = i * (Math.PI * 2 / roots) + r.nextDouble() * 0.5;
            double len = 5 + r.nextInt(3);
            for (double s = 0; s <= 1.0; s += 0.5 / len) {
                double out = r0 * 0.5 + s * len;
                double y = 3.5 * Math.pow(1 - s, 1.6) - s * 2.5;
                double rad = 1.5 * (1 - s) + 0.5;
                sphere(level, base, Math.cos(a) * out, y, Math.sin(a) * out, rad, ROOT, true);
            }
        }

        // Branches: reach up and out from the upper half; each ends in a leaf cloud.
        int branches = 5 + r.nextInt(3);
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < branches; i++) {
            int y0 = (int) (height * (0.45 + 0.45 * i / (double) branches));
            double a = i * golden * 2 + r.nextDouble() * 0.6;
            double len = 6 + r.nextInt(3);
            double rise = 0.45 + r.nextDouble() * 0.35;
            double sx = cx[y0], sz = cz[y0];
            double ex = sx + Math.cos(a) * len, ez = sz + Math.sin(a) * len, ey = y0 + len * rise;
            int steps = (int) (len * 2.5);
            for (int k = 0; k <= steps; k++) {
                double t = k / (double) steps;
                // A slight upward curve toward the tip.
                double px = Mth.lerp(t, sx, ex), pz = Mth.lerp(t, sz, ez);
                double py = Mth.lerp(t, y0, ey) + Math.sin(t * Math.PI) * 1.2;
                sphere(level, base, px, py, pz, Mth.lerp(t, 1.1, 0.5), BARK, false);
            }
            leafCloud(level, base, ex, ey + 1, ez, 3.5 + r.nextDouble() * 1.5, 2.2 + r.nextDouble(), r);
        }
        // Crown.
        leafCloud(level, base, cx[height], height + 1.5, cz[height], 5.0 + r.nextDouble(), 3.2, r);
        return true;
    }

    /** A horizontal disc of logs (one layer of the trunk). */
    private static void disc(WorldGenLevel level, BlockPos base, double ox, int y, double oz, double rad, BlockState state) {
        int ri = (int) Math.ceil(rad);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            double fx = dx - ox, fz = dz - oz;
            if (fx * fx + fz * fz > rad * rad) continue;
            if (!inReach(dx, dz)) continue;
            p.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
            if (canReplaceWood(level.getBlockState(p))) level.setBlock(p, state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), 2);
        }
    }

    private static void sphere(WorldGenLevel level, BlockPos base, double ox, double oy, double oz, double rad,
                               BlockState state, boolean intoGround) {
        int ri = (int) Math.ceil(rad);
        int bx = (int) Math.floor(ox), by = (int) Math.floor(oy), bz = (int) Math.floor(oz);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -ri; dx <= ri; dx++) for (int dy = -ri; dy <= ri; dy++) for (int dz = -ri; dz <= ri; dz++) {
            double fx = bx + dx + 0.5 - ox, fy = by + dy + 0.5 - oy, fz = bz + dz + 0.5 - oz;
            if (fx * fx + fy * fy + fz * fz > rad * rad) continue;
            if (!inReach(bx + dx, bz + dz)) continue;
            p.set(base.getX() + bx + dx, base.getY() + by + dy, base.getZ() + bz + dz);
            BlockState cur = level.getBlockState(p);
            boolean ok = canReplaceWood(cur) || (intoGround && (cur.is(BlockTags.DIRT) || cur.is(BlockTags.BASE_STONE_OVERWORLD)));
            if (ok) level.setBlock(p, state, 2);
        }
    }

    /** A flattened cloud of leaves with a ragged edge; berry vines hang from its underside. */
    private static void leafCloud(WorldGenLevel level, BlockPos base, double ox, double oy, double oz,
                                  double rx, double ry, RandomSource r) {
        int ix = (int) Math.ceil(rx), iy = (int) Math.ceil(ry);
        int bx = (int) Math.floor(ox), by = (int) Math.floor(oy), bz = (int) Math.floor(oz);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -ix; dx <= ix; dx++) for (int dy = -iy; dy <= iy; dy++) for (int dz = -ix; dz <= ix; dz++) {
            double fx = (dx + 0.5 + bx - ox) / rx, fy = (dy + 0.5 + by - oy) / ry, fz = (dz + 0.5 + bz - oz) / rx;
            double d = fx * fx + fy * fy + fz * fz;
            if (d > 1.0 || (d > 0.7 && r.nextFloat() < 0.35F)) continue;   // ragged edge
            if (!inReach(bx + dx, bz + dz)) continue;
            p.set(base.getX() + bx + dx, base.getY() + by + dy, base.getZ() + bz + dz);
            if (level.getBlockState(p).isAir()) level.setBlock(p, LEAVES, 2);
        }
        // Glow berry vines from the bottom of the cloud.
        for (int dx = -ix; dx <= ix; dx++) for (int dz = -ix; dz <= ix; dz++) {
            if (r.nextFloat() > 0.12F || !inReach(bx + dx, bz + dz)) continue;
            int x = base.getX() + bx + dx, z = base.getZ() + bz + dz;
            int y = base.getY() + by - iy;
            p.set(x, y, z);
            while (p.getY() < base.getY() + by + iy && !level.getBlockState(p).is(BlockTags.LEAVES)) p.move(Direction.UP);
            if (!level.getBlockState(p).is(BlockTags.LEAVES)) continue;
            int len = 2 + r.nextInt(6);
            for (int k = 1; k <= len; k++) {
                p.move(Direction.DOWN);
                if (!level.getBlockState(p).isAir()) break;
                boolean last = k == len || !level.getBlockState(p.below()).isAir();
                BlockState vine = (last ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                        .setValue(CaveVines.BERRIES, r.nextFloat() < 0.35F);
                level.setBlock(p, vine, 2);
                if (last) break;
            }
        }
    }

    private static boolean inReach(int dx, int dz) {
        return Math.abs(dx) <= REACH && Math.abs(dz) <= REACH;
    }

    private static boolean canReplaceWood(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.REPLACEABLE_BY_TREES);
    }
}
