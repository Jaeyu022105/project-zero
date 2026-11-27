package com.projectzero.worldgen;

import com.projectzero.ProjectZero;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Our overworld biomes. Each biome is data (data/projectzero/worldgen/biome/*.json); this class
 * holds their keys. {@link ProjectZeroRegion} places them in the world through TerraBlender and
 * {@link ModSurfaceRules} paints their ground.
 */
public final class ModBiomes {
    private ModBiomes() {}

    /** Giant-mushroom woods: huge caps among dark oaks, mycelium and podzol floor, drifting spores. */
    public static final ResourceKey<Biome> CAPWOOD = key("capwood");
    /** Elven woods: giant pale elder trees with leaf-cloud crowns and glowing berry vines, birch between them. */
    public static final ResourceKey<Biome> ELDERWOOD = key("elderwood");
    /** Sheer stone pillars over misty green valleys, pines and vines on top. */
    public static final ResourceKey<Biome> SKY_PILLARS = key("sky_pillars");
    /** Floating islands with waterfalls over lush flowered land. */
    public static final ResourceKey<Biome> SKYFALL_ISLES = key("skyfall_isles");

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ProjectZero.id(name));
    }
}
