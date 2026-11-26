package com.projectzero.registry;

import com.projectzero.ProjectZero;
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
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> AWAKENING_CRYSTAL = ITEMS.register("awakening_crystal",
            () -> new BlockItem(ModBlocks.AWAKENING_CRYSTAL.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> AWAKENING_CORE = ITEMS.register("awakening_core",
            () -> new BlockItem(ModBlocks.AWAKENING_CORE.get(), new Item.Properties().rarity(Rarity.RARE)));
}
