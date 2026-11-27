package com.projectzero.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

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
