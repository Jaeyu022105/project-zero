package com.projectzero.leveling;

import com.projectzero.ProjectZero;
import com.projectzero.gates.GateRank;
import com.projectzero.gates.MobScaling;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.Tags;

import java.util.HashMap;
import java.util.Map;

/**
 * How much XP a kill is worth. Every mob except players gives XP, but gate and dungeon
 * monsters are the real way to level (design decision 2026-11-26):
 *   vanilla mobs  x0.3, other mods' mobs x1, gate monsters x3, bosses x10.
 * Custom mobs and bosses can also register a fixed value with {@link #register}.
 */
public final class XpRewards {
    private XpRewards() {}

    /** Entity type tag for monsters that spawn from gates and dungeons. */
    public static final TagKey<EntityType<?>> GATE_MONSTERS =
            TagKey.create(Registries.ENTITY_TYPE, ProjectZero.id("gate_monsters"));

    private static final Map<EntityType<?>, Long> FIXED = new HashMap<>();

    public static void register(EntityType<?> type, long xp) {
        FIXED.put(type, xp);
    }

    public static long forKill(LivingEntity killed) {
        return forKill(killed, 1);
    }

    public static long forKill(LivingEntity killed, int killerLevel) {
        if (killed instanceof Player) return 0;
        if (killed.getPersistentData().getBoolean(com.projectzero.skills.SkillEffects.SUMMON_TAG)) return 0;

        Long fixed = FIXED.get(killed.getType());
        double xp;
        if (fixed != null) {
            xp = fixed;
        } else {
            xp = MobScaling.unscaledMaxHealth(killed);
            if (killed instanceof Enemy) xp *= 2.0;

            ResourceLocation typeId = EntityType.getKey(killed.getType());
            boolean isGateMonster = killed.getType().is(GATE_MONSTERS)
                    || killed.getPersistentData().getBoolean(MobScaling.GATE_MONSTER_KEY);
            if (isGateMonster) {
                xp *= LevelingRules.GATE_MONSTER_XP_MULTIPLIER;
            } else if ("minecraft".equals(typeId.getNamespace())) {
                xp *= LevelingRules.VANILLA_XP_MULTIPLIER;
            }

            boolean isBoss = killed.getType().is(Tags.EntityTypes.BOSSES)
                    || killed.getPersistentData().getBoolean(MobScaling.GATE_BOSS_KEY);
            if (isBoss) xp *= LevelingRules.BOSS_XP_MULTIPLIER;

            GateRank rank = MobScaling.getRank(killed);
            if (rank != null) {
                xp *= rank.multiplier();
            }
        }

        int mobLevel = MobScaling.effectiveLevel(killed);
        xp *= LevelingRules.levelGapXpMultiplier(killerLevel, mobLevel);

        return Math.max(1, Math.round(xp));
    }
}
