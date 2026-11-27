package com.projectzero.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * Our share of the overworld (TerraBlender). Inside this region the vanilla climate layout is kept,
 * and some vanilla biomes are swapped for ours, so our biomes sit where their vanilla cousins would
 * and the world still feels like Minecraft. Weight sets how much of the world uses this region.
 */
public class ProjectZeroRegion extends Region {
    public static final int WEIGHT = 30;   // vanilla's own region is 10: ours covers about three quarters of the world

    public ProjectZeroRegion(ResourceLocation name) {
        super(name, RegionType.OVERWORLD, WEIGHT);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            // Common biomes on purpose: our share has to read as most of the world, not rare patches.
            builder.replaceBiome(Biomes.DARK_FOREST, ModBiomes.CAPWOOD);
            builder.replaceBiome(Biomes.SWAMP, ModBiomes.CAPWOOD);
            builder.replaceBiome(Biomes.FOREST, ModBiomes.ELDERWOOD);
            builder.replaceBiome(Biomes.BIRCH_FOREST, ModBiomes.ELDERWOOD);
            builder.replaceBiome(Biomes.OLD_GROWTH_BIRCH_FOREST, ModBiomes.ELDERWOOD);
            builder.replaceBiome(Biomes.WINDSWEPT_HILLS, ModBiomes.SKY_PILLARS);
            builder.replaceBiome(Biomes.WINDSWEPT_FOREST, ModBiomes.SKY_PILLARS);
            builder.replaceBiome(Biomes.WINDSWEPT_GRAVELLY_HILLS, ModBiomes.SKY_PILLARS);
            builder.replaceBiome(Biomes.JUNGLE, ModBiomes.SKY_PILLARS);
            builder.replaceBiome(Biomes.SPARSE_JUNGLE, ModBiomes.SKY_PILLARS);
            builder.replaceBiome(Biomes.BAMBOO_JUNGLE, ModBiomes.SKY_PILLARS);
            builder.replaceBiome(Biomes.MEADOW, ModBiomes.SKYFALL_ISLES);
            builder.replaceBiome(Biomes.FLOWER_FOREST, ModBiomes.SKYFALL_ISLES);
            builder.replaceBiome(Biomes.PLAINS, ModBiomes.SKYFALL_ISLES);
            builder.replaceBiome(Biomes.SUNFLOWER_PLAINS, ModBiomes.SKYFALL_ISLES);
            builder.replaceBiome(Biomes.TAIGA, ModBiomes.CLOUD_FOREST);
            builder.replaceBiome(Biomes.SAVANNA, ModBiomes.BAOBAB_SAVANNA);
            builder.replaceBiome(Biomes.SAVANNA_PLATEAU, ModBiomes.BAOBAB_SAVANNA);
            builder.replaceBiome(Biomes.BADLANDS, ModBiomes.TSINGY);
            builder.replaceBiome(Biomes.ERODED_BADLANDS, ModBiomes.TSINGY);
            builder.replaceBiome(Biomes.WARM_OCEAN, ModBiomes.KARST_BAY);
            builder.replaceBiome(Biomes.LUKEWARM_OCEAN, ModBiomes.KARST_BAY);
        });
    }
}
