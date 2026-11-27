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
        return true;
    }

    private static void needle(WorldGenLevel level, BlockPos at, double r0, int h, RandomSource r) {
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, at.getX(), at.getZ());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int ri = (int) Math.ceil(r0);
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
                BlockState s = f < 0.72F ? ModNature.LIMESTONE.get().defaultBlockState()
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
}
