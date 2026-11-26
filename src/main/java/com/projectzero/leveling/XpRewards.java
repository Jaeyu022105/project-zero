package com.projectzero.leveling;

import com.projectzero.ProjectZero;
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
        if (killed instanceof Player) return 0;
        if (killed.getPersistentData().getBoolean(com.projectzero.skills.SkillEffects.SUMMON_TAG)) return 0;

        Long fixed = FIXED.get(killed.getType());
        if (fixed != null) return fixed;

        double xp = killed.getMaxHealth();
        if (killed instanceof Enemy) xp *= 2.0;

        ResourceLocation typeId = EntityType.getKey(killed.getType());
        if (killed.getType().is(GATE_MONSTERS)) {
            xp *= LevelingRules.GATE_MONSTER_XP_MULTIPLIER;
        } else if ("minecraft".equals(typeId.getNamespace())) {
            xp *= LevelingRules.VANILLA_XP_MULTIPLIER;
        }
        if (killed.getType().is(Tags.EntityTypes.BOSSES)) xp *= LevelingRules.BOSS_XP_MULTIPLIER;

        return Math.max(1, Math.round(xp));
    }
}
