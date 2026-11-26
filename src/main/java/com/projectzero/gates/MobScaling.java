package com.projectzero.gates;

import com.projectzero.ProjectZero;
import com.projectzero.leveling.LevelingRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * Applies rank-based health/damage multipliers, persistent rank + level metadata, and
 * {@code [C · Lv 34] Zombie} name tags to gate/ranked monsters (DESIGN-DOCUMENT.md §9).
 */
public final class MobScaling {
    private MobScaling() {}

    public static final String RANK_KEY = ProjectZero.MOD_ID + ":mob_rank";
    public static final String LEVEL_KEY = ProjectZero.MOD_ID + ":mob_level";
    public static final String SCALED_KEY = ProjectZero.MOD_ID + ":mob_scaled";
    public static final String GATE_MONSTER_KEY = ProjectZero.MOD_ID + ":gate_monster";
    public static final String GATE_BOSS_KEY = ProjectZero.MOD_ID + ":gate_boss";

    public static final ResourceLocation HEALTH_MODIFIER_ID = ProjectZero.id("mob_rank_health");
    public static final ResourceLocation DAMAGE_MODIFIER_ID = ProjectZero.id("mob_rank_damage");

    /**
     * Scales {@code entity} to {@code rank} and {@code mobLevel}: permanent {@code ADD_MULTIPLIED_BASE}
     * modifiers on max health and attack damage ({@code rank.multiplier() - 1}), persistent NBT metadata,
     * full heal to new max health, and custom name {@code [Rank · Lv N] TypeName}.
     */
    public static void apply(LivingEntity entity, GateRank rank, int mobLevel) {
        int clampedLevel = Math.max(1, Math.min(LevelingRules.MAX_LEVEL, mobLevel));
        double multBonus = rank.multiplier() - 1.0;

        AttributeInstance hpInst = entity.getAttribute(Attributes.MAX_HEALTH);
        if (hpInst != null) {
            hpInst.removeModifier(HEALTH_MODIFIER_ID);
            if (multBonus != 0.0) {
                hpInst.addPermanentModifier(new AttributeModifier(
                        HEALTH_MODIFIER_ID, multBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }

        AttributeInstance atkInst = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkInst != null) {
            atkInst.removeModifier(DAMAGE_MODIFIER_ID);
            if (multBonus != 0.0) {
                atkInst.addPermanentModifier(new AttributeModifier(
                        DAMAGE_MODIFIER_ID, multBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }

        entity.setHealth(entity.getMaxHealth());

        entity.getPersistentData().putBoolean(SCALED_KEY, true);
        entity.getPersistentData().putString(RANK_KEY, rank.name());
        entity.getPersistentData().putInt(LEVEL_KEY, clampedLevel);

        Component baseName = entity.getType().getDescription();
        entity.setCustomName(Component.literal("[" + rank.name() + " · Lv " + clampedLevel + "] ")
                .withStyle(rank.chatColor())
                .append(baseName.copy().withStyle(ChatFormatting.WHITE)));
    }

    public static boolean isScaled(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(SCALED_KEY);
    }

    public static @Nullable GateRank getRank(LivingEntity entity) {
        if (!entity.getPersistentData().contains(RANK_KEY)) return null;
        return GateRank.byName(entity.getPersistentData().getString(RANK_KEY));
    }

    public static int effectiveLevel(LivingEntity entity) {
        if (entity.getPersistentData().contains(LEVEL_KEY)) {
            return Math.max(1, entity.getPersistentData().getInt(LEVEL_KEY));
        }
        if (entity.getType().is(Tags.EntityTypes.BOSSES)) {
            return 50;
        }
        return 1;
    }

    /** Returns the entity's max health prior to {@link #HEALTH_MODIFIER_ID}, so XP doesn't double-count rank. */
    public static double unscaledMaxHealth(LivingEntity entity) {
        GateRank rank = getRank(entity);
        if (rank != null && isScaled(entity) && rank.multiplier() > 0.0) {
            return entity.getMaxHealth() / rank.multiplier();
        }
        return entity.getMaxHealth();
    }
}
