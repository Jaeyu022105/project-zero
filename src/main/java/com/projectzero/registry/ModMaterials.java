package com.projectzero.registry;

import com.projectzero.materials.MaterialRules;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/** Monster parts and rank-colored gate cores, registered in the shared item registry. */
public final class ModMaterials {
    private ModMaterials() {}

    public static void init() {}

    public static final RegistryObject<Item> BEAST_FANG = material("beast_fang");
    public static final RegistryObject<Item> BEAST_CLAW = material("beast_claw");
    public static final RegistryObject<Item> BEAST_PELT = material("beast_pelt");
    public static final RegistryObject<Item> RAW_BEAST_MEAT = ModItems.ITEMS.register("raw_beast_meat", () ->
            new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(MaterialRules.RAW_MEAT_NUTRITION)
                    .saturationModifier(MaterialRules.RAW_MEAT_SATURATION).build())));
    public static final RegistryObject<Item> COOKED_BEAST_MEAT = ModItems.ITEMS.register("cooked_beast_meat", () ->
            new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(MaterialRules.COOKED_MEAT_NUTRITION)
                    .saturationModifier(MaterialRules.COOKED_MEAT_SATURATION).build())));
    public static final RegistryObject<Item> CHITIN_PLATE = material("chitin_plate");
    public static final RegistryObject<Item> VENOM_GLAND = material("venom_gland");
    public static final RegistryObject<Item> BONE_SHARD = material("bone_shard");
    public static final RegistryObject<Item> ROTTEN_SINEW = material("rotten_sinew");

    private static RegistryObject<Item> material(String id) {
        return ModItems.ITEMS.register(id, () -> new Item(new Item.Properties()));
    }
}
