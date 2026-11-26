package com.projectzero.registry;

import com.projectzero.ProjectZero;
import com.projectzero.awakening.AwakeningShardItem;
import com.projectzero.awakening.ShardVesselItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ProjectZero.MOD_ID);

    public static final RegistryObject<Item> AWAKENING_STONE = ITEMS.register("awakening_stone",
            () -> new BlockItem(ModBlocks.AWAKENING_STONE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    /** Consumed by an Awakening Stone to reroll a job. Only gate bosses drop it (DESIGN-DOCUMENT.md §6). */
    public static final RegistryObject<Item> AWAKENING_SHARD = ITEMS.register("awakening_shard",
            () -> new AwakeningShardItem(new Item.Properties().rarity(Rarity.EPIC)));

    /** Holds one Awakening Shard and diverts half of kill XP into charging it. TODO(claude-art): placeholder icon. */
    public static final RegistryObject<Item> SHARD_VESSEL = ITEMS.register("shard_vessel",
            () -> new ShardVesselItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> AWAKENING_CRYSTAL = ITEMS.register("awakening_crystal",
            () -> new BlockItem(ModBlocks.AWAKENING_CRYSTAL.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> AWAKENING_CORE = ITEMS.register("awakening_core",
            () -> new BlockItem(ModBlocks.AWAKENING_CORE.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> AWAKENING_CRYSTAL_DEEP = ITEMS.register("awakening_crystal_deep",
            () -> new BlockItem(ModBlocks.AWAKENING_CRYSTAL_DEEP.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> AWAKENING_CLUSTER = ITEMS.register("awakening_cluster",
            () -> new BlockItem(ModBlocks.AWAKENING_CLUSTER.get(), new Item.Properties().rarity(Rarity.RARE)));
}
