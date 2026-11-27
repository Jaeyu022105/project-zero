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

    public static SurfaceRules.RuleSource rules() {
        // Capwood: mycelium drifts and podzol patches over the grass, with a little coarse dirt.
        SurfaceRules.RuleSource capwoodFloor = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.12D), MYCELIUM),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.25D), PODZOL),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.45D), COARSE_DIRT));
        SurfaceRules.RuleSource capwood = SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.CAPWOOD),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(),
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), capwoodFloor))));
        return SurfaceRules.sequence(capwood);
    }
}
