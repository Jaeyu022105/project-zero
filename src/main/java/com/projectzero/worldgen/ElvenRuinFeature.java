package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Elderwood: what's left of an old elven shrine. A ring of pale columns (calcite and quartz), most
 * broken at different heights, some still joined by a curved arch, a round mossy floor of stone
 * tiles, a fallen column drum or two, and vines climbing everything. Stays within about 9 blocks
 * of the origin.
 */
public class ElvenRuinFeature extends Feature<NoneFeatureConfiguration> {
    public ElvenRuinFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int base = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        double rad = 4 + r.nextDouble() * 2.5;
        // Floor: broken tiles, moss eating them.
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int ri = (int) Math.ceil(rad);
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            if (dx * dx + dz * dz > rad * rad || r.nextFloat() < 0.25F) continue;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX() + dx, o.getZ() + dz);
            if (Math.abs(top - base) > 2) continue;
            p.set(o.getX() + dx, top - 1, o.getZ() + dz);
            if (!level.getBlockState(p).isSolid()) continue;
            float f = r.nextFloat();
            level.setBlock(p, (f < 0.4F ? Blocks.MOSSY_STONE_BRICKS : f < 0.7F ? Blocks.CHISELED_QUARTZ_BLOCK : f < 0.85F ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSS_BLOCK).defaultBlockState(), 2);
        }
        // Columns around the ring.
        int n = 6 + r.nextInt(3);
        int[] heights = new int[n];
        BlockPos[] feet = new BlockPos[n];
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            int x = o.getX() + (int) Math.round(Math.cos(a) * rad), z = o.getZ() + (int) Math.round(Math.sin(a) * rad);
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            feet[i] = new BlockPos(x, top, z);
            heights[i] = r.nextFloat() < 0.35F ? 7 + r.nextInt(2) : 1 + r.nextInt(5);   // whole or broken
            if (Math.abs(top - base) > 3) { heights[i] = 0; continue; }
            for (int y = 0; y < heights[i]; y++) {
                BlockState s = y == 0 ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : Blocks.QUARTZ_PILLAR.defaultBlockState();
                if (r.nextFloat() < 0.25F) s = Blocks.CALCITE.defaultBlockState();
                level.setBlock(feet[i].above(y), s, 2);
                vine(level, feet[i].above(y), r);
            }
        }
        // Arches between neighbouring whole columns.
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            if (heights[i] < 7 || heights[j] < 7) continue;
            BlockPos a = feet[i].above(heights[i] - 1), b = feet[j].above(heights[j] - 1);
            int steps = 12;
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                double lift = Math.sin(Math.PI * t) * 2.2;
                BlockPos q = new BlockPos((int) Math.round(a.getX() + (b.getX() - a.getX()) * t),
                        (int) Math.round(a.getY() + (b.getY() - a.getY()) * t + lift),
                        (int) Math.round(a.getZ() + (b.getZ() - a.getZ()) * t));
                if (level.getBlockState(q).isAir()) level.setBlock(q, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
            }
        }
        // A fallen drum lying outside the ring.
        if (r.nextBoolean()) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            BlockPos start = o.relative(d, ri + 1);
            for (int k = 0; k < 3; k++) {
                BlockPos q = start.relative(d, k);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, q.getX(), q.getZ());
                if (Math.abs(top - base) > 3) break;
                level.setBlock(new BlockPos(q.getX(), top, q.getZ()),
                        Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, d.getAxis()), 2);
            }
        }
        return true;
    }

    /** Sometimes hang a vine on a free side of this block. */
    private static void vine(WorldGenLevel level, BlockPos pos, RandomSource r) {
        if (r.nextFloat() > 0.35F) return;
        Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        BlockPos v = pos.relative(d);
        if (!level.getBlockState(v).isAir()) return;
        // The vine attaches to the column, which is on its opposite side.
        level.setBlock(v, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(d.getOpposite()), true), 2);
    }
}
