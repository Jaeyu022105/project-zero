package com.projectzero.anim;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Player body animations for skills. The ordinal goes over the network (SkillVisualPacket kind ANIM,
 * rgb = ordinal, ticks = duration). Poses live client-side in {@code client.anim.PlayerPoses}.
 * Timing follows the game-feel skill: short wind-up, snappy strike, a held end pose, then return.
 */
public enum PlayerAnim {
    SLASH(12), SLAM(14), PUNCH(10), THROW(10), GUARD(16), CAST_UP(16), NOVA(18), PUSH(14), ROAR(16), DASH(10),
    BOND_CRYSTAL(18), INTERACT(10);

    public final int ticks;

    PlayerAnim(int ticks) {
        this.ticks = ticks;
    }

    public static PlayerAnim byId(int id) {
        PlayerAnim[] all = values();
        return id >= 0 && id < all.length ? all[id] : SLASH;
    }

    /** Which body animation a skill behavior plays, or null for none. */
    public static @Nullable PlayerAnim forBehavior(ResourceLocation behavior) {
        return switch (behavior.getPath()) {
            case "cursed_edge" -> SLASH;
            case "colossus_breaker" -> SLAM;
            case "iron_knuckle" -> PUNCH;
            case "ricochet_bolt" -> THROW;
            case "riposte_stance", "aegis_bastion" -> GUARD;
            case "tempest_call", "pack_howl" -> CAST_UP;
            case "solar_cataclysm" -> NOVA;
            case "soul_shackle" -> PUSH;
            case "second_wind", "bloodlust" -> ROAR;
            case "dash", "shadow_step" -> DASH;
            default -> null;
        };
    }
}
