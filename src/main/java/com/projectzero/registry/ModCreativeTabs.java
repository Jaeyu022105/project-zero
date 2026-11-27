package com.projectzero.registry;

import com.projectzero.ProjectZero;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ProjectZero.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.projectzero"))
                    .icon(() -> new ItemStack(ModItems.AWAKENING_SHARD.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.AWAKENING_STONE.get());
                        output.accept(ModItems.AWAKENING_SHARD.get());
                        output.accept(ModItems.SHARD_VESSEL.get());
                        output.accept(ModItems.SKILLBOOK.get());
                        output.accept(ModItems.MANA_CRYSTAL_BLOCK.get());
                        output.accept(ModItems.MANA_CRYSTAL_SHARD.get());
                        output.accept(ModItems.AWAKENING_CRYSTAL.get());
                        output.accept(ModItems.AWAKENING_CORE.get());
                        output.accept(ModItems.AWAKENING_CRYSTAL_DEEP.get());
                        output.accept(ModItems.AWAKENING_CLUSTER.get());
                        output.accept(ModItems.IRON_GAUNTLETS.get());
                        output.accept(ModItems.COPPER_RING.get());
                        output.accept(ModItems.SILVER_RING.get());
                        output.accept(ModItems.GOLD_RING.get());
                        output.accept(ModItems.BONE_AMULET.get());
                        output.accept(ModItems.OBSIDIAN_EARRINGS.get());
                        output.accept(ModItems.LEATHER_BELT.get());
                        output.accept(ModItems.HUNTER_CHARM.get());
                        output.accept(ModItems.MANA_CHARM.get());
                    })
                    .build());
}
