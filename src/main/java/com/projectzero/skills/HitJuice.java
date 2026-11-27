package com.projectzero.skills;

import com.projectzero.client.ClientSkillVisuals;
import com.projectzero.fx.Fx;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SkillVisualPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of the game-feel rules (.agents/skills/game-feel): what sells a skill hit.
 * Call {@link #impact} for each target hit, then {@link #casterFeel} once per cast if anything was hit.
 * The client draws the victim's white flash and shake (hitstop), camera shake and punch-in.
 */
public final class HitJuice {
    private HitJuice() {}

    /** Hit weight tiers. {@code stop} = victim flash/shake ticks (hitstop tier). */
    public enum Weight {
        LIGHT(2, 6), HEAVY(4, 10), FINISHER(6, 14);

        public final int stop;
        public final int sparks;

        Weight(int stop, int sparks) {
            this.stop = stop;
            this.sparks = sparks;
        }
    }

    /** Directional sparks away from the caster, victim flash + shake, impact sound on the hit tick. */
    public static void impact(ServerPlayer caster, LivingEntity target, int rgb, Weight w) {
        ServerLevel level = caster.serverLevel();
        RandomSource rnd = level.random;
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.55, 0);
        Vec3 dir = at.subtract(caster.getEyePosition());
        dir = dir.lengthSqr() < 1.0E-4 ? caster.getLookAngle() : dir.normalize();
        for (int i = 0; i < w.sparks; i++) {
            Vec3 v = dir.add((rnd.nextDouble() - 0.5) * 0.9, rnd.nextDouble() * 0.5, (rnd.nextDouble() - 0.5) * 0.9).normalize();
            // count 0: the offset is the particle's velocity, scaled by speed.
            level.sendParticles(Fx.spark(rgb, 1.1F + w.ordinal() * 0.2F), at.x, at.y, at.z, 0, v.x, v.y, v.z,
                    0.35 + rnd.nextDouble() * 0.25);
        }
        level.sendParticles(Fx.glow(0xFFFFFF, 1.4F + w.ordinal() * 0.4F), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        ModNetwork.sendToTrackingEntity(target, new SkillVisualPacket(target.getId(),
                ClientSkillVisuals.Kind.HIT_FLASH.ordinal(), rgb, w.stop));
        float pitch = switch (w) { case LIGHT -> 1.3F; case HEAVY -> 0.95F; case FINISHER -> 0.7F; };
        level.playSound(null, at.x, at.y, at.z,
                w == Weight.FINISHER ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.6F + w.ordinal() * 0.2F, pitch);
    }

    /**
     * A lingering mark on the body (EnemyReactions.MARK_*). Bosses get a minimal, shorter version so
     * their attack reads stay clean.
     */
    public static void mark(LivingEntity target, int style, int ticks) {
        boolean boss = com.projectzero.combat.BossAffinity.isBoss(target);
        int flags = style | (boss ? 0x80 : 0);
        ModNetwork.sendToTrackingEntity(target, new SkillVisualPacket(target.getId(),
                ClientSkillVisuals.Kind.MARK.ordinal(), flags << 24, boss ? ticks / 3 : ticks));
    }

    /** Once per cast, after a hit: camera shake for heavy hits, plus a punch-in for finishers. */
    public static void casterFeel(ServerPlayer caster, Weight w) {
        if (w == Weight.LIGHT) return;
        ModNetwork.sendToPlayer(caster, new SkillVisualPacket(caster.getId(),
                ClientSkillVisuals.Kind.SHAKE.ordinal(), 0, w == Weight.HEAVY ? 4 : 6));
        if (w == Weight.FINISHER) {
            ModNetwork.sendToPlayer(caster, new SkillVisualPacket(caster.getId(),
                    ClientSkillVisuals.Kind.PUNCH.ordinal(), 0, 6));
        }
    }

    /** A crescent slash arc swept around the caster, facing where they look. */
    public static void slash(ServerPlayer caster, int rgb, int ticks) {
        ModNetwork.sendToTrackingAndSelf(caster, new SkillVisualPacket(caster.getId(),
                ClientSkillVisuals.Kind.SLASH.ordinal(), rgb, ticks));
    }
}
