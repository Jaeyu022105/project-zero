package com.projectzero.client;

import com.projectzero.fx.ManaParticleOptions;
import com.projectzero.fx.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;

/**
 * Client particle for every {@link ManaParticleOptions} family. Always full-bright, tinted by
 * the options color; each family moves and fades in its own way.
 */
public class ManaParticle extends TextureSheetParticle {
    private enum Style { GLOW, SPARK, RUNE, SHARD, MOTE, STREAK, INFLOW, DROP, LEAF, FIREFLY }

    private final Style style;
    private final SpriteSet sprites;
    private final float baseSize;
    private final float spin;
    private final double originX, originZ;
    /** Leaves: sway phase, and whether this one can be caught by a nearby gate. */
    private final float phase;
    private final boolean catchable;

    protected ManaParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                           ManaParticleOptions opt, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.style = styleOf(opt.type());
        this.originX = x;
        this.originZ = z;
        float r = ((opt.rgb() >> 16) & 0xFF) / 255F, g = ((opt.rgb() >> 8) & 0xFF) / 255F, b = (opt.rgb() & 0xFF) / 255F;
        float jitter = 0.9F + random.nextFloat() * 0.2F;
        setColor(Math.min(1F, r * jitter), Math.min(1F, g * jitter), Math.min(1F, b * jitter));
        this.hasPhysics = false;
        this.gravity = 0F;
        this.friction = 0.9F;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.spin = (random.nextFloat() - 0.5F) * 0.3F;
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.catchable = random.nextFloat() < 0.45F;
        float size = Math.max(0.1F, opt.size());
        switch (style) {
            case GLOW -> { lifetime = 14 + random.nextInt(8); baseSize = 0.11F * size; yd += 0.01; }
            case SPARK -> { lifetime = 7 + random.nextInt(5); baseSize = 0.09F * size; }
            case RUNE -> { lifetime = 22 + random.nextInt(8); baseSize = 0.16F * size; yd += 0.012; }
            case SHARD -> { lifetime = 22 + random.nextInt(10); baseSize = 0.08F * size; gravity = 0.6F;
                            hasPhysics = true; xd += (random.nextDouble() - 0.5) * 0.12; yd += 0.12;
                            zd += (random.nextDouble() - 0.5) * 0.12; friction = 0.96F; }
            case MOTE -> { lifetime = 40 + random.nextInt(30); baseSize = 0.07F * size; friction = 0.98F; }
            case INFLOW -> { lifetime = com.projectzero.fx.Fx.INFLOW_TICKS; baseSize = 0.08F * size; friction = 1F; }
            case DROP -> { lifetime = 30 + random.nextInt(25); baseSize = 0.06F * size * (0.7F + random.nextFloat() * 0.6F);
                           gravity = 1.0F; hasPhysics = true; friction = 0.98F; }
            case LEAF -> { lifetime = 140 + random.nextInt(80); baseSize = 0.09F * size * (0.8F + random.nextFloat() * 0.4F);
                           gravity = 0.012F; hasPhysics = true; friction = 0.96F; roll = random.nextFloat() * Mth.TWO_PI; }
            case FIREFLY -> { lifetime = 65 + random.nextInt(55); baseSize = 0.065F * size * (0.85F + random.nextFloat() * 0.35F);
                              friction = 0.96F; }
            default -> { lifetime = 6 + random.nextInt(3); baseSize = 0.13F * size; friction = 0.7F; }
        }
        this.quadSize = baseSize;
        if (style == Style.RUNE || style == Style.SHARD || style == Style.DROP || style == Style.LEAF) setSprite(sprites.get(random));
        else setSpriteFromAge(sprites);
    }

    private static Style styleOf(ParticleType<?> type) {
        if (type == ModParticles.SPARK.get()) return Style.SPARK;
        if (type == ModParticles.RUNE.get()) return Style.RUNE;
        if (type == ModParticles.SHARD.get()) return Style.SHARD;
        if (type == ModParticles.MOTE.get()) return Style.MOTE;
        if (type == ModParticles.STREAK.get()) return Style.STREAK;
        if (type == ModParticles.INFLOW.get()) return Style.INFLOW;
        if (type == ModParticles.DROP.get()) return Style.DROP;
        if (type == ModParticles.LEAF.get()) return Style.LEAF;
        if (type == ModParticles.FIREFLY.get()) return Style.FIREFLY;
        return Style.GLOW;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float life = age / (float) lifetime;
        switch (style) {
            case GLOW -> { quadSize = baseSize * (1F - life * 0.7F); alpha = 1F - life * life; }
            case SPARK -> { quadSize = baseSize * (0.6F + 0.6F * Mth.sin(life * Mth.PI)); alpha = 1F - life * 0.5F; }
            case RUNE -> { alpha = Mth.sin(life * Mth.PI); quadSize = baseSize * (0.85F + 0.15F * life); }
            case SHARD -> { oRoll = roll; roll += spin; alpha = life < 0.7F ? 1F : (1F - life) / 0.3F; }
            case MOTE -> {
                // Swirl around the spawn point and sink inward.
                double dx = x - originX, dz = z - originZ;
                xd += -dz * 0.01 - dx * 0.004;
                zd += dx * 0.01 - dz * 0.004;
                alpha = Mth.sin(life * Mth.PI);
            }
            case INFLOW -> {
                // Speeds up as it nears the core, fades in, shrinks to nothing at the end.
                xd *= 1.04; yd *= 1.04; zd *= 1.04;
                alpha = Math.min(1F, life * 4F);
                quadSize = baseSize * (1F - life * life);
            }
            case DROP -> {
                // Splats when it lands: stops, spreads a little, then fades.
                if (onGround) { xd = 0; zd = 0; quadSize = Math.min(baseSize * 1.6F, quadSize * 1.08F); }
                alpha = life < 0.75F ? 1F : (1F - life) / 0.25F;
            }
            case LEAF -> tickLeaf(life);
            case FIREFLY -> {
                xd += Mth.sin(age * 0.11F + phase) * 0.0018;
                yd += Mth.cos(age * 0.09F + phase * 1.3F) * 0.0012;
                zd += Mth.cos(age * 0.10F + phase * 0.7F) * 0.0018;
                float env = life < 0.15F ? life / 0.15F : (life > 0.82F ? (1F - life) / 0.18F : 1F);
                float blink = 0.15F + 0.85F * Math.max(0F, Mth.sin(age * 0.22F + phase));
                alpha = env * blink;
                quadSize = baseSize * (0.75F + 0.35F * blink);
            }
            default -> { quadSize = baseSize * (1F - life); alpha = 1F - life; }
        }
        if (style != Style.RUNE && style != Style.SHARD && style != Style.DROP && style != Style.LEAF) setSpriteFromAge(sprites);
    }

    /**
     * A falling leaf: drifts side to side and tumbles as it sinks, rests on the ground, then fades.
     * Near an open gate some leaves are caught: they curve toward the rift, speed up, spin faster
     * and shrink away into it.
     */
    private void tickLeaf(float life) {
        oRoll = roll;
        net.minecraft.world.phys.Vec3 rift = catchable ? GateRiftRenderer.nearestRift(x, y, z, PULL_RANGE) : null;
        if (rift != null && !onGround) {
            double dx = rift.x - x, dy = rift.y - y, dz = rift.z - z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < 0.5) { remove(); return; }
            double pull = 0.012 * (1.0 - d / PULL_RANGE) + 0.002;
            xd += dx / d * pull + -dz / d * pull * 0.6;   // a little swirl, not a straight line
            yd += dy / d * pull + gravity * 0.04;          // cancel most of the fall
            zd += dz / d * pull + dx / d * pull * 0.6;
            roll += spin * 3F;
            quadSize = baseSize * (float) Math.min(1.0, d / 2.0 + 0.2);
            if (age > lifetime - 20) age = lifetime - 20;  // don't fade out mid-flight
            alpha = 1F;
            return;
        }
        if (onGround) {
            xd = 0; zd = 0;
            if (age < lifetime - 40) age = lifetime - 40;  // rest a couple of seconds, then fade
        } else {
            float sway = Mth.sin(age * 0.12F + phase);
            xd += sway * 0.0025;
            zd += Mth.cos(age * 0.09F + phase) * 0.0018;
            roll += spin * (0.4F + Math.abs(sway));
        }
        alpha = life < 0.85F ? Math.min(1F, age / 6F) : (1F - life) / 0.15F;
    }

    private static final double PULL_RANGE = 9.0;

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return style == Style.DROP || style == Style.LEAF ? super.getLightColor(partialTick) : 0xF000F0;
    }

    public static final class Provider implements ParticleProvider<ManaParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ManaParticleOptions opt, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new ManaParticle(level, x, y, z, vx, vy, vz, opt, sprites);
        }
    }
}
