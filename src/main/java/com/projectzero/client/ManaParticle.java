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
    private enum Style { GLOW, SPARK, RUNE, SHARD, MOTE, STREAK }

    private final Style style;
    private final SpriteSet sprites;
    private final float baseSize;
    private final float spin;
    private final double originX, originZ;

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
        float size = Math.max(0.1F, opt.size());
        switch (style) {
            case GLOW -> { lifetime = 14 + random.nextInt(8); baseSize = 0.11F * size; yd += 0.01; }
            case SPARK -> { lifetime = 7 + random.nextInt(5); baseSize = 0.09F * size; }
            case RUNE -> { lifetime = 22 + random.nextInt(8); baseSize = 0.16F * size; yd += 0.012; }
            case SHARD -> { lifetime = 22 + random.nextInt(10); baseSize = 0.08F * size; gravity = 0.6F;
                            hasPhysics = true; xd += (random.nextDouble() - 0.5) * 0.12; yd += 0.12;
                            zd += (random.nextDouble() - 0.5) * 0.12; friction = 0.96F; }
            case MOTE -> { lifetime = 40 + random.nextInt(30); baseSize = 0.07F * size; friction = 0.98F; }
            default -> { lifetime = 6 + random.nextInt(3); baseSize = 0.13F * size; friction = 0.7F; }
        }
        this.quadSize = baseSize;
        if (style == Style.RUNE || style == Style.SHARD) setSprite(sprites.get(random));
        else setSpriteFromAge(sprites);
    }

    private static Style styleOf(ParticleType<?> type) {
        if (type == ModParticles.SPARK.get()) return Style.SPARK;
        if (type == ModParticles.RUNE.get()) return Style.RUNE;
        if (type == ModParticles.SHARD.get()) return Style.SHARD;
        if (type == ModParticles.MOTE.get()) return Style.MOTE;
        if (type == ModParticles.STREAK.get()) return Style.STREAK;
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
            default -> { quadSize = baseSize * (1F - life); alpha = 1F - life; }
        }
        if (style != Style.RUNE && style != Style.SHARD) setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
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
