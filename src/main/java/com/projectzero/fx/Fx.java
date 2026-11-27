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
    /**
     * Spawn with velocity = (target - start) / {@link #INFLOW_DIVISOR} and it lands on the target:
     * it lives {@link #INFLOW_TICKS} ticks and speeds up 4% a tick (sum of 1.04^k for 22 ticks).
     */
    public static ParticleOptions inflow(int rgb, float size) { return new ManaParticleOptions(ModParticles.INFLOW.get(), rgb, size); }
    public static final int INFLOW_TICKS = 22;
    public static final double INFLOW_DIVISOR = 34.25;
    public static ParticleOptions drop(int rgb, float size) { return new ManaParticleOptions(ModParticles.DROP.get(), rgb, size); }
    public static ParticleOptions streak(int rgb, float size) { return new ManaParticleOptions(ModParticles.STREAK.get(), rgb, size); }
}
