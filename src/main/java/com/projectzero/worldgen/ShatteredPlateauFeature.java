package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Shattered Sky Plateau, in two passes that each handle one whole chunk.
 * <ul>
 * <li>{@link #shape()} (raw generation, before everything else in the biome): lifts and levels the
 * ground into one high, flat table at {@link #TABLE} (with a few low terraces), easing back to the
 * natural ground near the biome edge, then cuts narrow chasms down through it in stepped ledges. The
 * chasms follow world-wide noise so they run on unbroken from chunk to chunk.</li>
 * <li>{@link #sky()} (last in vegetation): broken-off slabs of the table float over the chasms, and
 * natural tuff arches span them.</li>
 * </ul>
 * Floating rock is tuff on the underside, so a neighbour chunk levelled later stops its cut there and
 * never slices through a slab or an arch foot that reached into it. Biomes away from the chunk are read
 * from the biome source, never through the level (see AGENTS.md, worldgen rule).
 */
public class ShatteredPlateauFeature extends Feature<NoneFeatureConfiguration> {
    /** Height of the table top. Client mist reads this too (deeper below it = thicker cloud). */
    public static final int TABLE = 168;

    private static SimplexNoise crack, warp, depthNoise, terrace, patch;
    private static long noiseSeed = Long.MIN_VALUE;

    private final boolean sky;

    private ShatteredPlateauFeature(boolean sky) {
        super(NoneFeatureConfiguration.CODEC);
        this.sky = sky;
    }

    public static ShatteredPlateauFeature shape() { return new ShatteredPlateauFeature(false); }
    public static ShatteredPlateauFeature sky() { return new ShatteredPlateauFeature(true); }

    private static synchronized void init(long seed) {
        if (crack != null && seed == noiseSeed) return;
        crack = new SimplexNoise(new LegacyRandomSource(seed ^ 0x5A77E2EDL));
        warp = new SimplexNoise(new LegacyRandomSource(seed ^ 0x3A2F1L));
        depthNoise = new SimplexNoise(new LegacyRandomSource(seed ^ 0x0DEE9L));
        terrace = new SimplexNoise(new LegacyRandomSource(seed ^ 0x7E2ACEL));
        patch = new SimplexNoise(new LegacyRandomSource(seed ^ 0x9A7C4L));
        noiseSeed = seed;
    }

    /** Table height at a column: flat, with a few broad terraces a block or four apart. */
    static int tableAt(int x, int z) {
        double t = terrace.getValue(x / 220.0, z / 220.0);
        return TABLE + (t > 0.45 ? 4 : t < -0.45 ? -3 : 0);
    }

    /** 0 outside a chasm, rising to 1 along the middle of the crack. */
    static double chasmAt(int x, int z) {
        // A little domain warp so cracks zigzag and read sharp instead of smoothly curving.
        double wx = x + 7 * warp.getValue(x / 26.0, z / 26.0), wz = z + 7 * warp.getValue(x / 26.0 + 40, z / 26.0 + 40);
        double v = Math.abs(crack.getValue(wx / 150.0, wz / 150.0));
        double w = 0.055 + 0.018 * depthNoise.getValue(x / 70.0, z / 70.0);
        return v >= w ? 0 : 1 - v / w;
    }

    /** How far down a chasm column is cut below the table: stepped ledges, deepest in the middle. */
    static int chasmDepth(int x, int z, double t) {
        double j = t + 0.09 * depthNoise.getValue(x / 9.0, z / 9.0);
        if (j < 0.28) return 4;
        if (j < 0.50) return 12;
        if (j < 0.72) return 24;
        return 58 + (int) (18 * depthNoise.getValue(x / 35.0, z / 35.0));
    }

    /** Share of plateau biome in a ring around a point, from the biome source (safe at any distance). */
    static double plateauWeight(FeaturePlaceContext<?> ctx, int x, int z) {
        BiomeSource src = ctx.chunkGenerator().getBiomeSource();
        Climate.Sampler sampler = ctx.level().getLevel().getChunkSource().randomState().sampler();
        int qy = QuartPos.fromBlock(TABLE);
        int hit = 0, n = 0;
        for (int ring = 0; ring <= 2; ring++) {
            int rad = ring * 12, dirs = ring == 0 ? 1 : 8;
            for (int d = 0; d < dirs; d++) {
                double a = d * Math.PI / 4;
                int sx = x + (int) Math.round(Math.cos(a) * rad), sz = z + (int) Math.round(Math.sin(a) * rad);
                if (src.getNoiseBiome(QuartPos.fromBlock(sx), qy, QuartPos.fromBlock(sz), sampler).is(ModBiomes.SHATTERED_PLATEAU)) hit++;
                n++;
            }
        }
        return hit / (double) n;
    }

    /** Ground cover on the table and on ledges: mostly grass, with bare stone and coarse dirt patches. */
    private static BlockState topBlock(int x, int z) {
        double p = patch.getValue(x / 14.0, z / 14.0);
        return (p > 0.55 ? Blocks.STONE : p > 0.38 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK).defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        init(ctx.level().getSeed());
        return sky ? placeSky(ctx) : placeShape(ctx);
    }

    // ---------------------------------------------------------------- shaping

    private boolean placeShape(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        int cx = ctx.origin().getX() & ~15, cz = ctx.origin().getZ() & ~15;
        int minY = level.getMinBuildHeight() + 12, maxY = level.getMaxBuildHeight() - 1;
        // Plateau weight on a 5x5 grid of 4-block cells, interpolated per column.
        double[][] wgt = new double[5][5];
        boolean any = false;
        for (int i = 0; i < 5; i++) for (int k = 0; k < 5; k++) {
            wgt[i][k] = plateauWeight(ctx, cx + i * 4, cz + k * 4);
            any |= wgt[i][k] > 0;
        }
        if (!any) return false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE.defaultBlockState(), dirt = Blocks.DIRT.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = cx + dx, z = cz + dz;
            double fx = dx / 4.0, fz = dz / 4.0;
            int i = Math.min((int) fx, 3), k = Math.min((int) fz, 3);
            double w = Mth.lerp2(fx - i, fz - k, wgt[i][k], wgt[i + 1][k], wgt[i][k + 1], wgt[i + 1][k + 1]);
            if (w <= 0) continue;
            // Steep rise at the edge: the table ends in a cliff band, not a long ramp.
            double s = Mth.smoothstep(Mth.clamp((w - 0.25) / 0.55, 0, 1));
            int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            int target = (int) Math.round(Mth.lerp(s, ground, tableAt(x, z)));
            target = Mth.clamp(target, minY + 8, maxY - 8);

            p.set(x, target, z);
            if (!level.getBlockState(p).isAir()) {
                // Too high here: cut the rock above the table away, up to the first air (or floating tuff).
                for (int y = target + 1; y < maxY; y++) {
                    p.setY(y);
                    BlockState b = level.getBlockState(p);
                    if (b.isAir() || b.is(Blocks.TUFF)) break;
                    level.setBlock(p, air, 2);
                }
            } else {
                // Too low: build the ground up to the table.
                int g = target;
                while (g > minY) {
                    p.setY(g);
                    if (!level.getBlockState(p).isAir()) break;
                    g--;
                }
                for (int y = g + 1; y <= target; y++) {
                    p.setY(y);
                    level.setBlock(p, stone, 2);
                }
            }
            // Fresh ground cover on top.
            p.setY(target);
            level.setBlock(p, topBlock(x, z), 2);
            for (int y = target - 1; y >= target - 3; y--) {
                p.setY(y);
                if (!level.getBlockState(p).isAir()) level.setBlock(p, dirt, 2);
            }

            // Chasms only well inside the biome.
            if (s < 0.98) continue;
            double t = chasmAt(x, z);
            if (t <= 0) continue;
            int bottom = Math.max(minY, target - chasmDepth(x, z, t));
            for (int y = target; y > bottom; y--) {
                p.setY(y);
                if (level.getBlockState(p).is(Blocks.BEDROCK)) break;
                level.setBlock(p, air, 2);
            }
            // Ledges keep a skin of moss and grass; the deep floor is scree.
            p.setY(bottom);
            if (!level.getBlockState(p).isAir() && !level.getBlockState(p).is(Blocks.BEDROCK)) {
                boolean deep = target - bottom > 30;
                level.setBlock(p, (deep ? (patch.getValue(x / 6.0, z / 6.0) > 0 ? Blocks.GRAVEL : Blocks.STONE)
                        : patch.getValue(x / 5.0, z / 5.0) > 0.2 ? Blocks.MOSS_BLOCK : Blocks.GRASS_BLOCK).defaultBlockState(), 2);
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- slabs and arches

    private boolean placeSky(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        int cx = ctx.origin().getX() & ~15, cz = ctx.origin().getZ() & ~15;
        // Find the chasm columns in this chunk (local biome reads only).
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int found = 0, sx = 0, sz = 0;
        for (int dx = 1; dx < 16; dx += 2) for (int dz = 1; dz < 16; dz += 2) {
            int x = cx + dx, z = cz + dz;
            if (chasmAt(x, z) < 0.5) continue;
            p.set(x, TABLE, z);
            if (!level.getBiome(p).is(ModBiomes.SHATTERED_PLATEAU)) continue;
            found++;
            if (r.nextInt(found) == 0) { sx = x; sz = z; }
        }
        if (found == 0) return false;
        boolean placed = false;
        // One or two slabs over most chasm chunks.
        int slabs = r.nextFloat() < 0.7F ? 1 + (r.nextFloat() < 0.35F ? 1 : 0) : 0;
        for (int i = 0; i < slabs; i++) placed |= slab(level, r, cx, cz, p);
        if (r.nextFloat() < 0.3F) placed |= arch(ctx, r, sx, sz, cx, cz, p);
        return placed;
    }

    private static boolean inReach(int x, int z, int cx, int cz) {
        return x >= cx - 15 && x <= cx + 30 && z >= cz - 15 && z <= cz + 30;
    }

    private static void put(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z, BlockState s, int cx, int cz) {
        if (!inReach(x, z, cx, cz)) return;
        p.set(x, y, z);
        if (level.getBlockState(p).isAir()) level.setBlock(p, s, 2);
    }

    /** A broken-off piece of the table: grassy top, a jagged tuff underside tapering to a point. */
    private static boolean slab(WorldGenLevel level, RandomSource r, int cx, int cz, BlockPos.MutableBlockPos p) {
        double rx = 4 + r.nextDouble() * 5, rz = 4 + r.nextDouble() * 5;
        int ox = cx + 4 + r.nextInt(8), oz = cz + 4 + r.nextInt(8);
        int top = TABLE + 7 + r.nextInt(26);
        double tilt = (r.nextDouble() - 0.5) * 0.25;   // a slight list, as if it broke off and drifted
        double rot = r.nextDouble() * Math.PI, cos = Math.cos(rot), sin = Math.sin(rot);
        int ri = (int) Math.ceil(Math.max(rx, rz)) + 1;
        BlockState tuff = Blocks.TUFF.defaultBlockState(), dirt = Blocks.DIRT.defaultBlockState();
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            double ax = (dx * cos + dz * sin) / rx, az = (-dx * sin + dz * cos) / rz;
            double d = Math.sqrt(ax * ax + az * az) + 0.12 * patch.getValue((ox + dx) / 3.0, (oz + dz) / 3.0);
            if (d > 1) continue;
            int x = ox + dx, z = oz + dz;
            int y0 = top + (int) Math.round(dx * tilt);
            int down = (int) Math.round((1 - d * d) * (rx + rz) * 0.9 + 2 * depthNoise.getValue(x / 4.0, z / 4.0)) + 2;
            put(level, p, x, y0, z, topBlock(x, z), cx, cz);
            put(level, p, x, y0 - 1, z, dirt, cx, cz);
            for (int y = y0 - 2; y >= y0 - down; y--)
                put(level, p, x, y, z, (r.nextFloat() < 0.12F ? Blocks.ANDESITE : Blocks.TUFF).defaultBlockState(), cx, cz);
            // The lowest block is always tuff, so a later levelling cut stops under the slab.
            put(level, p, x, y0 - down - 1, z, tuff, cx, cz);
            if (r.nextFloat() < 0.08F) put(level, p, x, y0 - down - 2, z, Blocks.HANGING_ROOTS.defaultBlockState(), cx, cz);
            if (d < 0.8 && r.nextFloat() < 0.18F) put(level, p, x, y0 + 1, z, Blocks.SHORT_GRASS.defaultBlockState(), cx, cz);
        }
        return true;
    }

    /** A natural tuff arch across the chasm, rising from the table on both rims. */
    private static boolean arch(FeaturePlaceContext<NoneFeatureConfiguration> ctx, RandomSource r, int sx, int sz, int cx, int cz, BlockPos.MutableBlockPos p) {
        WorldGenLevel level = ctx.level();
        // Across the crack: along the gradient of the chasm value.
        double gx = chasmAt(sx + 2, sz) - chasmAt(sx - 2, sz), gz = chasmAt(sx, sz + 2) - chasmAt(sx, sz - 2);
        double gl = Math.sqrt(gx * gx + gz * gz);
        if (gl < 1e-4) return false;
        gx /= gl; gz /= gl;
        // Walk out each way to the rim, then a few blocks onto the table.
        int a = rim(sx, sz, -gx, -gz), b = rim(sx, sz, gx, gz);
        if (a < 0 || b < 0) return false;
        a += 3; b += 3;
        int ax = (int) Math.round(sx - gx * a), az = (int) Math.round(sz - gz * a);
        int bx = (int) Math.round(sx + gx * b), bz = (int) Math.round(sz + gz * b);
        if (!inReach(ax, az, cx, cz) || !inReach(bx, bz, cx, cz)) return false;
        // Both feet stand well inside the plateau, where the table is flat.
        if (plateauWeight(ctx, ax, az) < 0.9 || plateauWeight(ctx, bx, bz) < 0.9) return false;
        int ya = tableAt(ax, az), yb = tableAt(bx, bz);
        double len = Math.hypot(bx - ax, bz - az);
        double rise = 7 + r.nextInt(7) + len * 0.15;
        double thick = 2.2 + r.nextDouble() * 1.2, half = 1.4 + r.nextDouble() * 0.8;
        double px = -(bz - az) / len, pz = (bx - ax) / len;   // sideways
        BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
        int steps = (int) Math.ceil(len * 2);
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps;
            double x = Mth.lerp(t, ax, bx), z = Mth.lerp(t, az, bz);
            double yc = Mth.lerp(t, ya, yb) + rise * 4 * t * (1 - t);
            // Thicker at the feet, thinnest at the keystone.
            double th = thick + 3.5 * Math.pow(Math.abs(t - 0.5) * 2, 3);
            double hw = half + 1.2 * Math.pow(Math.abs(t - 0.5) * 2, 3);
            for (double o = -hw; o <= hw; o += 0.5) {
                int bxx = (int) Math.floor(x + px * o), bzz = (int) Math.floor(z + pz * o);
                int yTop = (int) Math.round(yc);
                int yBot = (int) Math.round(yc - th);
                // Feet reach down to the table so the arch stands on it.
                if (t < 0.12 || t > 0.88) yBot = Math.min(yBot, (int) Math.round(Mth.lerp(t, ya, yb)) + 1);
                for (int y = yBot; y < yTop; y++) put(level, p, bxx, y, bzz, Blocks.TUFF.defaultBlockState(), cx, cz);
                put(level, p, bxx, yTop, bzz, Math.abs(o) < hw - 0.6 ? moss : Blocks.TUFF.defaultBlockState(), cx, cz);
            }
        }
        return true;
    }

    /** Blocks walked from a chasm column along a direction until outside the chasm; -1 if too far. */
    private static int rim(int x, int z, double dx, double dz) {
        for (int i = 1; i <= 14; i++)
            if (chasmAt((int) Math.round(x + dx * i), (int) Math.round(z + dz * i)) <= 0) return i;
        return -1;
    }
}
