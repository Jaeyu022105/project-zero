package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.ArrayList;
import java.util.List;

/**
 * Shattered Sky Plateau. Runs once per chunk and handles the whole origin chunk:
 * <ol>
 *   <li><b>Table:</b> the ground is levelled into broad flat tables. Each column snaps to the nearest
 *   {@link #STEP}-block terrace (cut down or filled up by at most half a step), so the peaks become a
 *   high, flat table broken by short cliffs, without walls at the biome edge.</li>
 *   <li><b>Chasms:</b> narrow, sharp cracks follow world-wide noise so they run on from chunk to chunk.
 *   Their walls step down in ledges: a shallow lip, a middle shelf, then the deep core.</li>
 *   <li><b>Slabs:</b> broken-off pieces of the table hover over the cracks; two in one chunk may be
 *   joined by a thin stone arch.</li>
 * </ol>
 * Only the origin chunk is written, and the biome is only read inside it.
 */
public class ShatteredPlateauFeature extends Feature<NoneFeatureConfiguration> {
    private static final int STEP = 14;
    private static SimplexNoise noise, depthNoise;
    private static long noiseSeed = Long.MIN_VALUE;

    public ShatteredPlateauFeature() { super(NoneFeatureConfiguration.CODEC); }

    private static synchronized void init(long seed) {
        if (noise != null && seed == noiseSeed) return;
        noise = new SimplexNoise(new LegacyRandomSource(seed ^ 0x5A77E2EDL));
        depthNoise = new SimplexNoise(new LegacyRandomSource(seed ^ 0x0DEE9L));
        noiseSeed = seed;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        init(level.getSeed());
        int cx = ctx.origin().getX() & ~15, cz = ctx.origin().getZ() & ~15;
        int minY = level.getMinBuildHeight() + 12;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false, chasm = false;
        int sumTop = 0, n = 0;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = cx + dx, z = cz + dz;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            p.set(x, top, z);
            if (!level.getBiome(p).is(ModBiomes.SHATTERED_PLATEAU)) continue;
            any = true;
            top = level(level, p, x, z, top);

            double v = Math.abs(noise.getValue(x / 120.0, z / 120.0));
            double w = 0.035 + 0.015 * depthNoise.getValue(x / 60.0, z / 60.0);
            if (v > w) continue;
            // Stepped walls: lip, shelf, then the deep core in the middle of the crack.
            double f = 1 - v / w;
            int depth = f < 0.3 ? 5 + (int) (f * 10)
                    : f < 0.6 ? 18 + (int) (4 * depthNoise.getValue(x / 20.0, z / 20.0))
                    : (int) (55 + 40 * (f - 0.6) / 0.4 + 12 * depthNoise.getValue(x / 40.0, z / 40.0));
            int bottom = Math.max(minY, top - depth);
            for (int y = top; y >= bottom; y--) {
                p.set(x, y, z);
                if (level.getBlockState(p).is(Blocks.BEDROCK) || !level.getFluidState(p).isEmpty()) break;
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
            // Ledges get a skin of grass and gravel so the steps read from above.
            p.set(x, bottom - 1, z);
            if (f < 0.6 && level.getBlockState(p).is(BlockTags.BASE_STONE_OVERWORLD))
                level.setBlock(p, (r.nextFloat() < 0.7F ? Blocks.GRASS_BLOCK : Blocks.GRAVEL).defaultBlockState(), 2);
            chasm = true; sumTop += top; n++;
        }
        if (chasm) slabs(level, r, cx, cz, sumTop / n, p);
        return any;
    }

    /**
     * Snaps the column to the nearest terrace: ground above it is cut away, a gap below it is filled
     * with stone under a dirt and grass skin. Returns the new top (first air block).
     */
    private static int level(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z, int top) {
        int ground = top - 1;
        int target = Math.round(ground / (float) STEP) * STEP;
        if (Math.abs(target - ground) > STEP / 2) return top;
        p.set(x, ground, z);
        if (!level.getFluidState(p).isEmpty()) return top;
        BlockState surface = level.getBlockState(p);
        if (!surface.is(Blocks.GRASS_BLOCK) && !surface.is(BlockTags.BASE_STONE_OVERWORLD) && !surface.is(BlockTags.DIRT)
                && !surface.is(Blocks.SNOW_BLOCK) && !surface.is(Blocks.GRAVEL) && !surface.is(Blocks.CALCITE)) return top;
        if (target < ground) {
            for (int y = ground + 1; y > target; y--) { p.set(x, y, z); level.setBlock(p, Blocks.AIR.defaultBlockState(), 2); }
        } else {
            for (int y = ground; y <= target; y++) {
                p.set(x, y, z);
                if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced() || y == ground)
                    level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
            }
        }
        p.set(x, target, z);
        level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        for (int y = target - 1; y >= target - 2; y--) { p.set(x, y, z); level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2); }
        p.set(x, target + 1, z);
        if (level.getBlockState(p).canBeReplaced() && !level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        return target + 1;
    }

    /** One to three broken-off slabs hover over the crack; two may be joined by a thin arch. */
    private static void slabs(WorldGenLevel level, RandomSource r, int cx, int cz, int avgTop, BlockPos.MutableBlockPos p) {
        if (r.nextFloat() > 0.65F) return;
        int count = 1 + (r.nextFloat() < 0.45F ? 1 : 0) + (r.nextFloat() < 0.15F ? 1 : 0);
        List<BlockPos> tops = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double rad = 2.5 + r.nextDouble() * 3.5;
            int ri = (int) Math.ceil(rad);
            // Keep the whole slab inside the origin chunk.
            int x0 = cx + ri + r.nextInt(Math.max(1, 16 - 2 * ri));
            int z0 = cz + ri + r.nextInt(Math.max(1, 16 - 2 * ri));
            BlockPos c = new BlockPos(x0, avgTop + 4 + r.nextInt(22), z0);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / rad;
                if (d > 1) continue;
                int down = (int) Math.round((1 - d) * rad * 1.8) + 1;
                for (int dy = -down; dy <= 0; dy++) {
                    p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if ((p.getX() & ~15) != cx || (p.getZ() & ~15) != cz) continue;
                    if (!level.getBlockState(p).isAir()) continue;
                    BlockState s = dy == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : dy == -1 ? Blocks.DIRT.defaultBlockState()
                            : (r.nextFloat() < 0.7F ? Blocks.STONE : Blocks.ANDESITE).defaultBlockState();
                    level.setBlock(p, s, 2);
                }
            }
            BlockPos above = c.above();
            if (level.getBlockState(above).isAir() && r.nextBoolean()) level.setBlock(above, Blocks.SHORT_GRASS.defaultBlockState(), 2);
            tops.add(c);
        }
        // A thin stone arch between the first two slabs, sagging a little in the middle.
        if (tops.size() >= 2) {
            BlockPos a = tops.get(0), b = tops.get(1);
            int steps = (int) Math.ceil(Math.sqrt(a.distSqr(b))) * 2;
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double sag = Math.sin(Math.PI * t) * -1.5;
                p.set((int) Math.round(a.getX() + (b.getX() - a.getX()) * t),
                      (int) Math.round(a.getY() - 1 + (b.getY() - a.getY()) * t + sag),
                      (int) Math.round(a.getZ() + (b.getZ() - a.getZ()) * t));
                if (level.getBlockState(p).isAir()) level.setBlock(p, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            }
        }
    }
}
