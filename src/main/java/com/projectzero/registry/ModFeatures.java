package com.projectzero.registry;

import com.projectzero.ProjectZero;
import com.projectzero.gates.AncientGateFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** World generation features. Placement lives in data/projectzero/worldgen and forge/biome_modifier. */
public final class ModFeatures {
    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, ProjectZero.MOD_ID);

    /** Rare ancient gate in a stone ring on a dais (see gates/AncientGate). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ANCIENT_GATE =
            FEATURES.register("ancient_gate", AncientGateFeature::new);

    /** Giant elven-style tree for the Elderwood (see worldgen/ElderTreeFeature). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ELDER_TREE =
            FEATURES.register("elder_tree", com.projectzero.worldgen.ElderTreeFeature::new);

    /** Fallen tree trunk with an optional stump; the log block comes from the config. */
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration>> FALLEN_LOG =
            FEATURES.register("fallen_log", com.projectzero.worldgen.FallenLogFeature::new);
    /** Half-buried boulder of the configured block. */
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration>> BOULDER =
            FEATURES.register("boulder", com.projectzero.worldgen.BoulderFeature::new);

    /** Giant 28-48 block mushrooms with glowing gills (Capwood). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MEGA_MUSHROOM =
            FEATURES.register("mega_mushroom", com.projectzero.worldgen.MegaMushroomFeature::new);
    /** Sheer stone pillars with pines on top (Sky Pillars). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> STONE_SPIRE =
            FEATURES.register("stone_spire", com.projectzero.worldgen.StoneSpireFeature::new);
    /** Floating islands with hanging vines and waterfalls (Skyfall Isles). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FLOATING_ISLAND =
            FEATURES.register("floating_island", com.projectzero.worldgen.FloatingIslandFeature::new);
}
