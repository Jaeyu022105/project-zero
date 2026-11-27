package com.projectzero.registry;

import com.projectzero.materials.MaterialRules;
import com.projectzero.materials.MaterialItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.RegistryObject;

/** Monster parts and rank-colored gate cores, registered in the shared item registry. */
public final class ModMaterials {
    private ModMaterials() {}

    public static void init() {}

    public static final RegistryObject<Item> BEAST_FANG = material("beast_fang", "tooltip.projectzero.source.beast");
    public static final RegistryObject<Item> BEAST_CLAW = material("beast_claw", "tooltip.projectzero.source.beast");
    public static final RegistryObject<Item> BEAST_PELT = material("beast_pelt", "tooltip.projectzero.source.beast");
    public static final RegistryObject<Item> RAW_BEAST_MEAT = ModItems.ITEMS.register("raw_beast_meat", () ->
            new MaterialItem(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(MaterialRules.RAW_MEAT_NUTRITION)
                    .saturationModifier(MaterialRules.RAW_MEAT_SATURATION).build()),
                    "tooltip.projectzero.source.beast"));
    public static final RegistryObject<Item> COOKED_BEAST_MEAT = ModItems.ITEMS.register("cooked_beast_meat", () ->
            new MaterialItem(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(MaterialRules.COOKED_MEAT_NUTRITION)
                    .saturationModifier(MaterialRules.COOKED_MEAT_SATURATION).build()),
                    "tooltip.projectzero.source.beast"));
    public static final RegistryObject<Item> CHITIN_PLATE = material("chitin_plate", "tooltip.projectzero.source.arachnid");
    public static final RegistryObject<Item> VENOM_GLAND = material("venom_gland", "tooltip.projectzero.source.arachnid");
    public static final RegistryObject<Item> BONE_SHARD = material("bone_shard", "tooltip.projectzero.source.undead");
    public static final RegistryObject<Item> ROTTEN_SINEW = material("rotten_sinew", "tooltip.projectzero.source.undead");

    public static final RegistryObject<Item> MONSTER_CRYSTAL_E = crystal("monster_crystal_e", Rarity.COMMON, false);
    public static final RegistryObject<Item> MONSTER_CRYSTAL_D = crystal("monster_crystal_d", Rarity.UNCOMMON, false);
    public static final RegistryObject<Item> MONSTER_CRYSTAL_C = crystal("monster_crystal_c", Rarity.UNCOMMON, false);
    public static final RegistryObject<Item> MONSTER_CRYSTAL_B = crystal("monster_crystal_b", Rarity.RARE, false);
    public static final RegistryObject<Item> MONSTER_CRYSTAL_A = crystal("monster_crystal_a", Rarity.EPIC, true);
    public static final RegistryObject<Item> MONSTER_CRYSTAL_S = crystal("monster_crystal_s", Rarity.EPIC, true);

    private static RegistryObject<Item> material(String id, String sourceKey) {
        return ModItems.ITEMS.register(id,
                () -> new MaterialItem(new Item.Properties(), sourceKey));
    }

    private static RegistryObject<Item> crystal(String id, Rarity rarity, boolean glint) {
        Item.Properties base = new Item.Properties().rarity(rarity);
        Item.Properties properties = glint
                ? base.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                : base;
        return ModItems.ITEMS.register(id, () -> new MaterialItem(properties,
                "tooltip.projectzero.source.crystal." + id.substring(id.lastIndexOf('_') + 1)));
    }
}
