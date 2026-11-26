package com.projectzero.registry;

import com.mojang.serialization.Codec;
import com.projectzero.ProjectZero;
import com.projectzero.awakening.ShardCharge;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Custom item stack data components (MC 1.21+). */
public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ProjectZero.MOD_ID);

    /** Stored on {@code shard_vessel}: whether it holds a shard, its XP charge, and its tier. */
    public static final RegistryObject<DataComponentType<ShardCharge>> SHARD_CHARGE =
            DATA_COMPONENTS.register("shard_charge", () -> DataComponentType.<ShardCharge>builder()
                    .persistent(ShardCharge.CODEC)
                    .networkSynchronized(ShardCharge.STREAM_CODEC)
                    .build());

    /** Stored on charged {@code awakening_shard} stacks (1..5; absent for tier 0 so uncharged shards stack). */
    public static final RegistryObject<DataComponentType<Integer>> SHARD_CHARGE_TIER =
            DATA_COMPONENTS.register("shard_charge_tier", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
}
