package com.projectzero.registry;

import com.mojang.serialization.MapCodec;
import com.projectzero.ProjectZero;
import com.projectzero.equipment.AccessoryLootModifier;
import com.projectzero.materials.MaterialLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Global loot modifier serializers for Project Zero. */
public final class ModLootModifiers {
    private ModLootModifiers() {}

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ProjectZero.MOD_ID);

    public static final RegistryObject<MapCodec<AccessoryLootModifier>> ACCESSORY_DROPS =
            LOOT_MODIFIERS.register("accessory_drops", () -> AccessoryLootModifier.CODEC);
    public static final RegistryObject<MapCodec<MaterialLootModifier>> MATERIAL_DROPS =
            LOOT_MODIFIERS.register("material_drops", () -> MaterialLootModifier.CODEC);
}
