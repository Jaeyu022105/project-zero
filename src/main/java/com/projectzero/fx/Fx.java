package com.projectzero.fx;

import net.minecraft.core.particles.ParticleOptions;

/** Shorthand for building our particle options: {@code Fx.glow(0x7FE8FF, 1F)}. */
public final class Fx {
    private Fx() {}

    public static ParticleOptions glow(int rgb, float size) { return new ManaParticleOptions(ModParticles.GLOW.get(), rgb, size); }
    public static ParticleOptions spark(int rgb, float size) { return new ManaParticleOptions(ModParticles.SPARK.get(), rgb, size); }
    public static ParticleOptions rune(int rgb, float size) { return new ManaParticleOptions(ModParticles.RUNE.get(), rgb, size); }
    public static ParticleOptions shard(int rgb, float size) { return new ManaParticleOptions(ModParticles.SHARD.get(), rgb, size); }
    public static ParticleOptions mote(int rgb, float size) { return new ManaParticleOptions(ModParticles.MOTE.get(), rgb, size); }
    public static ParticleOptions streak(int rgb, float size) { return new ManaParticleOptions(ModParticles.STREAK.get(), rgb, size); }
}
