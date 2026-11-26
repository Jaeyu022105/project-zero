package com.projectzero.domain;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A tiny glowing speck hanging in the Reawakening void. Three sizes. No collision. */
public class VoidMoteBlock extends Block {
    public static final MapCodec<VoidMoteBlock> CODEC = simpleCodec(VoidMoteBlock::new);
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 2);
    private static final VoxelShape[] SHAPES = {
            Block.box(7, 7, 7, 9, 9, 9),
            Block.box(6.5, 6.5, 6.5, 9.5, 9.5, 9.5),
            Block.box(6, 6, 6, 10, 10, 10)
    };

    public VoidMoteBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SIZE, 0));
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIZE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(SIZE)];
    }
}
