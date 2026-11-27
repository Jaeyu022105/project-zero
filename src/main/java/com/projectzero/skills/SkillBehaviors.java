package com.projectzero.skills;

import com.google.gson.JsonObject;
import com.projectzero.ProjectZero;
import net.minecraft.ChatFormatting;
import com.projectzero.client.ClientSkillVisuals;
import com.projectzero.fx.Fx;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SkillVisualPacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of Java {@link SkillBehavior} implementations referenced by JSON skill files.
 * Numbers come from each skill's JSON {@code params}; visuals use vanilla particles and sounds.
 * Each skill layers its own shapes on top (sigils, shockwaves, arcs, domes; see {@link SkillFx}).
 */
public final class SkillBehaviors {
    private SkillBehaviors() {}

    private static final Map<ResourceLocation, SkillBehavior> BEHAVIORS = new HashMap<>();

    static {
        register(ProjectZero.id("noop"), (player, skill, params) -> true);
        // Basic skills
        register(ProjectZero.id("dash"), SkillBehaviors::castDash);
        register(ProjectZero.id("second_wind"), SkillBehaviors::castSecondWind);
        register(ProjectZero.id("focus"), (player, skill, params) -> false); // passive, handled in SkillEffects
        register(ProjectZero.id("bloodlust"), SkillBehaviors::castBloodlust);
        // 10 Job signature skills
        register(ProjectZero.id("iron_knuckle"), SkillBehaviors::castIronKnuckle);       // Brawler
        register(ProjectZero.id("ricochet_bolt"), SkillBehaviors::castRicochetBolt);     // Slinger
        register(ProjectZero.id("riposte_stance"), SkillBehaviors::castRiposteStance);   // Duelist
        register(ProjectZero.id("aegis_bastion"), SkillBehaviors::castAegisBastion);     // Bulwark
        register(ProjectZero.id("cursed_edge"), SkillBehaviors::castCursedEdge);         // Hexblade
        register(ProjectZero.id("pack_howl"), SkillBehaviors::castPackHowl);             // Beastcaller
        register(ProjectZero.id("tempest_call"), SkillBehaviors::castTempestCall);       // Stormweaver
        register(ProjectZero.id("shadow_step"), SkillBehaviors::castShadowStep);         // Veilstalker
        register(ProjectZero.id("soul_shackle"), SkillBehaviors::castSoulShackle);       // Gravewarden
        register(ProjectZero.id("solar_cataclysm"), SkillBehaviors::castSolarCataclysm); // Sunbreaker
        // Milestone skills
        register(ProjectZero.id("colossus_breaker"), SkillBehaviors::castColossusBreaker);
    }

    public static void register(ResourceLocation id, SkillBehavior behavior) {
        BEHAVIORS.put(id, behavior);
    }

    public static @Nullable SkillBehavior get(ResourceLocation id) {
        return BEHAVIORS.get(id);
    }

    // ---- Basic skills ----

