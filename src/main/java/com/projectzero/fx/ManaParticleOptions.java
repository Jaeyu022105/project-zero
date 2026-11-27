package com.projectzero.fx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Options for all of our own particles: which sprite family ({@link #type}), a tint and a size.
 * One options class, several particle types (glow, spark, rune, shard, mote).
 */
public record ManaParticleOptions(ParticleType<ManaParticleOptions> type, int rgb, float size) implements ParticleOptions {
    @Override
    public ParticleType<ManaParticleOptions> getType() {
        return type;
    }

    static MapCodec<ManaParticleOptions> codec(ParticleType<ManaParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("color").forGetter(ManaParticleOptions::rgb),
                Codec.FLOAT.fieldOf("size").forGetter(ManaParticleOptions::size)
        ).apply(i, (c, s) -> new ManaParticleOptions(type, c, s)));
    }

    static StreamCodec<ByteBuf, ManaParticleOptions> streamCodec(ParticleType<ManaParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, ManaParticleOptions::rgb,
                ByteBufCodecs.FLOAT, ManaParticleOptions::size,
                (c, s) -> new ManaParticleOptions(type, c, s));
    }
}
