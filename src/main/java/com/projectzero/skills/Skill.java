package com.projectzero.skills;

import com.google.gson.JsonObject;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * One skill definition loaded from {@code data/<namespace>/skills/<name>.json}.
 */
public record Skill(
        ResourceLocation id,
        SkillType type,
        SkillSource source,
        Optional<ResourceLocation> job,
        int manaCost,
        int cooldownTicks,
        int unlockLevel,
        ResourceLocation behavior,
        JsonObject params
) {
    public static String translationKey(ResourceLocation id) {
        return Util.makeDescriptionId("skill", id);
    }

    public static Component displayName(ResourceLocation id) {
        return Component.translatable(translationKey(id));
    }

    public static Component description(ResourceLocation id) {
        return Component.translatable(translationKey(id) + ".desc");
    }

    public Component displayName() {
        return displayName(id);
    }

    public Component description() {
        return description(id);
    }
}
