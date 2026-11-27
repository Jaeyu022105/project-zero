package com.projectzero.worldgen;

import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Salt Mirror Flats: a row of raked salt cones, 2 to 4 tall, standing in the shallow water like a
 * harvest left to dry, so the mirror reflects them. Sometimes a single big cone instead of a row.
 * Stays within about 12 blocks of the origin.
 */
public class SaltMoundFeature extends Feature<NoneFeatureConfiguration> {
    public SaltMoundFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        BlockState salt = ModNature.SALT_CRUST.get().defaultBlockState();
        if (r.nextFloat() < 0.25F) {
            cone(level, o, 2.6 + r.nextDouble(), 4 + r.nextInt(2), salt);
            return true;
        }
        Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        Direction s = d.getClockWise();
        int rows = 1 + r.nextInt(2), count = 4 + r.nextInt(4);
        for (int row = 0; row < rows; row++) for (int i = 0; i < count; i++) {
            if (r.nextFloat() < 0.15F) continue; // a gap where one was already carted off
            BlockPos c = o.relative(d, i * 3 - count).relative(s, row * 4);
            cone(level, c, 1.2 + r.nextDouble() * 0.5, 2 + r.nextInt(2), salt);
        }
        return true;
    }

    /** A small cone of salt whose base sits on the flat, standing in the water if there is any. */
    private static void cone(WorldGenLevel level, BlockPos c, double r0, int h, BlockState salt) {
        // Find the flat's surface in this column: the first block that is neither air nor water.
        int y = c.getY();
        for (int dy = 2; dy >= -3; dy--) {
            BlockState b = level.getBlockState(c.above(dy));
            if (!b.isAir() && !b.is(Blocks.WATER)) { y = c.getY() + dy + 1; break; }
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy < h; dy++) {
            double rad = r0 * (1 - dy / (double) h) + 0.3;
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                if (dx * dx + dz * dz > rad * rad) continue;
                p.set(c.getX() + dx, y + dy, c.getZ() + dz);
                BlockState cur = level.getBlockState(p);
                if (cur.isAir() || cur.is(Blocks.WATER) || cur.canBeReplaced()) level.setBlock(p, salt, 2);
            }
        }
    }
}
