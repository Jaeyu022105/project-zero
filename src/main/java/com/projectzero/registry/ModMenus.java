package com.projectzero.registry;

import com.projectzero.ProjectZero;
import com.projectzero.equipment.AccessoryMenu;
import com.projectzero.equipment.BackpackMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Container menu types for Project Zero. */
public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ProjectZero.MOD_ID);

    public static final RegistryObject<MenuType<AccessoryMenu>> ACCESSORIES =
            MENUS.register("accessories", () -> new MenuType<>(AccessoryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<BackpackMenu>> BACKPACK = MENUS.register("backpack",
            () -> net.minecraftforge.common.extensions.IForgeMenuType.create(BackpackMenu::new));
}
