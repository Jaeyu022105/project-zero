package com.projectzero.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates a rare ancient gate with the terrain (placed by the {@code projectzero:ancient_gate}
 * biome modifier). Skips water, rivers and oceans, and slopes too steep for the dais to sit on.
 * The portal has no dungeon yet; one is allocated the first time a player steps through.
 */
public class AncientGateFeature extends Feature<NoneFeatureConfiguration> {
    /** Largest height difference across the footprint before we give up on the spot. */
    private static final int MAX_SLOPE = 3;

    public AncientGateFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();

        var biome = level.getBiome(origin);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
            return false;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -5; dx <= 5; dx += 5) {
            for (int dz = -5; dz <= 5; dz += 5) {
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX() + dx, origin.getZ() + dz);
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + dx, origin.getZ() + dz);
                if (surface > y) return false;   // water (or ice) on top: not here
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        if (max - min > MAX_SLOPE) return false;

        // Sit on the low side so the foundation, not the dais, absorbs the slope.
        BlockPos ground = new BlockPos(origin.getX(), min - 1, origin.getZ());
        if (ground.getY() <= level.getMinBuildHeight() + 4 || ground.getY() > level.getMaxBuildHeight() - 24) return false;

        GateRank rank = GateRules.randomRank(random);
        GateSize size = GateRules.randomSize(random);
        boolean alongX = random.nextBoolean();
        BlockPos base = AncientGate.baseFromGround(ground, size, alongX);

        AncientGate.placeUnlinkedPortal(level, base, rank, size, alongX, level.getLevelData().getGameTime());
        AncientGate.build(level, base, rank, size, alongX, AncientGate.paletteFor(level.getBiome(base), base),
                random, true);
        return true;
    }
}
