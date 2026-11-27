package com.projectzero.materials;

import com.projectzero.registry.ModMaterials;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;

/** All drop, food, and future sell-value tuning for monster materials. */
public final class MaterialRules {
    private MaterialRules() {}

    public static final float BEAST_FANG_CHANCE = 0.30F;
    public static final float BEAST_CLAW_CHANCE = 0.30F;
    public static final float BEAST_PELT_CHANCE = 0.35F;
    public static final float BEAST_MEAT_CHANCE = 0.40F;
    public static final float CHITIN_CHANCE = 0.35F;
    public static final float VENOM_GLAND_CHANCE = 0.20F;
    public static final float BONE_SHARD_CHANCE = 0.35F;
    public static final float ROTTEN_SINEW_CHANCE = 0.30F;
    public static final float LOOTING_CHANCE_PER_LEVEL = 0.025F;
    public static final float MAX_LOOTING_BONUS = 0.15F;
    public static final float MAX_PART_CHANCE = 0.70F;
    public static final int BASE_PART_ROLLS = 1;
    public static final int MINIMUM_DROP_COUNT = 1;
    public static final int MAXIMUM_DROP_COUNT = 1;
    public static final int RAW_MEAT_NUTRITION = 3;
    public static final float RAW_MEAT_SATURATION = 0.3F;
    public static final int COOKED_MEAT_NUTRITION = 9;
    public static final float COOKED_MEAT_SATURATION = 0.9F;

    public record PartDrop(RegistryObject<Item> item, float chance) {}

    private static final List<PartDrop> BEAST_DROPS = List.of(
            new PartDrop(ModMaterials.BEAST_FANG, BEAST_FANG_CHANCE),
            new PartDrop(ModMaterials.BEAST_CLAW, BEAST_CLAW_CHANCE),
            new PartDrop(ModMaterials.BEAST_PELT, BEAST_PELT_CHANCE),
            new PartDrop(ModMaterials.RAW_BEAST_MEAT, BEAST_MEAT_CHANCE));
    private static final List<PartDrop> ARACHNID_DROPS = List.of(
            new PartDrop(ModMaterials.CHITIN_PLATE, CHITIN_CHANCE),
            new PartDrop(ModMaterials.VENOM_GLAND, VENOM_GLAND_CHANCE));
    private static final List<PartDrop> SKELETON_DROPS =
            List.of(new PartDrop(ModMaterials.BONE_SHARD, BONE_SHARD_CHANCE));
    private static final List<PartDrop> ZOMBIE_DROPS =
            List.of(new PartDrop(ModMaterials.ROTTEN_SINEW, ROTTEN_SINEW_CHANCE));

    private static final Map<EntityType<?>, List<PartDrop>> PART_DROPS = Map.ofEntries(
            Map.entry(EntityType.WOLF, BEAST_DROPS), Map.entry(EntityType.FOX, BEAST_DROPS),
            Map.entry(EntityType.POLAR_BEAR, BEAST_DROPS), Map.entry(EntityType.HOGLIN, BEAST_DROPS),
            Map.entry(EntityType.RAVAGER, BEAST_DROPS), Map.entry(EntityType.SPIDER, ARACHNID_DROPS),
            Map.entry(EntityType.CAVE_SPIDER, ARACHNID_DROPS), Map.entry(EntityType.SKELETON, SKELETON_DROPS),
            Map.entry(EntityType.STRAY, SKELETON_DROPS), Map.entry(EntityType.WITHER_SKELETON, SKELETON_DROPS),
            Map.entry(EntityType.ZOMBIE, ZOMBIE_DROPS), Map.entry(EntityType.HUSK, ZOMBIE_DROPS),
            Map.entry(EntityType.DROWNED, ZOMBIE_DROPS));

    public static List<PartDrop> partDrops(EntityType<?> type) {
        return PART_DROPS.getOrDefault(type, List.of());
    }
}
