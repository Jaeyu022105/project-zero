package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Salt Mirror Flats: a sheet of still water one block deep laid over the salt wherever the ground
 * is level, so the flat reflects the sky. Only columns at the origin's height whose neighbours are
 * no lower get water, so nothing spills.
 */
public class SaltPoolFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 13;

    public SaltPoolFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int h = o.getY();
        double rad = 7 + r.nextDouble() * (REACH - 7);
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -REACH; dx <= REACH; dx++) for (int dz = -REACH; dz <= REACH; dz++) {
            double edge = rad + Math.sin(Math.atan2(dz, dx) * 3 + h) * 1.5;
            if (dx * dx + dz * dz > edge * edge) continue;
            int x = o.getX() + dx, z = o.getZ() + dz;
            if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) != h) continue;
            boolean held = true;
            for (Direction d : Direction.Plane.HORIZONTAL)
                if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x + d.getStepX(), z + d.getStepZ()) < h) { held = false; break; }
            if (!held) continue;
            p.set(x, h - 1, z);
            if (level.getBlockState(p).getFluidState().isEmpty() && !level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
                any = true;
            }
        }
        return any;
    }
}
