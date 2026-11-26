package com.projectzero.skills;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side execution behavior for a {@link Skill}, referenced by {@code behavior} id in JSON.
 */
@FunctionalInterface
public interface SkillBehavior {
    /**
     * Runs an active skill. Return {@code true} to consume mana and start the cooldown, or
     * {@code false} if preconditions failed (e.g., no valid target) so mana is not spent.
     */
    boolean execute(ServerPlayer player, Skill skill, JsonObject params);

    /** Optional periodic hook called once per second while a passive skill is learned. */
    default void onPassiveSecond(ServerPlayer player, Skill skill, JsonObject params) {}
}
