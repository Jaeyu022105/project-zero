package com.projectzero.awakening;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.AmethystClusterBlock;

/**
 * Spiky crystal clusters that grow on the surface of Awakening Stone formations, so they read
 * as real crystal rather than a pile of cubes. Faces any direction, like amethyst clusters.
 */
public class AwakeningClusterBlock extends AmethystClusterBlock {
    public static final MapCodec<AwakeningClusterBlock> CODEC = simpleCodec(AwakeningClusterBlock::new);

    public AwakeningClusterBlock(Properties properties) {
        super(7.0F, 3.0F, properties);
    }

    // The parent declares MapCodec<AmethystClusterBlock>, so the override must return that type.
    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<AmethystClusterBlock> codec() {
        return (MapCodec<AmethystClusterBlock>) (MapCodec<?>) CODEC;
    }
}
