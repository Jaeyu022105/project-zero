package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Capwood: a fairy ring. A near-perfect circle of small mushrooms on a band of mycelium, the grass
 * inside left untouched and greener (moss), and sometimes a glowing shroomlight half-buried in the
 * middle. Stays within about 7 blocks of the origin.
 */
public class FairyRingFeature extends Feature<NoneFeatureConfiguration> {
    public FairyRingFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        double rad = 3 + r.nextDouble() * 3;
        boolean red = r.nextBoolean();
        int ri = (int) Math.ceil(rad) + 1;
        boolean any = false;
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            int x = o.getX() + dx, z = o.getZ() + dz;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos ground = new BlockPos(x, top - 1, z);
            BlockState g = level.getBlockState(ground);
            if (!g.is(BlockTags.DIRT)) continue;
            if (Math.abs(d - rad) < 0.6) {
                level.setBlock(ground, Blocks.MYCELIUM.defaultBlockState(), 2);
                if (r.nextFloat() < 0.8F)
                    level.setBlock(ground.above(), (red ^ r.nextFloat() < 0.2F ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState(), 2);
                any = true;
            } else if (d < rad - 0.6 && r.nextFloat() < 0.6F) {
                level.setBlock(ground, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
            }
        }
        if (any && r.nextFloat() < 0.4F) {
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
            level.setBlock(new BlockPos(o.getX(), top - 1, o.getZ()), Blocks.SHROOMLIGHT.defaultBlockState(), 2);
        }
        return any;
    }
}
