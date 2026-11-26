package com.projectzero.domain;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The mirror floor of the Reawakening sea: dark, glassy and see-through, so the stars and
 * motes below show through like reflections. Faces between two sea blocks are hidden.
 */
public class VoidSeaBlock extends Block {
    public static final MapCodec<VoidSeaBlock> CODEC = simpleCodec(VoidSeaBlock::new);

    public VoidSeaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }
}
