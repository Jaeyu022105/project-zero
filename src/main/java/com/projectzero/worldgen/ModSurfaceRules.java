package com.projectzero.worldgen;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;

/** Ground cover for our biomes, added in front of vanilla's rules through TerraBlender. */
public final class ModSurfaceRules {
    private ModSurfaceRules() {}

    private static final SurfaceRules.RuleSource MYCELIUM = SurfaceRules.state(Blocks.MYCELIUM.defaultBlockState());
    private static final SurfaceRules.RuleSource PODZOL = SurfaceRules.state(Blocks.PODZOL.defaultBlockState());
    private static final SurfaceRules.RuleSource COARSE_DIRT = SurfaceRules.state(Blocks.COARSE_DIRT.defaultBlockState());
    private static final SurfaceRules.RuleSource TERRACOTTA = SurfaceRules.state(Blocks.TERRACOTTA.defaultBlockState());
    // Built lazily (blocks registered by then): surface rules are created during common setup.
    private static SurfaceRules.RuleSource LIMESTONE;

    public static SurfaceRules.RuleSource rules() {
        LIMESTONE = SurfaceRules.state(com.projectzero.registry.ModNature.LIMESTONE.get().defaultBlockState());
        // Capwood: mostly mycelium, with podzol and coarse dirt where the surface noise dips.
        SurfaceRules.RuleSource capwoodFloor = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.30D), MYCELIUM),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.55D), PODZOL),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.70D), COARSE_DIRT));
        SurfaceRules.RuleSource capwood = SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.CAPWOOD),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(),
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), capwoodFloor))));
        // Baobab Savanna: grass with patches of red earth (terracotta) and coarse dirt.
        SurfaceRules.RuleSource baobab = SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.BAOBAB_SAVANNA),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(),
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.45D), TERRACOTTA),
                                        SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25D), COARSE_DIRT))))));
        // Tsingy: bare limestone with pockets of coarse dirt where shrubs take hold.
        SurfaceRules.RuleSource tsingy = SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.TSINGY),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.1D), LIMESTONE),
                                COARSE_DIRT)),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, LIMESTONE))));
        return SurfaceRules.sequence(capwood, baobab, tsingy);
    }
}
