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
    public static final int WEIGHT = 3;

    public ProjectZeroRegion(ResourceLocation name) {
        super(name, RegionType.OVERWORLD, WEIGHT);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.DARK_FOREST, ModBiomes.CAPWOOD);
        });
    }
}
