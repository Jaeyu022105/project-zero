package com.projectzero.skills;

import com.google.gson.JsonObject;
import com.projectzero.ProjectZero;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of Java {@link SkillBehavior} implementations referenced by JSON skill files.
 */
public final class SkillBehaviors {
    private SkillBehaviors() {}

    private static final Map<ResourceLocation, SkillBehavior> BEHAVIORS = new HashMap<>();

    static {
        register(ProjectZero.id("noop"), (player, skill, params) -> true);
        register(ProjectZero.id("dash"), SkillBehaviors::castDash);
    }

    public static void register(ResourceLocation id, SkillBehavior behavior) {
        BEHAVIORS.put(id, behavior);
    }

    public static @Nullable SkillBehavior get(ResourceLocation id) {
        return BEHAVIORS.get(id);
    }

    /** Short forward burst with temporary invulnerability frames (i-frames). */
    private static boolean castDash(ServerPlayer player, Skill skill, JsonObject params) {
        double speed = GsonHelper.getAsDouble(params, "speed", 1.35);
        double lift = GsonHelper.getAsDouble(params, "lift", 0.2);
        int iframes = GsonHelper.getAsInt(params, "iframes_ticks", 6);

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0E-4) {
            horizontal = Vec3.directionFromRotation(0, player.getYRot());
        } else {
            horizontal = horizontal.normalize();
        }

        player.setDeltaMovement(horizontal.x * speed, Math.max(player.getDeltaMovement().y, lift), horizontal.z * speed);
        player.hurtMarked = true;
        player.invulnerableTime = Math.max(player.invulnerableTime, iframes);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.3, player.getZ(),
                12, 0.25, 0.15, 0.25, 0.04);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.3F);
        return true;
    }
}
