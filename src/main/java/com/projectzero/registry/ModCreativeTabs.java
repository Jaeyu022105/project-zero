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
                    })
                    .build());
}
