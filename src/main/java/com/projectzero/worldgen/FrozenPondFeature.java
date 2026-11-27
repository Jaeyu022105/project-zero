package com.projectzero.worldgen;

import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Crystal Tundra: a frozen pond. A sheet of clear ice over dark water, with glowing mana crystal
 * growing on the floor so it shines up through the ice at night. Snow drifts over parts of it and
 * piles up along the shore. Only level ground gets the pond, so the water never spills. Stays
 * within about 10 blocks of the origin.
 */
public class FrozenPondFeature extends Feature<NoneFeatureConfiguration> {
    public FrozenPondFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, o.getX(), o.getZ());   // first air above the ground
        double rad = 4 + r.nextDouble() * 5;
        int depth = 2 + r.nextInt(2);
        int ri = (int) Math.ceil(rad) + 2;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            int x = o.getX() + dx, z = o.getZ() + dz;
            double d = Math.sqrt(dx * dx + dz * dz);
            double edge = rad + Math.sin(Math.atan2(dz, dx) * 3 + h) * 1.2;
            if (d > edge + 2) continue;
            if (d > edge) {
                // Shore: snow piles a little higher here.
                drift(level, r, x, z, 2 + r.nextInt(3));
                continue;
            }
            if (!level(level, x, z, h)) continue;
            int dd = d < edge * 0.5 ? depth : depth - 1;
            for (int y = h - 1; y >= h - 1 - dd; y--) {
                p.set(x, y, z);
                if (y == h - 1) level.setBlock(p, Blocks.ICE.defaultBlockState(), 2);
                else level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
            }
            p.set(x, h - 2 - dd, z);
            boolean crystal = r.nextFloat() < 0.12F;
            level.setBlock(p, (crystal ? ModNature.MANA_CRYSTAL_BLOCK.get() : r.nextBoolean() ? Blocks.GRAVEL : Blocks.CLAY).defaultBlockState(), 2);
            p.set(x, h, z);
            BlockState above = level.getBlockState(p);
            if (!above.isAir() && above.canBeReplaced()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            if (r.nextFloat() < 0.18F && level.getBlockState(p).isAir()) level.setBlock(p, Blocks.SNOW.defaultBlockState(), 2);
            any = true;
        }
        return any;
    }

    /** The column's ground and its four neighbours' are at this height, so water under the ice can't leak. */
    private static boolean level(WorldGenLevel level, int x, int z, int h) {
        if (level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) != h) return false;
        for (Direction d : Direction.Plane.HORIZONTAL)
            if (level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + d.getStepX(), z + d.getStepZ()) < h) return false;
        return true;
    }

    /** Thicken the snow on top of the ground at this column to the given number of layers. */
    private static void drift(WorldGenLevel level, RandomSource r, int x, int z, int layers) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos p = new BlockPos(x, top, z);
        BlockState cur = level.getBlockState(p);
        if (cur.is(Blocks.SNOW)) {
            int now = cur.getValue(SnowLayerBlock.LAYERS);
            level.setBlock(p, cur.setValue(SnowLayerBlock.LAYERS, Math.min(8, Math.max(now, layers))), 2);
        } else if (cur.isAir() && level.getBlockState(p.below()).isSolid()) {
            level.setBlock(p, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers), 2);
        }
    }
}
