package com.projectzero.titles;

import com.google.gson.JsonObject;
import com.projectzero.leveling.Stat;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data-driven title definition loaded from {@code data/<namespace>/titles/*.json}.
 */
public record Title(
        ResourceLocation id,
        Condition condition,
        Map<Stat, Integer> statBonuses,
        List<AttributeBonus> attributeBonuses,
        Optional<ResourceLocation> behavior,
        JsonObject params,
        Optional<ResourceLocation> grantsSkill
) {
    public record AttributeBonus(ResourceLocation attribute, double amount, AttributeModifier.Operation operation) {}

    public record Condition(String type, Optional<ResourceLocation> target, int count) {
        public boolean isMet(MilestoneTracker tracker) {
            return switch (type) {
                case "total_kills" -> tracker.totalKills() >= count;
                case "boss_kills" -> tracker.bossKills() >= count;
                case "level" -> tracker.highestLevel() >= count;
                case "level_no_deaths" -> tracker.highestLevel() >= count && tracker.deaths() == 0;
                case "rerolls" -> tracker.rerolls() >= count;
                case "gates_cleared" -> tracker.gatesCleared() >= count;
                case "entity_kills" -> target.map(tracker::killsOf).orElse(0) >= count;
                case "tag_kills" -> target.map(id -> tracker.killsOfTag(TagKey.create(Registries.ENTITY_TYPE, id)))
                        .orElse(0) >= count;
                default -> false;
            };
        }
    }

    public static String translationKey(ResourceLocation id) {
        return Util.makeDescriptionId("title", id);
    }

    public static Component displayName(ResourceLocation id) {
        return Component.translatable(translationKey(id));
    }

    public static Component description(ResourceLocation id) {
        return Component.translatable(translationKey(id) + ".desc");
    }

    public Component displayName() { return displayName(id); }
    public Component description() { return description(id); }
}