    private static boolean castDash(ServerPlayer player, Skill skill, JsonObject params) {
        double speed = GsonHelper.getAsDouble(params, "speed", 1.35);
        double lift = GsonHelper.getAsDouble(params, "lift", 0.2);
        int iframes = GsonHelper.getAsInt(params, "iframes_ticks", 6);

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        horizontal = horizontal.lengthSqr() < 1.0E-4
                ? Vec3.directionFromRotation(0, player.getYRot())
                : horizontal.normalize();

        player.setDeltaMovement(horizontal.x * speed, Math.max(player.getDeltaMovement().y, lift), horizontal.z * speed);
        player.hurtMarked = true;
        player.invulnerableTime = Math.max(player.invulnerableTime, iframes);

        ServerLevel level = player.serverLevel();
        level.sendParticles(Fx.streak(SkillFx.WIND, 1.4F), player.getX(), player.getY() + 0.3, player.getZ(),
                12, 0.25, 0.15, 0.25, 0.04);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.3F);
        SkillFx.shockwave(level, player.position().add(0, 0.1, 0), 0.3, 1.6, 4, SkillFx.dust(SkillFx.WIND, 0.8F));
        SkillFx.trail(level, player, 6, SkillFx.dust(SkillFx.WIND, 1.0F));
        return true;
    }

    private static boolean castSecondWind(ServerPlayer player, Skill skill, JsonObject params) {
        int duration = GsonHelper.getAsInt(params, "duration_ticks", 60);
        double fraction = GsonHelper.getAsDouble(params, "total_fraction", 0.30);
        SkillEffects.startSecondWind(player, duration, fraction);

        ServerLevel level = player.serverLevel();
        level.sendParticles(Fx.glow(SkillFx.LIFE, 1.2F), player.getX(), player.getY() + 1.2, player.getZ(),
                6, 0.35, 0.35, 0.35, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.5F);
        SkillFx.sigil(level, player.position(), 1.4, SkillFx.LIFE);
        SkillFx.spiral(level, player.position(), 2.2, 0.8, 3, duration, SkillFx.dust(SkillFx.LIFE, 0.9F));
        return true;
    }

    private static boolean castBloodlust(ServerPlayer player, Skill skill, JsonObject params) {
        int duration = GsonHelper.getAsInt(params, "duration_ticks", 200);
        double dmgBonus = GsonHelper.getAsDouble(params, "damage_bonus", 0.20);
        double spdBonus = GsonHelper.getAsDouble(params, "speed_bonus", 0.10);
        SkillEffects.startBloodlust(player, duration, dmgBonus, spdBonus);

        ServerLevel level = player.serverLevel();
        level.sendParticles(Fx.spark(SkillFx.BLOOD, 1.6F), player.getX(), player.getY() + 1.4, player.getZ(),
                8, 0.4, 0.4, 0.4, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.7F, 1.2F);
        SkillFx.sigil(level, player.position(), 1.6, SkillFx.BLOOD);
        SkillFx.column(level, player.position(), 2.4, 0.7, 40, SkillFx.dust(SkillFx.BLOOD, 1.3F));
        SkillFx.shockwave(level, player.position().add(0, 0.1, 0), 0.5, 3.5, 6, SkillFx.dust(0x801018, 1.2F));
        return true;
    }

    // ---- Job signature skills ----

    /** Brawler: close-range shockwave punch with heavy knockback. */
    private static boolean castIronKnuckle(ServerPlayer player, Skill skill, JsonObject params) {
        double range = GsonHelper.getAsDouble(params, "range", 4.0);
        float baseDamage = GsonHelper.getAsFloat(params, "base_damage", 8.0F);
        float atkScale = GsonHelper.getAsFloat(params, "attack_scale", 0.8F);
        double knockback = GsonHelper.getAsDouble(params, "knockback", 1.4);

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 impact = player.getEyePosition().add(look.scale(2.0));
        float damage = baseDamage + (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * atkScale;

        for (LivingEntity target : hostilesNear(player, impact, range)) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            target.knockback(knockback, -look.x, -look.z);
        }

        level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 2, 0.2, 0.2, 0.2, 0.0);
        level.sendParticles(Fx.shard(SkillFx.STEEL, 1.2F), impact.x, impact.y, impact.z, 18, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
        SkillFx.shockwave(level, impact, 0.3, range, 5, SkillFx.dust(SkillFx.EMBER, 1.4F));
        SkillFx.crescent(level, impact, look, 1.4, SkillFx.dust(0xFFE0B0, 1.0F));
        return true;
    }

    /** Slinger: high-speed bolt that ricochets to nearby foes. */
    private static boolean castRicochetBolt(ServerPlayer player, Skill skill, JsonObject params) {
        double range = GsonHelper.getAsDouble(params, "range", 24.0);
        float damage = GsonHelper.getAsFloat(params, "damage", 9.0F);
        int bounces = GsonHelper.getAsInt(params, "bounces", 2);
        double bounceRadius = GsonHelper.getAsDouble(params, "bounce_radius", 6.5);
        float bounceRetention = GsonHelper.getAsFloat(params, "bounce_retention", 0.75F);

        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        LivingEntity first = raycastTarget(player, eye, look, range);
        Vec3 end = first != null ? first.getEyePosition() : eye.add(look.scale(range));
        particleLine(level, eye, end);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.4F);

        if (first != null) {
            Set<LivingEntity> hit = new HashSet<>();
            LivingEntity current = first;
            float curDmg = damage;
            for (int i = 0; i <= bounces && current != null; i++) {
                hit.add(current);
                current.hurt(player.damageSources().playerAttack(player), curDmg);
                curDmg *= bounceRetention;

                LivingEntity prev = current;
                current = level.getEntitiesOfClass(LivingEntity.class,
                                AABB.ofSize(prev.position(), bounceRadius * 2, bounceRadius * 2, bounceRadius * 2),
                                e -> isValidTarget(player, e) && !hit.contains(e))
                        .stream()
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(prev)))
                        .orElse(null);
                if (current != null && i < bounces) {
                    particleLine(level, prev.getEyePosition(), current.getEyePosition());
                }
            }
        }
        return true;
    }

    /** Duelist: enters a short parry window that negates and counters the next incoming attack. */
    private static boolean castRiposteStance(ServerPlayer player, Skill skill, JsonObject params) {
        int window = GsonHelper.getAsInt(params, "window_ticks", 35);
        float counterBase = GsonHelper.getAsFloat(params, "counter_damage", 12.0F);
        float atkScale = GsonHelper.getAsFloat(params, "attack_scale", 1.0F);
        SkillEffects.startRiposte(player, window, counterBase, atkScale);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0, player.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.8F, 1.6F);
        {
            Vec3 f = Vec3.directionFromRotation(0, player.getYRot());
            Vec3 side = new Vec3(-f.z, 0, f.x);
            Vec3 c = player.position().add(0, 1.1, 0).add(f.scale(0.9));
            ParticleOptions blade = SkillFx.dust(SkillFx.STEEL, 0.8F);
            SkillFx.line(level, c.add(side.scale(-0.7)).add(0, 0.7, 0), c.add(side.scale(0.7)).add(0, -0.7, 0), 0.1, blade);
            SkillFx.line(level, c.add(side.scale(0.7)).add(0, 0.7, 0), c.add(side.scale(-0.7)).add(0, -0.7, 0), 0.1, blade);
            SkillFx.ring(level, player.position().add(0, 0.1, 0), 1.2, blade);
        }
        return true;
    }

    /** Bulwark: grants absorption + resistance and taunts nearby monsters. */
    private static boolean castAegisBastion(ServerPlayer player, Skill skill, JsonObject params) {
        float absorption = GsonHelper.getAsFloat(params, "absorption_amount", 12.0F);
        int duration = GsonHelper.getAsInt(params, "duration_ticks", 160);
        double tauntRadius = GsonHelper.getAsDouble(params, "taunt_radius", 8.0);

        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), absorption));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));

        ServerLevel level = player.serverLevel();
        for (Mob mob : level.getEntitiesOfClass(Mob.class,
                AABB.ofSize(player.position(), tauntRadius * 2, 8.0, tauntRadius * 2),
                m -> m instanceof Enemy && m.isAlive())) {
            mob.setTarget(player);
        }

        level.sendParticles(Fx.shard(SkillFx.AEGIS, 1.3F), player.getX(), player.getY() + 1.0, player.getZ(),
                24, 0.5, 0.6, 0.5, 0.2);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F);
        SkillFx.sigil(level, player.position(), 1.8, SkillFx.AEGIS);
        // The shield emblem flashes up in front, then a hex bubble stays around you while the buff lasts.
        ModNetwork.sendToTrackingAndSelf(player, new SkillVisualPacket(player.getId(),
                ClientSkillVisuals.Kind.SHIELD_POP.ordinal(), SkillFx.AEGIS, 16));
        ModNetwork.sendToTrackingAndSelf(player, new SkillVisualPacket(player.getId(),
                ClientSkillVisuals.Kind.BUBBLE.ordinal(), SkillFx.AEGIS, duration));
        ParticleOptions shell = SkillFx.dust(SkillFx.AEGIS, 1.1F);
        Vec3 at = player.position();
        SkillFx.dome(level, at, 1.8, 40, shell);
        SkillFx.shockwave(level, at.add(0, 0.1, 0), 1.0, tauntRadius, 8, SkillFx.dust(0xFF5050, 0.8F));
        return true;
    }

    /** Hexblade: dark forward wave applying Wither and draining health per foe hit. */
    private static boolean castCursedEdge(ServerPlayer player, Skill skill, JsonObject params) {
        double range = GsonHelper.getAsDouble(params, "range", 10.0);
        float damage = GsonHelper.getAsFloat(params, "damage", 10.0F);
        int witherTicks = GsonHelper.getAsInt(params, "wither_ticks", 80);
        float healPerHit = GsonHelper.getAsFloat(params, "heal_per_hit", 2.0F);

        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Set<LivingEntity> struck = new HashSet<>();

        for (double d = 1.0; d <= range; d += 1.0) {
            Vec3 p = start.add(look.scale(d));
            level.sendParticles(Fx.rune(SkillFx.HEX, 1.2F), p.x, p.y - 0.2, p.z, 4, 0.35, 0.2, 0.35, 0.02);
            level.sendParticles(Fx.glow(0x2A1040, 1.6F), p.x, p.y - 0.2, p.z, 2, 0.25, 0.1, 0.25, 0.01);
            for (LivingEntity target : hostilesNear(player, p, 1.8)) {
                if (struck.add(target)) {
                    target.hurt(player.damageSources().indirectMagic(player, player), damage);
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, witherTicks, 1));
                }
            }
        }
        if (!struck.isEmpty()) {
            player.heal(struck.size() * healPerHit);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.7F, 1.3F);
        ParticleOptions hex = SkillFx.dust(SkillFx.HEX, 1.3F);
        ParticleOptions dark = SkillFx.dust(0x2A1040, 1.5F);
        for (int i = 0; i < (int) range; i += 2) {
            Vec3 c = start.add(look.scale(1.0 + i)).add(0, -0.3, 0);
            double r = 1.2 + i * 0.08;
            SkillFx.later(1 + i / 2, () -> {
                SkillFx.crescent(level, c, look, r, hex);
                SkillFx.crescent(level, c.add(look.scale(-0.2)), look, r * 0.9, dark);
            });
        }
        return true;
    }

    /** Beastcaller: summons temporary tamed Spirit Hounds that hunt nearby enemies. */
    private static boolean castPackHowl(ServerPlayer player, Skill skill, JsonObject params) {
        int count = GsonHelper.getAsInt(params, "count", 2);
        int duration = GsonHelper.getAsInt(params, "duration_ticks", 240);

        ServerLevel level = player.serverLevel();
        long expireAt = level.getGameTime() + duration;
        LivingEntity nearestFoe = hostilesNear(player, player.position(), 12.0).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        for (int i = 0; i < count; i++) {
            Wolf hound = EntityType.WOLF.create(level);
            if (hound == null) continue;
            double angle = (Math.PI * 2 * i) / Math.max(1, count);
            hound.moveTo(player.getX() + Math.cos(angle) * 1.5, player.getY(),
                    player.getZ() + Math.sin(angle) * 1.5, player.getYRot(), 0);
            hound.tame(player);
            hound.setCustomName(Component.literal("Spirit Hound").withStyle(ChatFormatting.AQUA));
            hound.getPersistentData().putBoolean(SkillEffects.SUMMON_TAG, true);
            hound.getPersistentData().putLong(SkillEffects.SUMMON_EXPIRE_TAG, expireAt);
            hound.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, false));
            hound.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false));
            if (nearestFoe != null) hound.setTarget(nearestFoe);
            level.addFreshEntity(hound);
            level.sendParticles(Fx.glow(SkillFx.SPIRIT, 1.2F), hound.getX(), hound.getY() + 0.5, hound.getZ(),
                    14, 0.3, 0.3, 0.3, 0.03);
            SkillFx.sigil(level, hound.position(), 0.9, SkillFx.SPIRIT);
            SkillFx.column(level, hound.position(), 1.6, 0.4, 24, SkillFx.dust(SkillFx.SPIRIT, 0.9F));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 0.9F, 1.1F);
        return true;
    }

    /** Stormweaver: calls fire-safe lightning bolts onto nearby enemies. */
    private static boolean castTempestCall(ServerPlayer player, Skill skill, JsonObject params) {
        double radius = GsonHelper.getAsDouble(params, "radius", 9.0);
        int maxTargets = GsonHelper.getAsInt(params, "max_targets", 5);
        float damage = GsonHelper.getAsFloat(params, "damage", 14.0F);

        ServerLevel level = player.serverLevel();
        List<LivingEntity> targets = hostilesNear(player, player.position(), radius);
        if (targets.isEmpty()) {
            Vec3 ahead = player.position().add(player.getLookAngle().scale(4.0));
            spawnVisualBolt(level, ahead);
        } else {
            for (int i = 0; i < Math.min(maxTargets, targets.size()); i++) {
                LivingEntity t = targets.get(i);
                spawnVisualBolt(level, t.position());
                t.hurt(skillDamage(player, DamageTypes.LIGHTNING_BOLT), damage);
            }
        }
        level.sendParticles(Fx.spark(SkillFx.STORM, 1.5F), player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.8, 0.8, 0.8, 0.25);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.9F, 1.1F);
        SkillFx.sigil(level, player.position(), 2.2, SkillFx.STORM);
        SkillFx.spiral(level, player.position(), 3.0, 1.2, 2, 10, SkillFx.dust(SkillFx.STORM, 1.0F));
        return true;
    }

    private static void spawnVisualBolt(ServerLevel level, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    /** Veilstalker: blinks behind the aimed target (or forward) and empowers the next strike. */
    private static boolean castShadowStep(ServerPlayer player, Skill skill, JsonObject params) {
        double range = GsonHelper.getAsDouble(params, "range", 14.0);
        int invisTicks = GsonHelper.getAsInt(params, "invisibility_ticks", 40);
        float backstabBonus = GsonHelper.getAsFloat(params, "backstab_multiplier", 0.50F);

        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position();
        level.sendParticles(Fx.mote(SkillFx.SHADOW, 1.5F), player.getX(), player.getY() + 1.0, player.getZ(),
                24, 0.3, 0.6, 0.3, 0.08);

        LivingEntity target = raycastTarget(player, player.getEyePosition(), player.getLookAngle().normalize(), range);
        if (target != null) {
            Vec3 targetLook = Vec3.directionFromRotation(0, target.getYRot());
            Vec3 behind = target.position().subtract(targetLook.scale(1.4));
            player.teleportTo(level, behind.x, target.getY(), behind.z, target.getYRot(), player.getXRot());
        } else {
            Vec3 forward = player.position().add(player.getLookAngle().normalize().scale(Math.min(8.0, range)));
            player.teleportTo(level, forward.x, player.getY(), forward.z, player.getYRot(), player.getXRot());
        }

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invisTicks, 0, false, false));
        SkillEffects.armBackstab(player, 80, backstabBonus);

        level.sendParticles(Fx.glow(0x1A1028, 2F), player.getX(), player.getY() + 1.0, player.getZ(),
                20, 0.3, 0.5, 0.3, 0.03);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.2F);
        ParticleOptions shade = SkillFx.dust(SkillFx.SHADOW, 1.6F);
        SkillFx.column(level, origin, 1.9, 0.45, 40, shade);
        SkillFx.line(level, origin.add(0, 1.0, 0), player.position().add(0, 1.0, 0), 0.3, SkillFx.dust(0x1A1028, 1.2F));
        SkillFx.column(level, player.position(), 1.9, 0.45, 30, shade);
        return true;
    }

    /** Gravewarden: necrotic chains root enemies in an area and siphon health back. */
    private static boolean castSoulShackle(ServerPlayer player, Skill skill, JsonObject params) {
        double radius = GsonHelper.getAsDouble(params, "radius", 7.0);
        float damage = GsonHelper.getAsFloat(params, "damage", 15.0F);
        int rootTicks = GsonHelper.getAsInt(params, "root_ticks", 70);
        float siphonFraction = GsonHelper.getAsFloat(params, "siphon_fraction", 0.25F);

        ServerLevel level = player.serverLevel();
        float totalDealt = 0.0F;
        for (LivingEntity target : hostilesNear(player, player.position(), radius)) {
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rootTicks, 5));
            totalDealt += damage;
            level.sendParticles(Fx.rune(SkillFx.SOUL, 1F), target.getX(), target.getY() + 0.8, target.getZ(),
                    10, 0.3, 0.4, 0.3, 0.03);
            Vec3 t = target.position();
            ParticleOptions chain = SkillFx.dust(SkillFx.SOUL, 0.8F);
            for (int k = 0; k < 3; k++) {
                double a = k * Math.PI * 2 / 3;
                Vec3 anchor = t.add(Math.cos(a) * 1.4, 0, Math.sin(a) * 1.4);
                SkillFx.line(level, anchor, t.add(0, target.getBbHeight() * 0.6, 0), 0.15, chain);
            }
        }
        if (totalDealt > 0) {
            player.heal(totalDealt * siphonFraction);
        }

        level.sendParticles(Fx.glow(SkillFx.SOUL, 1.4F), player.getX(), player.getY() + 0.2, player.getZ(),
                32, radius * 0.4, 0.2, radius * 0.4, 0.04);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8F, 1.3F);
        SkillFx.sigil(level, player.position(), radius * 0.6, SkillFx.SOUL);
        SkillFx.ring(level, player.position().add(0, 0.1, 0), radius, SkillFx.dust(SkillFx.SOUL, 1.0F));
        return true;
    }

    /** Sunbreaker: radiant nova that burns nearby foes, purges debuffs, and grants Fire Resistance. */
    private static boolean castSolarCataclysm(ServerPlayer player, Skill skill, JsonObject params) {
        double radius = GsonHelper.getAsDouble(params, "radius", 8.0);
        float damage = GsonHelper.getAsFloat(params, "damage", 18.0F);
        int burnSeconds = GsonHelper.getAsInt(params, "burn_seconds", 6);
        int fireResTicks = GsonHelper.getAsInt(params, "fire_res_ticks", 200);

        ServerLevel level = player.serverLevel();
        for (LivingEntity target : hostilesNear(player, player.position(), radius)) {
            target.igniteForSeconds(burnSeconds);
            target.hurt(skillDamage(player, DamageTypes.IN_FIRE), damage);
        }

        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(inst);
            }
        }
        for (MobEffectInstance inst : harmful) {
            player.removeEffect(inst.getEffect());
        }
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, fireResTicks, 0, false, true));

        level.sendParticles(Fx.glow(SkillFx.EMBER, 1.5F), player.getX(), player.getY() + 1.0, player.getZ(),
                60, radius * 0.35, 0.6, radius * 0.35, 0.08);
        level.sendParticles(Fx.spark(SkillFx.SUN, 1.6F), player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.6, 0.8, 0.6, 0.15);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
        SkillFx.sigil(level, player.position(), 2.4, SkillFx.SUN);
        SkillFx.column(level, player.position(), 6.0, 0.6, 70, SkillFx.dust(0xFFF4C0, 1.4F));
        SkillFx.shockwave(level, player.position().add(0, 0.2, 0), 0.8, radius, 8, SkillFx.dust(SkillFx.SUN, 1.6F));
        SkillFx.later(3, () -> SkillFx.shockwave(level, player.position().add(0, 0.6, 0), 0.8, radius * 0.8, 6,
                SkillFx.dust(SkillFx.EMBER, 1.3F)));
        return true;
    }

    /** Giant Killer milestone skill: crushing strike dealing base + % target max HP (capped). */
    private static boolean castColossusBreaker(ServerPlayer player, Skill skill, JsonObject params) {
        double range = GsonHelper.getAsDouble(params, "range", 5.5);
        float baseDamage = GsonHelper.getAsFloat(params, "base_damage", 14.0F);
        float atkScale = GsonHelper.getAsFloat(params, "attack_scale", 1.1F);
        float maxHpFraction = GsonHelper.getAsFloat(params, "max_hp_fraction", 0.08F);
        float maxBonus = GsonHelper.getAsFloat(params, "max_bonus_damage", 40.0F);

        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 horiz = new Vec3(look.x, 0, look.z);
        if (horiz.lengthSqr() > 1.0E-4) horiz = horiz.normalize();
        LivingEntity primary = raycastTarget(player, eye, look, range);
        Vec3 center = primary != null ? primary.position() : player.position().add(horiz.scale(2.2));

        float atkPart = baseDamage + (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * atkScale;
        List<LivingEntity> targets = hostilesNear(player, center, 3.5);
        if (primary != null && !targets.contains(primary)) {
            targets = new ArrayList<>(targets);
            targets.add(primary);
        }

        for (LivingEntity target : targets) {
            float hpBonus = Math.min(maxBonus, target.getMaxHealth() * maxHpFraction);
            target.hurt(player.damageSources().playerAttack(player), atkPart + hpBonus);
            target.knockback(0.9, -look.x, -look.z);
        }

        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.8, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(Fx.shard(SkillFx.EMBER, 1.4F), center.x, center.y + 0.8, center.z, 24, 0.5, 0.5, 0.5, 0.25);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 0.75F);
        ParticleOptions crack = SkillFx.dust(SkillFx.EMBER, 1.2F);
        Vec3 ground = new Vec3(center.x, center.y + 0.1, center.z);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + level.random.nextDouble() * 0.3;
            double len = 2.5 + level.random.nextDouble() * 1.5;
            SkillFx.line(level, ground, ground.add(Math.cos(a) * len, 0, Math.sin(a) * len), 0.2, crack);
        }
        SkillFx.shockwave(level, ground, 0.5, 4.0, 5, SkillFx.dust(0xFFE0B0, 1.0F));
        return true;
    }

    // ---- targeting helpers ----

    private static boolean isValidTarget(ServerPlayer caster, LivingEntity entity) {
        if (entity == caster || !entity.isAlive()) return false;
        if (entity.getPersistentData().getBoolean(SkillEffects.SUMMON_TAG)) return false;
        if (entity instanceof ServerPlayer other) return caster.canHarmPlayer(other);
        return true;
    }

    private static List<LivingEntity> hostilesNear(ServerPlayer caster, Vec3 center, double radius) {
        return caster.serverLevel().getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(center, radius * 2, radius * 2, radius * 2),
                e -> isValidTarget(caster, e) && e.position().distanceToSqr(center) <= radius * radius);
    }

    private static @Nullable LivingEntity raycastTarget(ServerPlayer caster, Vec3 start, Vec3 dir, double range) {
        LivingEntity best = null;
        double bestDist = range + 1.0;
        for (LivingEntity e : caster.serverLevel().getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(start.add(dir.scale(range * 0.5)), range + 2, range + 2, range + 2),
                ent -> isValidTarget(caster, ent))) {
            AABB box = e.getBoundingBox().inflate(0.5);
            var hit = box.clip(start, start.add(dir.scale(range)));
            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = e;
                }
            }
        }
        return best;
    }

    private static void particleLine(ServerLevel level, Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        if (dist < 0.1) return;
        Vec3 step = to.subtract(from).normalize().scale(0.6);
        int steps = Math.min(50, (int) (dist / 0.6));
        Vec3 cur = from;
        for (int i = 0; i < steps; i++) {
            cur = cur.add(step);
            level.sendParticles(Fx.glow(SkillFx.GOLD, 0.6F), cur.x, cur.y, cur.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        // Glowing tracer and a spark burst where it lands.
        SkillFx.line(level, from, to, 0.25, SkillFx.dust(SkillFx.GOLD, 0.7F));
        level.sendParticles(Fx.spark(SkillFx.GOLD, 1.4F), to.x, to.y, to.z, 8, 0.15, 0.15, 0.15, 0.2);
    }

    /** A damage source of {@code type} credited to {@code player}, so skill kills give XP and count for milestones. */
    private static DamageSource skillDamage(ServerPlayer player, ResourceKey<DamageType> type) {
        return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type), player);
    }
}
