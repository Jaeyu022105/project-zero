package com.projectzero.gates;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mana crystals growing on dungeon walls (DESIGN-DOCUMENT.md §9). Mine them for Mana Crystal
 * items and a little XP. Faces any direction, like amethyst clusters.
 */
public class ManaCrystalBlock extends AmethystClusterBlock {
    public static final MapCodec<ManaCrystalBlock> CODEC = simpleCodec(ManaCrystalBlock::new);

    public ManaCrystalBlock(Properties properties) {
        super(7.0F, 3.0F, properties);
    }

    // The parent declares MapCodec<AmethystClusterBlock>, so the override must return that type.
    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<AmethystClusterBlock> codec() {
        return (MapCodec<AmethystClusterBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (dropExperience) tryDropExperience(level, pos, tool, UniformInt.of(1, 4));
    }
}
