package com.projectzero.leveling;

import com.projectzero.ProjectZero;
import com.projectzero.equipment.EquipmentService;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SyncStatBonusesPacket;
import com.projectzero.titles.TitleService;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;

/**
 * Turns stat values into gameplay. Uses transient attribute modifiers, so they are
 * re-applied on login and respawn and never pile up in the save file.
 */
public final class StatEffects {
    private StatEffects() {}

    private static final ResourceLocation STRENGTH_DAMAGE = ProjectZero.id("strength_damage");
    private static final ResourceLocation VITALITY_HEALTH = ProjectZero.id("vitality_health");
    private static final ResourceLocation AGILITY_SPEED = ProjectZero.id("agility_speed");
    private static final ResourceLocation AGILITY_ATTACK_SPEED = ProjectZero.id("agility_attack_speed");

    /** Extra stat points from equipped Title and Accessories. */
    public static int bonusStatPoints(Player player, Stat stat) {
        return TitleService.statBonus(player, stat) + EquipmentService.statBonus(player, stat);
    }

    public static int effectiveBonus(Player player, PlayerStats stats, Stat stat) {
        return Math.max(0, stats.bonus(stat) + bonusStatPoints(player, stat));
    }

    public static void apply(ServerPlayer player, PlayerStats stats) {
        set(player, Attributes.ATTACK_DAMAGE, STRENGTH_DAMAGE,
                LevelingRules.strengthDamageMultiplier(effectiveBonus(player, stats, Stat.STRENGTH)),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(player, Attributes.MAX_HEALTH, VITALITY_HEALTH,
                LevelingRules.vitalityHealthBonus(effectiveBonus(player, stats, Stat.VITALITY)),
                AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.MOVEMENT_SPEED, AGILITY_SPEED,
                LevelingRules.agilitySpeedMultiplier(effectiveBonus(player, stats, Stat.AGILITY)),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.ATTACK_SPEED, AGILITY_ATTACK_SPEED,
                LevelingRules.agilityAttackSpeedBonus(effectiveBonus(player, stats, Stat.AGILITY)),
                AttributeModifier.Operation.ADD_VALUE);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        EnumMap<Stat, Integer> bonuses = new EnumMap<>(Stat.class);
        for (Stat st : Stat.values()) {
            int b = bonusStatPoints(player, st);
            if (b != 0) bonuses.put(st, b);
        }
        ModNetwork.sendToPlayer(player, new SyncStatBonusesPacket(bonuses));
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
                            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /** Sense: chance to fully evade an attack from another entity (soft-capped below 30%). */
    public static boolean rollEvade(Player player, PlayerStats stats, RandomSource random) {
        double chance = LevelingRules.senseEvadeChance(effectiveBonus(player, stats, Stat.SENSE));
        return chance > 0 && random.nextDouble() < chance;
    }

    public static boolean rollEvade(PlayerStats stats, RandomSource random) {
        double chance = LevelingRules.senseEvadeChance(stats.bonus(Stat.SENSE));
        return chance > 0 && random.nextDouble() < chance;
    }

    /** Intelligence: max mana (includes title/equipment bonus Intelligence). */
    public static int maxMana(Player player, PlayerStats stats) {
        int totalInt = Math.max(1, stats.get(Stat.INTELLIGENCE) + bonusStatPoints(player, Stat.INTELLIGENCE));
        return totalInt * LevelingRules.MANA_PER_INTELLIGENCE;
    }

    public static int maxMana(PlayerStats stats) {
        return stats.get(Stat.INTELLIGENCE) * LevelingRules.MANA_PER_INTELLIGENCE;
    }
}
