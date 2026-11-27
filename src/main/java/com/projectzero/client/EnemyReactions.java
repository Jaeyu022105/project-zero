package com.projectzero.client;

import com.projectzero.ProjectZero;
import com.projectzero.config.ClientConfig;
import com.projectzero.fx.Fx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * How enemy bodies react, drawn on each client: blood (by creature type) sprayed away from the hit
 * on every hurt and a bigger burst on death, plus lingering skill marks (burn, storm, hex, soul) sent
 * by {@code HitJuice}. Gore can be turned off in the client config.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EnemyReactions {
    private EnemyReactions() {}

    /** What a creature bleeds. gore = hidden when the gore option is off. */
    public enum Blood {
        RED(0x6E0A0E, true), ROT(0x3E4A22, true), BONE(0xE6E0CC, false), GOO(0x6CC04A, false),
        EMBER(0xFF8A2A, false), STONE(0x9A9A96, false), SNOW(0xF2F6FF, false), VOID(0x7A3AD8, false),
        ICHOR(0xA8C832, false), LEAF(0x4E9A3A, false);

        final int rgb;
        final boolean gore;

        Blood(int rgb, boolean gore) {
            this.rgb = rgb;
            this.gore = gore;
        }
    }

    /** Mark styles; the id is the top byte of the MARK visual's rgb. */
    public static final int MARK_BURN = 1, MARK_STORM = 2, MARK_HEX = 3, MARK_SOUL = 4, MARK_MINIMAL = 0x80;

    private static final double RANGE_SQ = 48 * 48;

    public static Blood bloodOf(Entity e) {
        if (e instanceof AbstractSkeleton || e instanceof SkeletonHorse || e instanceof WitherBoss) return Blood.BONE;
        if (e instanceof MagmaCube || e instanceof Blaze) return Blood.EMBER;
        if (e instanceof Slime) return Blood.GOO;
        if (e instanceof IronGolem) return Blood.STONE;
        if (e instanceof SnowGolem) return Blood.SNOW;
        if (e instanceof EnderMan || e instanceof Endermite || e instanceof Shulker || e instanceof EnderDragon) return Blood.VOID;
        if (e instanceof Spider || e instanceof Bee || e instanceof Silverfish) return Blood.ICHOR;
        if (e instanceof Creeper) return Blood.LEAF;
        if (e.getType().is(EntityTypeTags.UNDEAD)) return Blood.ROT;
        return Blood.RED;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || mc.player == null) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        boolean gore = ClientConfig.GORE.get();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity e) || e.distanceToSqr(cam) > RANGE_SQ) continue;
            if (e == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            boolean hurtNow = e.hurtDuration > 0 && e.hurtTime == e.hurtDuration - 1;
            boolean diedNow = e.deathTime == 1;
            if (!hurtNow && !diedNow) continue;
            Blood blood = bloodOf(e);
            if (blood.gore && !gore) continue;
            spray(level, e, blood, diedNow);
        }
        for (ClientSkillVisuals.Visual v : ClientSkillVisuals.active()) {
            if (v.kind != ClientSkillVisuals.Kind.MARK) continue;
            Entity e = level.getEntity(v.entityId);
            if (e == null || !e.isAlive()) continue;
            mark(level, e, v);
        }
    }

    /** Droplets thrown along the knockback (away from the hit), bigger creatures bleed more. */
    private static void spray(ClientLevel level, LivingEntity e, Blood blood, boolean death) {
        RandomSource rnd = level.random;
        Vec3 push = e.getDeltaMovement().multiply(1, 0, 1);
        Vec3 dir = push.lengthSqr() > 1.0E-4 ? push.normalize() : Vec3.ZERO;
        float bulk = Math.min(2.5F, e.getBbWidth() * e.getBbHeight());
        int n = (int) ((death ? 22 : 7) * (0.6F + bulk * 0.4F));
        double cy = e.getY() + e.getBbHeight() * 0.6;
        ParticleOptions drop = blood == Blood.EMBER ? Fx.spark(blood.rgb, 1F) : Fx.drop(blood.rgb, 1F + bulk * 0.2F);
        for (int i = 0; i < n; i++) {
            double sx = dir.x * 0.18 + (rnd.nextDouble() - 0.5) * (death ? 0.35 : 0.18);
            double sz = dir.z * 0.18 + (rnd.nextDouble() - 0.5) * (death ? 0.35 : 0.18);
            double sy = 0.06 + rnd.nextDouble() * (death ? 0.22 : 0.12);
            level.addParticle(drop, e.getX() + (rnd.nextDouble() - 0.5) * e.getBbWidth() * 0.6, cy,
                    e.getZ() + (rnd.nextDouble() - 0.5) * e.getBbWidth() * 0.6, sx, sy, sz);
        }
    }

    /** One tick of a lingering skill mark on the body. Minimal marks (bosses) spawn a third as often. */
    private static void mark(ClientLevel level, Entity e, ClientSkillVisuals.Visual v) {
        int style = (v.rgb >>> 24) & 0x7F;
        boolean minimal = ((v.rgb >>> 24) & MARK_MINIMAL) != 0;
        if (minimal && v.age % 3 != 0) return;
        RandomSource rnd = level.random;
        double x = e.getX() + (rnd.nextDouble() - 0.5) * e.getBbWidth();
        double y = e.getY() + rnd.nextDouble() * e.getBbHeight();
        double z = e.getZ() + (rnd.nextDouble() - 0.5) * e.getBbWidth();
        switch (style) {
            case MARK_BURN -> {
                level.addParticle(Fx.glow(0xFF7A20, 0.7F), x, y, z, 0, 0.03, 0);
                if (rnd.nextInt(3) == 0) level.addParticle(Fx.glow(0x2A1A12, 1.1F), x, y + 0.2, z, 0, 0.04, 0);
            }
            case MARK_STORM -> {
                if (rnd.nextInt(2) == 0) level.addParticle(Fx.spark(0x9AD8FF, 0.8F), x, y, z,
                        (rnd.nextDouble() - 0.5) * 0.2, (rnd.nextDouble() - 0.5) * 0.2, (rnd.nextDouble() - 0.5) * 0.2);
            }
            case MARK_HEX -> {
                if (rnd.nextInt(2) == 0) level.addParticle(Fx.glow(0x2A1040, 1.2F), x, y, z, 0, 0.01, 0);
                if (rnd.nextInt(5) == 0) level.addParticle(Fx.rune(0xB060FF, 0.5F), x, y, z, 0, 0.02, 0);
            }
            case MARK_SOUL -> {
                if (rnd.nextInt(2) == 0) level.addParticle(Fx.glow(0x70FFC8, 0.6F), x, e.getY() + 0.1, z, 0, 0.05, 0);
            }
            default -> {}
        }
    }
}
