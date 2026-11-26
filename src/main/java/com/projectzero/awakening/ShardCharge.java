package com.projectzero.awakening;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.projectzero.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Data component stored on a {@link ShardVesselItem}: whether it holds an Awakening Shard,
 * the cumulative XP diverted into it, and the highest charge tier reached (0..5).
 */
public record ShardCharge(boolean hasShard, long chargeXp, int tier) {
    public static final ShardCharge EMPTY = new ShardCharge(false, 0L, 0);

    public static final Codec<ShardCharge> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("has_shard").forGetter(ShardCharge::hasShard),
            Codec.LONG.fieldOf("charge_xp").forGetter(ShardCharge::chargeXp),
            Codec.INT.optionalFieldOf("tier", 0).forGetter(ShardCharge::tier)
    ).apply(inst, ShardCharge::new));

    public static final StreamCodec<ByteBuf, ShardCharge> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ShardCharge::hasShard,
            ByteBufCodecs.VAR_LONG, ShardCharge::chargeXp,
            ByteBufCodecs.VAR_INT, ShardCharge::tier,
            ShardCharge::new
    );

    public static ShardCharge get(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SHARD_CHARGE.get(), EMPTY);
    }
}
