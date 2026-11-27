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

    // Round 3 biomes.
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BAOBAB =
            FEATURES.register("baobab", com.projectzero.worldgen.BaobabFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TERMITE_MOUND =
            FEATURES.register("termite_mound", com.projectzero.worldgen.TermiteMoundFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TSINGY =
            FEATURES.register("tsingy", com.projectzero.worldgen.TsingyFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KARST_TOWER =
            FEATURES.register("karst_tower", com.projectzero.worldgen.KarstTowerFeature::new);

    // Round 4 biomes.
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SALT_POOL =
            FEATURES.register("salt_pool", com.projectzero.worldgen.SaltPoolFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> GEYSER =
            FEATURES.register("geyser", com.projectzero.worldgen.GeyserFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ICE_ARCH =
            FEATURES.register("ice_arch", com.projectzero.worldgen.IceArchFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CRYSTAL_OUTCROP =
            FEATURES.register("crystal_outcrop", com.projectzero.worldgen.CrystalOutcropFeature::new);

    // Round 5 biomes.
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MANA_WASTE =
            FEATURES.register("mana_waste", com.projectzero.worldgen.ManaWasteFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> DEAD_TREE =
            FEATURES.register("dead_tree", com.projectzero.worldgen.DeadTreeFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> GIANT_BAMBOO =
            FEATURES.register("giant_bamboo", com.projectzero.worldgen.GiantBambooFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SUNKEN_RUIN =
            FEATURES.register("sunken_ruin", com.projectzero.worldgen.SunkenRuinFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SHATTERED_PLATEAU =
            FEATURES.register("shattered_plateau", com.projectzero.worldgen.ShatteredPlateauFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FIREFLY_BUSH =
            FEATURES.register("firefly_bush", com.projectzero.worldgen.FireflyBushFeature::new);

    // Polish pass.
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BONE_REMAINS =
            FEATURES.register("bone_remains", com.projectzero.worldgen.BoneRemainsFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KOPJE =
            FEATURES.register("kopje", com.projectzero.worldgen.KopjeFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SALT_MOUND =
            FEATURES.register("salt_mound", com.projectzero.worldgen.SaltMoundFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TRAVERTINE_TERRACE =
            FEATURES.register("travertine_terrace", com.projectzero.worldgen.TravertineTerraceFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FROZEN_POND =
            FEATURES.register("frozen_pond", com.projectzero.worldgen.FrozenPondFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MANA_CRATER =
            FEATURES.register("mana_crater", com.projectzero.worldgen.ManaCraterFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FAIRY_RING =
            FEATURES.register("fairy_ring", com.projectzero.worldgen.FairyRingFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ELVEN_RUIN =
            FEATURES.register("elven_ruin", com.projectzero.worldgen.ElvenRuinFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TREE_FERN =
            FEATURES.register("tree_fern", com.projectzero.worldgen.TreeFernFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ICE_SHARD =
            FEATURES.register("ice_shard", com.projectzero.worldgen.IceShardFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> STILT_HUT =
            FEATURES.register("stilt_hut", com.projectzero.worldgen.StiltHutFeature::new);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType<?>> TREE_DECORATORS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.TREE_DECORATOR_TYPE, ProjectZero.MOD_ID);
    /** Buttress roots at the base of a tree (see worldgen/RootsDecorator). */
    public static final RegistryObject<net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType<com.projectzero.worldgen.RootsDecorator>> ROOTS =
            TREE_DECORATORS.register("roots", () -> new net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType<>(com.projectzero.worldgen.RootsDecorator.CODEC));
}
