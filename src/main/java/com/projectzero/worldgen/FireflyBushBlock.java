package com.projectzero.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Softly glowing night-firefly bush (DESIGN-DOCUMENT section 15).
 * Cross-cutout plant with no collision, instant break, light level 6, and shears / Silk Touch self-drop.
 * TODO(claude-art): placeholder 16x16 cutout texture (textures/block/firefly_bush.png).
 */
public class FireflyBushBlock extends BushBlock {
    public static final MapCodec<FireflyBushBlock> CODEC = simpleCodec(FireflyBushBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public FireflyBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return canGrowOn(state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.isClientSide()) {
            com.projectzero.client.Fireflies.emitFromBush(level, pos, random);
        }
    }

    public static boolean canGrowOn(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.CLAY)
                || state.is(BlockTags.SAND);
    }
}
