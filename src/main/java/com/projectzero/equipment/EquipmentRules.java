package com.projectzero.equipment;

import com.projectzero.gates.GateRank;
import com.projectzero.jobs.JobRarity;
import com.projectzero.leveling.Stat;
import com.projectzero.registry.ModDataComponents;
import com.projectzero.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

/**
 * Balance constants and stat rolling tables for accessories (DESIGN-DOCUMENT.md §12).
 */
public final class EquipmentRules {
    private EquipmentRules() {}

    // ---- Drop chances (gemini-12h-backlog Task 5) ----
    public static final double VANILLA_HOSTILE_DROP_CHANCE = 0.015; // 1.5% (Common/Uncommon, rank E)
    public static final double MODDED_MOB_DROP_CHANCE = 0.03;       // 3.0%
    public static final double GATE_MONSTER_DROP_CHANCE = 0.08;     // 8.0%
    public static final double BOSS_DROP_CHANCE = 1.00;             // 100.0%

    public static final double CRIT_DAMAGE_MULTIPLIER = 1.50;

    private static final AccessorySlotType[] DROPPABLE_SLOTS = {
            AccessorySlotType.GAUNTLETS,
            AccessorySlotType.RING,
            AccessorySlotType.AMULET,
            AccessorySlotType.EARRINGS,
            AccessorySlotType.BELT,
            AccessorySlotType.CHARM
    };

    public static AccessorySlotType randomDroppableSlot(RandomSource random) {
        return DROPPABLE_SLOTS[random.nextInt(DROPPABLE_SLOTS.length)];
    }

    public static JobRarity rollVanillaRarity(RandomSource random) {
        return random.nextDouble() < 0.72 ? JobRarity.COMMON : JobRarity.UNCOMMON;
    }

    public static JobRarity rollStandardRarity(RandomSource random, boolean boss) {
        int roll = random.nextInt(100);
        if (boss) {
            if (roll < 15) return JobRarity.UNCOMMON;
            if (roll < 55) return JobRarity.RARE;
            if (roll < 88) return JobRarity.EPIC;
            return JobRarity.LEGENDARY;
        }
        if (roll < 50) return JobRarity.COMMON;
        if (roll < 78) return JobRarity.UNCOMMON;
        if (roll < 93) return JobRarity.RARE;
        if (roll < 99) return JobRarity.EPIC;
        return JobRarity.LEGENDARY;
    }

    /** Rolls an {@link ItemStack} matching {@code slotType} with rolled {@link AccessoryData}. */
    public static ItemStack rollItem(RandomSource random, AccessorySlotType slotType, JobRarity rarity, GateRank rank) {
        Item item = pickItemForSlot(random, slotType, rarity);
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.ACCESSORY_DATA.get(), rollData(random, rarity, rank));
        return stack;
    }

    public static Item pickItemForSlot(RandomSource random, AccessorySlotType slotType, JobRarity rarity) {
        return switch (slotType) {
            case GAUNTLETS -> ModItems.IRON_GAUNTLETS.get();
            case RING -> switch (rarity) {
                case COMMON -> ModItems.COPPER_RING.get();
                case UNCOMMON, RARE -> ModItems.SILVER_RING.get();
                case EPIC, LEGENDARY -> ModItems.GOLD_RING.get();
            };
            case AMULET -> ModItems.BONE_AMULET.get();
            case EARRINGS -> ModItems.OBSIDIAN_EARRINGS.get();
            case BELT -> ModItems.LEATHER_BELT.get();
            case CHARM, RELIC -> random.nextBoolean() ? ModItems.HUNTER_CHARM.get() : ModItems.MANA_CHARM.get();
        };
    }

    public static AccessoryData rollData(RandomSource random, JobRarity rarity, GateRank rank) {
        int lines = switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 1 + random.nextInt(2);
            case RARE -> 2;
            case EPIC -> 2 + random.nextInt(2);
            case LEGENDARY -> 3 + random.nextInt(2);
        };

        int minBase = switch (rank) {
            case E -> 1;
            case D -> 2;
            case C -> 4;
            case B -> 7;
            case A -> 11;
            case S -> 16;
        };
        int maxBase = switch (rank) {
            case E -> 2;
            case D -> 4;
            case C -> 7;
            case B -> 11;
            case A -> 16;
            case S -> 24;
        };
        double rarityScale = switch (rarity) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.25;
            case RARE -> 1.55;
            case EPIC -> 1.90;
            case LEGENDARY -> 2.40;
        };

        List<Stat> pool = new ArrayList<>(List.of(Stat.values()));
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Stat tmp = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, tmp);
        }

        EnumMap<Stat, Integer> rolledStats = new EnumMap<>(Stat.class);
        for (int i = 0; i < Math.min(lines, pool.size()); i++) {
            int raw = minBase + random.nextInt(maxBase - minBase + 1);
            int val = Math.max(1, (int) Math.round(raw * rarityScale));
            rolledStats.put(pool.get(i), val);
        }

        double specialChance = switch (rarity) {
            case COMMON -> 0.0;
            case UNCOMMON -> 0.25;
            case RARE -> 0.55;
            case EPIC -> 0.85;
            case LEGENDARY -> 1.00;
        };

        Optional<AccessoryData.SpecialLine> special = Optional.empty();
        if (random.nextDouble() < specialChance) {
            AccessoryData.AffixType[] affixes = AccessoryData.AffixType.values();
            AccessoryData.AffixType chosen = affixes[random.nextInt(affixes.length)];
            double tierFactor = 1.0 + rank.ordinal() * 0.35 + rarity.ordinal() * 0.25;
            double value = switch (chosen) {
                case CRIT_CHANCE -> Math.round((0.02 * tierFactor) * 100.0) / 100.0;
                case MANA_REGEN -> Math.round((0.5 * tierFactor) * 10.0) / 10.0;
                case LIFESTEAL -> Math.round((0.015 * tierFactor) * 100.0) / 100.0;
                case XP_BONUS -> Math.round((0.025 * tierFactor) * 100.0) / 100.0;
                case DAMAGE_VS_UNDEAD -> Math.round((0.04 * tierFactor) * 100.0) / 100.0;
            };
            special = Optional.of(new AccessoryData.SpecialLine(chosen, value));
        }

        return new AccessoryData(rarity, rank, Collections.unmodifiableMap(rolledStats), special);
    }
}
