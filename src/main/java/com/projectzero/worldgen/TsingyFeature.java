package com.projectzero.worldgen;

import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A stand of Tsingy: 14 to 28 razor-thin limestone needles packed together, up to about 35 blocks tall,
 * with narrow slots between them. Tips end in a stone point. Vines and the odd shrub hide in the gaps.
 */
public class TsingyFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 13;

    public TsingyFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int needles = 14 + r.nextInt(14);
        int tall = 14 + r.nextInt(18);   // this stand's typical height
        for (int i = 0; i < needles; i++) {
            int dx = r.nextInt(REACH * 2 - 3) - REACH + 2, dz = r.nextInt(REACH * 2 - 3) - REACH + 2;
            double rad = 1.0 + r.nextDouble() * 1.5;
            double edgeFall = 1 - (dx * dx + dz * dz) / (double) (REACH * REACH);   // shorter toward the edge of the stand
            int h = (int) (Math.max(0.25, edgeFall) * (tall * (0.6 + r.nextDouble() * 0.7))) + 3;
            needle(level, o.offset(dx, 0, dz), rad, h, r);
        }
        rubble(level, o, r);
        if (r.nextFloat() < 0.55F) {
            int pools = 1 + r.nextInt(2);
            for (int i = 0; i < pools; i++) {
                int dx = r.nextInt(15) - 7, dz = r.nextInt(15) - 7;
                slotPool(level, o.offset(dx, 0, dz), r);
            }
        }
        return true;
    }

    private static void needle(WorldGenLevel level, BlockPos at, double r0, int h, RandomSource r) {
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, at.getX(), at.getZ());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int ri = (int) Math.ceil(r0);
        boolean weatheredTip = r.nextFloat() < 0.38F;
        for (int y = -2; y < h; y++) {
            double t = Math.max(0, y) / (double) h;
            double rad = r0 * (1 - t * t) + 0.25;
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                if (dx * dx + dz * dz > rad * rad) continue;
                p.set(at.getX() + dx, ground + y, at.getZ() + dz);
                BlockState cur = level.getBlockState(p);
                if (!(cur.isAir() || cur.canBeReplaced() || cur.is(net.minecraft.tags.BlockTags.DIRT) || cur.is(net.minecraft.tags.BlockTags.SAND)
                        || cur.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) || cur.is(net.minecraft.tags.BlockTags.TERRACOTTA))) continue;
                float f = r.nextFloat();
                BlockState s = weatheredTip && y >= h - 4
                        ? (r.nextBoolean() ? Blocks.TUFF.defaultBlockState() : Blocks.ANDESITE.defaultBlockState())
                        : f < 0.72F ? ModNature.LIMESTONE.get().defaultBlockState()
                        : f < 0.88F ? Blocks.STONE.defaultBlockState() : Blocks.TUFF.defaultBlockState();
                level.setBlock(p, s, 2);
            }
        }
        // Stone point on top.
        BlockPos tip = new BlockPos(at.getX(), ground + h, at.getZ());
        if (level.getBlockState(tip).isAir() && r.nextFloat() < 0.7F) {
            level.setBlock(tip, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
                    .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP), 2);
        }
        // Vines down the slot walls.
        if (r.nextFloat() < 0.35F) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            BlockPos.MutableBlockPos v = new BlockPos.MutableBlockPos(at.getX(), ground + h / 2, at.getZ());
            v.move(d, ri + 1);
            for (int k = 0; k < h / 2 && level.getBlockState(v).isAir() && !level.getBlockState(v.relative(d.getOpposite())).isAir(); k++) {
                level.setBlock(v, Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(d.getOpposite()), true), 2);
                v.move(Direction.DOWN);
            }
        }
    }

    /** Scattered sharp limestone chips break up the bare floor between dense stands. */
    private static void rubble(WorldGenLevel level, BlockPos origin, RandomSource r) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int heaps = 3 + r.nextInt(4);
        for (int i = 0; i < heaps; i++) {
            int dx = r.nextInt(19) - 9, dz = r.nextInt(19) - 9;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + dx, origin.getZ() + dz);
            int pieces = 2 + r.nextInt(5);
            for (int n = 0; n < pieces; n++) {
                int ox = r.nextInt(5) - 2, oz = r.nextInt(5) - 2;
                int h = r.nextFloat() < 0.35F ? 2 : 1;
                for (int dy = 0; dy < h; dy++) {
                    p.set(origin.getX() + dx + ox, y + dy, origin.getZ() + dz + oz);
                    BlockState current = level.getBlockState(p);
                    if (current.isAir() || current.canBeReplaced())
                        level.setBlock(p, r.nextFloat() < 0.25F ? Blocks.ANDESITE.defaultBlockState()
                                : ModNature.LIMESTONE.get().defaultBlockState(), 2);
                }
            }
        }
    }

    /** A still, shallow basin in a natural gap; uneven or spike-covered ground is left alone. */
    private static void slotPool(WorldGenLevel level, BlockPos at, RandomSource r) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, at.getX(), at.getZ());
        if (Math.abs(surface - at.getY()) > 4) return;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, at.getX() + dx, at.getZ() + dz) != surface) return;
            BlockState ground = level.getBlockState(new BlockPos(at.getX() + dx, surface - 1, at.getZ() + dz));
            if (!ground.is(net.minecraft.tags.BlockTags.DIRT) && !ground.is(Blocks.GRASS_BLOCK)) return;
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 2.2) continue;
            BlockPos floor = new BlockPos(at.getX() + dx, surface - 1, at.getZ() + dz);
            if (d < 1.45) {
                level.setBlock(floor, Blocks.WATER.defaultBlockState(), 2);
                level.setBlock(floor.below(), r.nextBoolean() ? ModNature.LIMESTONE.get().defaultBlockState()
                        : Blocks.GRAVEL.defaultBlockState(), 2);
            } else {
                level.setBlock(floor, r.nextFloat() < 0.3F ? Blocks.MOSS_BLOCK.defaultBlockState()
                        : ModNature.LIMESTONE.get().defaultBlockState(), 2);
                BlockPos fern = floor.above();
                if (r.nextFloat() < 0.24F && level.getBlockState(fern).isAir())
                    level.setBlock(fern, Blocks.FERN.defaultBlockState(), 2);
            }
        }
        // A couple of vine strands cling to the sharp limestone lips beside the water.
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (r.nextFloat() > 0.5F) continue;
            BlockPos wall = new BlockPos(at.getX() + d.getStepX() * 2, surface, at.getZ() + d.getStepZ() * 2);
            level.setBlock(wall, ModNature.LIMESTONE.get().defaultBlockState(), 2);
            level.setBlock(wall.above(), ModNature.LIMESTONE.get().defaultBlockState(), 2);
            BlockPos.MutableBlockPos vine = new BlockPos.MutableBlockPos(wall.getX() + d.getStepX(), surface + 1,
                    wall.getZ() + d.getStepZ());
            BlockState vineState = Blocks.VINE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(d.getOpposite()), true);
            for (int k = 0; k < 3 && level.getBlockState(vine).isAir()
                    && !level.getBlockState(vine.relative(d.getOpposite())).isAir(); k++) {
                level.setBlock(vine, vineState, 2);
                vine.move(Direction.DOWN);
            }
        }
    }
}
