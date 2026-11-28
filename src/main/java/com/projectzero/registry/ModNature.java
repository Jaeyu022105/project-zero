package com.projectzero.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;
import java.util.Optional;

/**
 * Our world-generation blocks (biomes, giant mushrooms, trees). Registered into the shared
 * {@link ModBlocks#BLOCKS} and {@link ModItems#ITEMS}; {@link #init()} only forces class loading
 * so the entries exist before the registry events fire.
 */
public final class ModNature {
    private ModNature() {}

    public static void init() {}

    /** Glowing gill layer under giant mushroom caps. */
    public static final RegistryObject<Block> GLOWCAP_GILLS = block("glowcap_gills",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.2F)
                    .sound(SoundType.WART_BLOCK).lightLevel(s -> 12)));

    /** Cap of the violet giant mushroom. */
    public static final RegistryObject<Block> VIOLET_CAP = block("violet_cap",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.2F)
                    .sound(SoundType.WOOD)));

    // ---- Skywood: the pale tree of the Skyfall Isles ----
    public static final RegistryObject<Block> SKYWOOD_LOG = block("skywood_log",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BIRCH_LOG)
                    .mapColor(MapColor.QUARTZ)));
    public static final RegistryObject<Block> SKYWOOD_PLANKS = block("skywood_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BIRCH_PLANKS)
                    .mapColor(MapColor.ICE)));
    public static final RegistryObject<Block> STRIPPED_SKYWOOD_LOG = block("stripped_skywood_log",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_BIRCH_LOG)
                    .mapColor(MapColor.QUARTZ)));
    public static final RegistryObject<Block> SKYWOOD_WOOD = block("skywood_wood",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_WOOD)
                    .mapColor(MapColor.QUARTZ)));
    public static final RegistryObject<Block> STRIPPED_SKYWOOD_WOOD = block("stripped_skywood_wood",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_BIRCH_WOOD)
                    .mapColor(MapColor.QUARTZ)));
    public static final RegistryObject<Block> SKYWOOD_SLAB = block("skywood_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_SLAB)));
    public static final RegistryObject<Block> SKYWOOD_STAIRS = block("skywood_stairs",
            () -> new StairBlock(SKYWOOD_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_STAIRS)));
    public static final RegistryObject<Block> SKYWOOD_FENCE = block("skywood_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_FENCE)));
    public static final RegistryObject<Block> SKYWOOD_FENCE_GATE = block("skywood_fence_gate",
            () -> new FenceGateBlock(WoodType.BIRCH, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_FENCE_GATE)));
    public static final RegistryObject<Block> SKYWOOD_DOOR = block("skywood_door",
            () -> new DoorBlock(BlockSetType.BIRCH, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_DOOR)));
    public static final RegistryObject<Block> SKYWOOD_TRAPDOOR = block("skywood_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.BIRCH, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_TRAPDOOR)));
    public static final RegistryObject<Block> SKYWOOD_BUTTON = block("skywood_button",
            () -> new ButtonBlock(BlockSetType.BIRCH, com.projectzero.nature.NatureRules.BUTTON_TICKS,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_BUTTON)));
    public static final RegistryObject<Block> SKYWOOD_PRESSURE_PLATE = block("skywood_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.BIRCH, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_PRESSURE_PLATE)));
    private static final ResourceKey<ConfiguredFeature<?, ?>> SKYWOOD_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            com.projectzero.ProjectZero.id("skywood"));
    public static final RegistryObject<Block> SKYWOOD_SAPLING = block("skywood_sapling",
            () -> new SaplingBlock(new TreeGrower("skywood", Optional.of(SKYWOOD_TREE), Optional.empty(), Optional.empty()),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_SAPLING)));
    public static final RegistryObject<Block> SKYWOOD_LEAVES = block("skywood_leaves",
            () -> new net.minecraft.world.level.block.LeavesBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CHERRY_LEAVES)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)));

    // ---- Round 3 ----
    /** Pale grey-cream limestone: Tsingy needles and karst towers. */
    public static final RegistryObject<Block> LIMESTONE = block("limestone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CALCITE)
                    .mapColor(MapColor.TERRACOTTA_WHITE).strength(1.2F, 4F)));
    /** Baobab: thick grey-brown bottle trunks of the Baobab Savanna. */
    public static final RegistryObject<Block> BAOBAB_LOG = block("baobab_log",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.ACACIA_LOG)
                    .mapColor(MapColor.TERRACOTTA_GRAY)));
    public static final RegistryObject<Block> BAOBAB_PLANKS = block("baobab_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.ACACIA_PLANKS)
                    .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)));
    public static final RegistryObject<Block> STRIPPED_BAOBAB_LOG = block("stripped_baobab_log",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_ACACIA_LOG)
                    .mapColor(MapColor.TERRACOTTA_GRAY)));
    public static final RegistryObject<Block> BAOBAB_WOOD = block("baobab_wood",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_WOOD)
                    .mapColor(MapColor.TERRACOTTA_GRAY)));
    public static final RegistryObject<Block> STRIPPED_BAOBAB_WOOD = block("stripped_baobab_wood",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_ACACIA_WOOD)
                    .mapColor(MapColor.TERRACOTTA_GRAY)));
    public static final RegistryObject<Block> BAOBAB_SLAB = block("baobab_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_SLAB)));
    public static final RegistryObject<Block> BAOBAB_STAIRS = block("baobab_stairs",
            () -> new StairBlock(BAOBAB_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_STAIRS)));
    public static final RegistryObject<Block> BAOBAB_FENCE = block("baobab_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE)));
    public static final RegistryObject<Block> BAOBAB_FENCE_GATE = block("baobab_fence_gate",
            () -> new FenceGateBlock(WoodType.ACACIA, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE_GATE)));
    public static final RegistryObject<Block> BAOBAB_DOOR = block("baobab_door",
            () -> new DoorBlock(BlockSetType.ACACIA, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_DOOR)));
    public static final RegistryObject<Block> BAOBAB_TRAPDOOR = block("baobab_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.ACACIA, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_TRAPDOOR)));
    public static final RegistryObject<Block> BAOBAB_BUTTON = block("baobab_button",
            () -> new ButtonBlock(BlockSetType.ACACIA, com.projectzero.nature.NatureRules.BUTTON_TICKS,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_BUTTON)));
    public static final RegistryObject<Block> BAOBAB_PRESSURE_PLATE = block("baobab_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.ACACIA, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_PRESSURE_PLATE)));
    private static final ResourceKey<ConfiguredFeature<?, ?>> BAOBAB_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            com.projectzero.ProjectZero.id("baobab"));
    public static final RegistryObject<Block> BAOBAB_SAPLING = block("baobab_sapling",
            () -> new SaplingBlock(new TreeGrower("baobab", Optional.of(BAOBAB_TREE), Optional.empty(), Optional.empty()),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_SAPLING)));
    // ---- Round 4 ----
    /** White salt crust with hexagon cracks: Salt Mirror Flats ground. */
    public static final RegistryObject<Block> SALT_CRUST = block("salt_crust",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CALCITE)
                    .mapColor(MapColor.SNOW).strength(0.6F).sound(SoundType.TUFF)));
    /** Solid glowing mana crystal: Crystal Tundra outcrops. */
    public static final RegistryObject<Block> MANA_CRYSTAL_BLOCK = block("mana_crystal_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.AMETHYST_BLOCK)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 10).emissiveRendering((s, l, p) -> true)));
    /** Softly glowing night-firefly bush (light 6, shears/Silk Touch drop). TODO(claude-art): placeholder texture. */
    public static final RegistryObject<Block> FIREFLY_BUSH = block("firefly_bush",
            () -> new com.projectzero.worldgen.FireflyBushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .lightLevel(s -> 6)
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    private static RegistryObject<Block> block(String name, Supplier<Block> make) {
        RegistryObject<Block> b = ModBlocks.BLOCKS.register(name, make);
        ModItems.ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }
}
