package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Geyser Basin: travertine terraces. A cascade of round, shallow pools stepping down the slope,
 * each held by a white calcite rim with streaks of pale and orange mineral, the water spilling
 * (visually) from one pool to the next. Every pool's water is fully walled by its rim or solid
 * ground, so nothing flows. Stays within about 14 blocks of the origin.
 */
public class TravertineTerraceFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MAX_OFFSET = 10;

    public TravertineTerraceFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        if (!level.getBlockState(o.below()).isSolid()) return false;
        // Step downhill: the lowest of the four directions 8 blocks out.
        Direction down = Direction.NORTH;
        int lowest = Integer.MAX_VALUE;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, o.getX() + d.getStepX() * 8, o.getZ() + d.getStepZ() * 8);
            if (h < lowest) { lowest = h; down = d; }
        }
        Direction side = down.getClockWise();
        int y = o.getY() - 1;   // water level of the first pool sits at the ground surface
        double off = 0, sideOff = 0;
        int tiers = 3 + r.nextInt(4);
        boolean any = false;
        for (int t = 0; t < tiers && off <= MAX_OFFSET; t++) {
            double rad = 2.4 + r.nextDouble() * 1.8;
            int cx = o.getX() + (int) Math.round(down.getStepX() * off + side.getStepX() * sideOff);
            int cz = o.getZ() + (int) Math.round(down.getStepZ() * off + side.getStepZ() * sideOff);
            int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz) - 1;
            if (t > 0) y = Math.min(y - 1, ground + 1);
            if (ground < y - 3) y = ground + 1;   // don't build a tall tower on a sudden drop
            any |= pool(level, r, cx, y, cz, rad);
            off += rad * 1.25;
            sideOff += (r.nextDouble() - 0.5) * 2;
        }
        return any;
    }

    /** One pool: calcite floor and rim, water inside at level y, solid fill down to the ground. */
    private static boolean pool(WorldGenLevel level, RandomSource r, int cx, int y, int cz, double rad) {
        int ri = (int) Math.ceil(rad);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > rad) continue;
            int x = cx + dx, z = cz + dz;
            boolean rim = d > rad - 1;
            // Solid base under the pool, down to the ground.
            for (int yy = y - 1; yy >= y - 6; yy--) {
                p.set(x, yy, z);
                BlockState cur = level.getBlockState(p);
                if (!(cur.isAir() || cur.canBeReplaced() || cur.is(Blocks.WATER))) {
                    if (yy == y - 1) level.setBlock(p, mineral(r, false), 2);
                    break;
                }
                level.setBlock(p, mineral(r, yy < y - 1), 2);
            }
            p.set(x, y, z);
            BlockState cur = level.getBlockState(p);
            if (cur.is(Blocks.CALCITE) || cur.is(Blocks.WHITE_TERRACOTTA) || cur.is(Blocks.ORANGE_TERRACOTTA)) continue; // a higher pool's base
            level.setBlock(p, rim ? mineral(r, false) : Blocks.WATER.defaultBlockState(), 2);
            // Clear the hillside above so the pool is open to the sky.
            for (int yy = y + 1; yy <= y + 4; yy++) {
                p.set(x, yy, z);
                if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
            any = true;
        }
        return any;
    }

    /** Mostly white calcite; streaks of orange and pale mineral on the faces. */
    private static BlockState mineral(RandomSource r, boolean face) {
        float f = r.nextFloat();
        if (face && f < 0.2F) return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        if (f < 0.12F) return Blocks.WHITE_TERRACOTTA.defaultBlockState();
        return Blocks.CALCITE.defaultBlockState();
    }
}
