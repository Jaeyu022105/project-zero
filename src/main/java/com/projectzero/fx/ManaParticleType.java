package com.projectzero.fx;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** A particle type carrying {@link ManaParticleOptions}. */
public final class ManaParticleType extends ParticleType<ManaParticleOptions> {
    private final MapCodec<ManaParticleOptions> codec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, ManaParticleOptions> streamCodec;

    public ManaParticleType() {
        super(false);
        this.codec = ManaParticleOptions.codec(this);
        this.streamCodec = ManaParticleOptions.streamCodec(this);
    }

    @Override
    public MapCodec<ManaParticleOptions> codec() {
        return codec;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ManaParticleOptions> streamCodec() {
        return streamCodec;
    }
}
