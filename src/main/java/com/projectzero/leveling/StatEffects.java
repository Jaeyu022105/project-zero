package com.projectzero.leveling;

import com.projectzero.ProjectZero;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

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

    public static void apply(ServerPlayer player, PlayerStats stats) {
        set(player, Attributes.ATTACK_DAMAGE, STRENGTH_DAMAGE,
                stats.bonus(Stat.STRENGTH) * LevelingRules.DAMAGE_PER_STRENGTH);
        set(player, Attributes.MAX_HEALTH, VITALITY_HEALTH,
                stats.bonus(Stat.VITALITY) * LevelingRules.HEALTH_PER_VITALITY);
        set(player, Attributes.MOVEMENT_SPEED, AGILITY_SPEED,
                stats.bonus(Stat.AGILITY) * LevelingRules.SPEED_PER_AGILITY);
        set(player, Attributes.ATTACK_SPEED, AGILITY_ATTACK_SPEED,
                stats.bonus(Stat.AGILITY) * LevelingRules.ATTACK_SPEED_PER_AGILITY);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** Sense: chance to fully evade an attack from another entity. */
    public static boolean rollEvade(PlayerStats stats, RandomSource random) {
        double chance = Math.min(LevelingRules.MAX_EVADE_CHANCE,
                stats.bonus(Stat.SENSE) * LevelingRules.EVADE_CHANCE_PER_SENSE);
        return chance > 0 && random.nextDouble() < chance;
    }

    /** Intelligence: max mana. Not used yet, reserved for skills. */
    public static int maxMana(PlayerStats stats) {
        return stats.get(Stat.INTELLIGENCE) * LevelingRules.MANA_PER_INTELLIGENCE;
    }
}
