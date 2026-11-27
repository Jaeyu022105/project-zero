package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A sheer stone pillar 40 to 170 blocks tall (Sky Pillars biome, after the sandstone pillars of
 * Zhangjiajie): banded rock with an uneven, weathered outline, mossy streaks, pines and shrubs
 * clinging to ledges on the cliff faces, a forested cap, and vines hanging from the rim. Placed
 * one to three per chunk so they stand as a crowded forest of pillars over a jungle valley.
 */
public class StoneSpireFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 14;

    public StoneSpireFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos base = ctx.origin().below(3);
        int height = 40 + r.nextInt(70) + (r.nextFloat() < 0.3F ? r.nextInt(60) : 0);
        if (base.getY() + height + 10 >= level.getMaxBuildHeight()) height = level.getMaxBuildHeight() - base.getY() - 12;
        if (height < 30) return false;
        // Mostly slender columns, some broad ones.
        double r0 = r.nextFloat() < 0.6F ? 2.5 + r.nextDouble() * 2.5 : 5 + r.nextDouble() * 3, r1 = r0 * (0.7 + r.nextDouble() * 0.25);
        java.util.List<BlockPos> ledges = new java.util.ArrayList<>();
        double p1 = r.nextDouble() * 10, p2 = r.nextDouble() * 10;
        int bandSeed = r.nextInt(7);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int[] topY = new int[(REACH * 2 + 1) * (REACH * 2 + 1)];
        java.util.Arrays.fill(topY, Integer.MIN_VALUE);
        for (int y = 0; y <= height; y++) {
            double t = y / (double) height;
            double rad = r0 + (r1 - r0) * t + (t > 0.92 ? -(t - 0.92) * 20 : 0);
            // Sway the centre a little so it isn't a perfect column.
            double cx = Math.sin(t * 3 + p1) * 1.2, cz = Math.cos(t * 2.6 + p2) * 1.2;
            BlockState band = band(y + bandSeed);
            int ri = (int) Math.ceil(rad) + 2;
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                if (Math.abs(dx) > REACH || Math.abs(dz) > REACH) continue;
                double fx = dx - cx, fz = dz - cz;
                double ang = Math.atan2(fz, fx);
                // Weathered outline: ridges that change slowly with height.
                double edge = rad + Math.sin(ang * 3 + y * 0.07 + p1) * 0.9 + Math.sin(ang * 7 + p2 - y * 0.05) * 0.45;
                if (fx * fx + fz * fz > edge * edge) continue;
                p.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
                BlockState cur = level.getBlockState(p);
                if (!(cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.DIRT) || cur.is(BlockTags.LEAVES) || cur.is(BlockTags.BASE_STONE_OVERWORLD))) continue;
                boolean outer = fx * fx + fz * fz > (edge - 1.2) * (edge - 1.2);
                BlockState s = outer && r.nextFloat() < 0.12F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : band;
                level.setBlock(p, s, 2);
                if (outer && y > 10 && y < height - 8 && r.nextFloat() < (rad < 4 ? 0.006F : 0.004F)) ledges.add(p.immutable());
                topY[(dx + REACH) * (REACH * 2 + 1) + dz + REACH] = Math.max(topY[(dx + REACH) * (REACH * 2 + 1) + dz + REACH], y);
            }
        }
        // Grassy cap and hanging vines from the rim.
        for (int dx = -REACH; dx <= REACH; dx++) for (int dz = -REACH; dz <= REACH; dz++) {
            int ty = topY[(dx + REACH) * (REACH * 2 + 1) + dz + REACH];
            if (ty < height - 6) continue;
            p.set(base.getX() + dx, base.getY() + ty, base.getZ() + dz);
            level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            level.setBlock(p.below(), Blocks.DIRT.defaultBlockState(), 2);
            BlockPos up = p.above();
            if (level.getBlockState(up).isAir() && r.nextFloat() < 0.4F)
                level.setBlock(up, (r.nextFloat() < 0.8F ? Blocks.SHORT_GRASS : Blocks.FERN).defaultBlockState(), 2);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos side = p.relative(d);
                if (!level.getBlockState(side).isAir() || r.nextFloat() > 0.45F) continue;
                int len = 6 + r.nextInt(26);
                BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(d.getOpposite()), true);
                BlockPos.MutableBlockPos v = side.mutable();
                for (int k = 0; k < len && level.getBlockState(v).isAir() && !level.getBlockState(v.relative(d.getOpposite())).isAir(); k++) {
                    level.setBlock(v, vine, 2);
                    v.move(Direction.DOWN);
                }
            }
        }
        // A crowded cap: wind-bent pines with leafy shrubs between them.
        double cx = Math.sin(3 + p1) * 1.2, cz = Math.cos(2.6 + p2) * 1.2;
        int plants = 2 + (int) (r1 * r1 * 0.5) + r.nextInt(3);
        for (int i = 0; i < plants; i++) {
            int dx = (int) Math.round(cx + (r.nextDouble() - 0.5) * r1 * 1.6), dz = (int) Math.round(cz + (r.nextDouble() - 0.5) * r1 * 1.6);
            if (Math.abs(dx) > REACH - 3 || Math.abs(dz) > REACH - 3) continue;
            int ty = topY[(dx + REACH) * (REACH * 2 + 1) + dz + REACH];
            if (ty < height - 6) continue;
            if (i % 3 == 2) shrub(level, base.offset(dx, ty + 1, dz), r);
            else pine(level, base.offset(dx, ty + 1, dz), r);
        }
        // Pines and shrubs growing straight out of cracks in the cliff.
        for (BlockPos l : ledges) {
            int dx = l.getX() - base.getX(), dz = l.getZ() - base.getZ();
            Direction out = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            BlockPos at = l.relative(out);
            if (Math.abs(at.getX() - base.getX()) > REACH - 3 || Math.abs(at.getZ() - base.getZ()) > REACH - 3) continue;
            if (!level.getBlockState(at).isAir()) continue;
            if (r.nextFloat() < 0.55F) cliffPine(level, at, out, r);
            else shrub(level, at, r);
        }
        return true;
    }

    /** A small pine that starts sideways out of the rock and turns up toward the light. */
    private static void cliffPine(WorldGenLevel level, BlockPos at, Direction out, RandomSource r) {
        BlockState side = Blocks.SPRUCE_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, out.getAxis());
        level.setBlock(at, side, 2);
        BlockPos next = at.relative(out);
        if (r.nextBoolean() && level.getBlockState(next).isAir()) { level.setBlock(next, side, 2); at = next; }
        pine(level, at.above(), r);
    }

    /** A low clump of leaves around a stub of trunk. */
    private static void shrub(WorldGenLevel level, BlockPos at, RandomSource r) {
        BlockState leaves = (r.nextFloat() < 0.3F ? Blocks.AZALEA_LEAVES : Blocks.OAK_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        if (level.getBlockState(at).isAir() || level.getBlockState(at).canBeReplaced()) level.setBlock(at, Blocks.OAK_LOG.defaultBlockState(), 2);
        int rad = 1 + r.nextInt(2);
        for (int dx = -rad; dx <= rad; dx++) for (int dy = 0; dy <= rad; dy++) for (int dz = -rad; dz <= rad; dz++) {
            if (dx * dx + dy * dy * 2 + dz * dz > rad * rad + 1 || r.nextFloat() < 0.15F) continue;
            BlockPos lp = at.offset(dx, dy, dz);
            if (level.getBlockState(lp).isAir()) level.setBlock(lp, leaves, 2);
        }
    }

    private static BlockState band(int y) {
        int b = Math.floorMod(y / 3 + (y / 11) * 2, 9);
        return switch (b) {
            case 0, 4 -> Blocks.ANDESITE.defaultBlockState();
            case 6 -> Blocks.TUFF.defaultBlockState();
            case 8 -> Blocks.CALCITE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    private static void pine(WorldGenLevel level, BlockPos at, RandomSource r) {
        int h = 5 + r.nextInt(4);
        Direction bend = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockPos.MutableBlockPos p = at.mutable();
        for (int y = 0; y < h; y++) {
            if (y == h / 2 + 1) p.move(bend);
            if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) level.setBlock(p, log, 2);
            int lr = y < 2 ? 0 : Math.max(0, (h - y) / 2);
            for (int dx = -lr; dx <= lr; dx++) for (int dz = -lr; dz <= lr; dz++) {
                if (dx == 0 && dz == 0 || Math.abs(dx) + Math.abs(dz) > lr + 1) continue;
                BlockPos lp = p.offset(dx, 0, dz);
                if (level.getBlockState(lp).isAir()) level.setBlock(lp, leaves, 2);
            }
            p.move(Direction.UP);
        }
        if (level.getBlockState(p).isAir()) level.setBlock(p, leaves, 2);
    }
}
