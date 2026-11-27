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
}
