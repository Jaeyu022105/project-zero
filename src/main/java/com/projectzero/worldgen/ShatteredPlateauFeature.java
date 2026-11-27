package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Shattered Sky Plateau: deep chasms split the plateau into pieces, following world-wide noise so
 * they run on unbroken from chunk to chunk; broken-off slabs of the plateau hang in the air over
 * them. Runs once per chunk and handles the whole chunk.
 */
public class ShatteredPlateauFeature extends Feature<NoneFeatureConfiguration> {
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
        boolean chasm = false;
        int sumTop = 0, n = 0;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = cx + dx, z = cz + dz;
            double v = Math.abs(noise.getValue(x / 110.0, z / 110.0));
            double w = 0.06 + 0.03 * depthNoise.getValue(x / 60.0, z / 60.0);
            if (v > w) continue;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            p.set(x, top, z);
            if (!level.getBiome(p).is(ModBiomes.SHATTERED_PLATEAU)) continue;
            // Deepest along the middle of the crack.
            int depth = (int) (45 + 35 * (1 - v / w) + 15 * depthNoise.getValue(x / 40.0, z / 40.0));
            int bottom = Math.max(minY, top - depth);
            for (int y = top; y >= bottom; y--) {
                p.set(x, y, z);
                if (level.getBlockState(p).is(Blocks.BEDROCK) || !level.getFluidState(p).isEmpty()) break;
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
            chasm = true; sumTop += top; n++;
        }
        // A broken-off slab hovering over the chasm in this chunk.
        if (chasm && r.nextFloat() < 0.35F) {
            int y = sumTop / n + 6 + r.nextInt(20);
            BlockPos c = new BlockPos(cx + 4 + r.nextInt(8), y, cz + 4 + r.nextInt(8));
            double rad = 2.5 + r.nextDouble() * 3;
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / rad;
                if (d > 1) continue;
                int down = (int) Math.round((1 - d) * rad * 1.6) + 1;
                for (int dy = -down; dy <= 0; dy++) {
                    p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (!level.getBlockState(p).isAir()) continue;
                    level.setBlock(p, (dy == 0 ? Blocks.GRASS_BLOCK : dy == -1 ? Blocks.DIRT : r.nextFloat() < 0.7F ? Blocks.STONE : Blocks.ANDESITE).defaultBlockState(), 2);
                }
            }
            BlockPos topAt = c.above();
            if (r.nextBoolean() && level.getBlockState(topAt).isAir()) level.setBlock(topAt, Blocks.SHORT_GRASS.defaultBlockState(), 2);
        }
        return chasm;
    }
}
