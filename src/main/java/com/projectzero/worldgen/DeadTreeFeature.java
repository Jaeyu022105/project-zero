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

/**
 * Bloodmoon Steppe dead trees. Three shapes:
 * <ul>
 *   <li>a plain dead tree with crooked reaching branches,</li>
 *   <li>a snapped stump (a short jagged trunk, the top lying beside it),</li>
 *   <li>a great dead tree: a thick flared trunk 12 to 20 tall whose branches fork again and claw at the sky.</li>
 * </ul>
 * Everything stays within about 10 blocks of the origin, inside the 3x3 chunks a feature may touch.
 */
public class DeadTreeFeature extends Feature<NoneFeatureConfiguration> {
    public DeadTreeFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        if (!level.getBlockState(o.below()).is(BlockTags.DIRT)) return false;
        BlockState log = (r.nextBoolean() ? Blocks.DARK_OAK_LOG : Blocks.STRIPPED_DARK_OAK_LOG).defaultBlockState();
        float kind = r.nextFloat();
        if (kind < 0.2F) great(level, o, log, r);
        else if (kind < 0.42F) snapped(level, o, log, r);
        else plain(level, o, log, r);
        return true;
    }

    private static void plain(WorldGenLevel level, BlockPos o, BlockState log, RandomSource r) {
        int h = 6 + r.nextInt(7);
        BlockPos.MutableBlockPos p = o.mutable();
        BlockPos[] trunk = new BlockPos[h];
        for (int y = 0; y < h; y++) {
            if (y > 2 && r.nextFloat() < 0.2F) {
                // A crooked step: fill the corner so the trunk stays connected face to face.
                Direction step = Direction.Plane.HORIZONTAL.getRandomDirection(r);
                p.move(step);
                set(level, p.below(), log);
            }
            set(level, p, log);
            trunk[y] = p.immutable();
            p.move(Direction.UP);
        }
        int branches = 3 + r.nextInt(3);
        for (int b = 0; b < branches; b++) {
            // Branches grow out of the trunk where it actually is, not out of the original column.
            BlockPos start = trunk[h / 2 + r.nextInt(Math.max(1, h - h / 2))];
            branch(level, start, r.nextDouble() * Math.PI * 2, 2 + r.nextInt(4), log, r, 0);
        }
        roots(level, o, log, 2 + r.nextInt(2), 2, r);
    }

    /** A jagged stump; the broken top lies on the ground next to it, pointing away. */
    private static void snapped(WorldGenLevel level, BlockPos o, BlockState log, RandomSource r) {
        int h = 2 + r.nextInt(3);
        for (int y = 0; y < h; y++) set(level, o.above(y), log);
        if (r.nextBoolean()) set(level, o.above(h), Blocks.DARK_OAK_FENCE.defaultBlockState()); // splinter
        Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        int len = 4 + r.nextInt(5);
        BlockState lying = log.setValue(RotatedPillarBlock.AXIS, d.getAxis());
        for (int k = 2; k < 2 + len; k++) {
            BlockPos p = o.relative(d, k);
            BlockPos ground = settle(level, p);
            if (ground != null) set(level, ground, lying);
        }
        roots(level, o, log, 2, 2, r);
    }

    private static void great(WorldGenLevel level, BlockPos o, BlockState log, RandomSource r) {
        int h = 12 + r.nextInt(9);
        double r0 = 1.1 + r.nextDouble() * 0.5;
        double lx = (r.nextDouble() - 0.5) * 0.25, lz = (r.nextDouble() - 0.5) * 0.25; // slight lean
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = -2; y < h; y++) {
            double t = Math.max(0, y) / (double) h;
            double rad = r0 * (1 - 0.55 * t) + (y <= 1 ? 1.0 - Math.max(0, y) * 0.4 : 0);
            int cx = o.getX() + (int) Math.round(lx * y), cz = o.getZ() + (int) Math.round(lz * y);
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                if (dx * dx + dz * dz > rad * rad) continue;
                p.set(cx + dx, o.getY() + y, cz + dz);
                if (y < 0) { if (level.getBlockState(p).is(BlockTags.DIRT)) level.setBlock(p, log, 2); }
                else set(level, p, log);
            }
        }
        int branches = 4 + r.nextInt(3);
        for (int b = 0; b < branches; b++) {
            double ang = b * (Math.PI * 2 / branches) + r.nextDouble() * 0.7;
            // Start on the leaning trunk's centre at that height, so the branch joins the trunk.
            int sy = h - 1 - r.nextInt(Math.max(1, h / 3));
            BlockPos start = o.offset((int) Math.round(lx * sy), sy, (int) Math.round(lz * sy));
            branch(level, start, ang, 3 + r.nextInt(4), log, r, 2);
        }
        roots(level, o, log, 4 + r.nextInt(3), 4, r);
    }

    /** A crooked branch that climbs as it reaches out; at depth > 0 it may fork into two thinner ones. */
    private static void branch(WorldGenLevel level, BlockPos start, double ang, int len, BlockState log, RandomSource r, int depth) {
        double x = 0, y = 0, z = 0;
        BlockPos end = start;
        for (int k = 0; k < len; k++) {
            ang += (r.nextDouble() - 0.5) * 0.5;
            x += Math.cos(ang); z += Math.sin(ang);
            y += 0.35 + r.nextDouble() * 0.5;
            end = start.offset((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            Direction.Axis axis = Math.abs(Math.cos(ang)) > Math.abs(Math.sin(ang)) ? Direction.Axis.X : Direction.Axis.Z;
            set(level, end, log.setValue(RotatedPillarBlock.AXIS, axis));
        }
        if (depth > 0) {
            branch(level, end, ang + 0.5 + r.nextDouble() * 0.4, Math.max(2, len - 2), log, r, depth - 1);
            if (r.nextBoolean()) branch(level, end, ang - 0.5 - r.nextDouble() * 0.4, Math.max(2, len - 2), log, r, depth - 1);
        } else if (r.nextFloat() < 0.4F) {
            set(level, end.above(), Blocks.DARK_OAK_FENCE.defaultBlockState()); // a bare twig at the tip
        }
    }

    /** Roots that crawl out over the ground and dive in. */
    private static void roots(WorldGenLevel level, BlockPos o, BlockState log, int count, int maxLen, RandomSource r) {
        for (int i = 0; i < count; i++) {
            double ang = i * (Math.PI * 2 / count) + r.nextDouble() * 0.8;
            int len = 1 + r.nextInt(maxLen);
            Direction.Axis axis = Math.abs(Math.cos(ang)) > Math.abs(Math.sin(ang)) ? Direction.Axis.X : Direction.Axis.Z;
            for (int k = 1; k <= len + 1; k++) {
                BlockPos p = o.offset((int) Math.round(Math.cos(ang) * (k + 0.8)), 0, (int) Math.round(Math.sin(ang) * (k + 0.8)));
                BlockPos g = settle(level, p);
                if (g == null) break;
                // Near the trunk the root rides on the ground, farther out it sinks into it.
                set(level, k <= len / 2 + 1 ? g : g.below(), log.setValue(RotatedPillarBlock.AXIS, axis));
            }
        }
    }

    /** The first open spot above solid ground at this column, searching a few blocks up and down; null if none. */
    private static BlockPos settle(WorldGenLevel level, BlockPos p) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos q = p.above(dy);
            BlockState here = level.getBlockState(q);
            if ((here.isAir() || here.canBeReplaced()) && level.getBlockState(q.below()).isSolid()) return q;
        }
        return null;
    }

    private static void set(WorldGenLevel level, BlockPos p, BlockState s) {
        BlockState cur = level.getBlockState(p);
        if (cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.DIRT)) level.setBlock(p, s, 2);
    }
}
