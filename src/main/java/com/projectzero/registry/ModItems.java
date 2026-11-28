package com.projectzero.registry;

import com.projectzero.ProjectZero;
import com.projectzero.awakening.AwakeningShardItem;
import com.projectzero.awakening.ShardVesselItem;
import com.projectzero.equipment.AccessoryItem;
import com.projectzero.equipment.AccessorySlotType;
import com.projectzero.skills.SkillbookItem;
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

    /** Holds one Awakening Shard and diverts half of kill XP into charging it. Icon fills by tier. */
    public static final RegistryObject<Item> SHARD_VESSEL = ITEMS.register("shard_vessel",
            () -> new ShardVesselItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** Teaches the skill stored in {@code projectzero:skill_id} when used. */
    public static final RegistryObject<Item> SKILLBOOK = ITEMS.register("skillbook",
            () -> new SkillbookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> AWAKENING_CRYSTAL = ITEMS.register("awakening_crystal",
            () -> new BlockItem(ModBlocks.AWAKENING_CRYSTAL.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> AWAKENING_CORE = ITEMS.register("awakening_core",
            () -> new BlockItem(ModBlocks.AWAKENING_CORE.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> AWAKENING_CRYSTAL_DEEP = ITEMS.register("awakening_crystal_deep",
            () -> new BlockItem(ModBlocks.AWAKENING_CRYSTAL_DEEP.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> AWAKENING_CLUSTER = ITEMS.register("awakening_cluster",
            () -> new BlockItem(ModBlocks.AWAKENING_CLUSTER.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> GATE_PORTAL = ITEMS.register("gate_portal",
            () -> new BlockItem(ModBlocks.GATE_PORTAL.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> MANA_CRYSTAL_BLOCK = ITEMS.register("mana_crystal",
            () -> new BlockItem(ModBlocks.MANA_CRYSTAL.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** Mined from dungeon walls. TODO(gemini): uses (vessel fuel, crafting, trade). */
    public static final RegistryObject<Item> MANA_CRYSTAL_SHARD = ITEMS.register("mana_crystal_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** Resets a companion's spent stat points when used from its roster screen. TODO(claude-art). */
    public static final RegistryObject<Item> BOND_CRYSTAL = ITEMS.register("bond_crystal",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> WAYPOINT_STONE = ITEMS.register("waypoint_stone",
            () -> new com.projectzero.waypoints.WaypointStoneItem(ModBlocks.WAYPOINT_STONE.get(), new Item.Properties()));

    // ---- Starter Accessories (icons + worn gauntlet layer done; see client/GauntletLayer) ----
    public static final RegistryObject<Item> IRON_GAUNTLETS = ITEMS.register("iron_gauntlets",
            () -> new AccessoryItem(AccessorySlotType.GAUNTLETS, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> COPPER_RING = ITEMS.register("copper_ring",
            () -> new AccessoryItem(AccessorySlotType.RING, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SILVER_RING = ITEMS.register("silver_ring",
            () -> new AccessoryItem(AccessorySlotType.RING, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GOLD_RING = ITEMS.register("gold_ring",
            () -> new AccessoryItem(AccessorySlotType.RING, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BONE_AMULET = ITEMS.register("bone_amulet",
            () -> new AccessoryItem(AccessorySlotType.AMULET, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> OBSIDIAN_EARRINGS = ITEMS.register("obsidian_earrings",
            () -> new AccessoryItem(AccessorySlotType.EARRINGS, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LEATHER_BELT = ITEMS.register("leather_belt",
            () -> new AccessoryItem(AccessorySlotType.BELT, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HUNTER_CHARM = ITEMS.register("hunter_charm",
            () -> new AccessoryItem(AccessorySlotType.CHARM, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MANA_CHARM = ITEMS.register("mana_charm",
            () -> new AccessoryItem(AccessorySlotType.CHARM, new Item.Properties().stacksTo(1)));
}
