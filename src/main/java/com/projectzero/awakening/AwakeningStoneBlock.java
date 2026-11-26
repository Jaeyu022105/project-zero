package com.projectzero.awakening;

import com.mojang.serialization.MapCodec;
import com.projectzero.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A huge spiky blue crystal found in some villages. Two blocks tall: the lower half draws the
 * whole crystal (the model reaches up to y=32), the upper half only gives it a hitbox.
 *
 * Right-click while holding an Awakening Shard to reroll your job ({@link AwakeningService}).
 *
 * Overrides are declared public on purpose: Mojang made many of these protected in 1.20.5+,
 * and public compiles whether the parent method is public or protected.
 */
public class AwakeningStoneBlock extends Block {
    public static final MapCodec<AwakeningStoneBlock> CODEC = simpleCodec(AwakeningStoneBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public AwakeningStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public MapCodec<AwakeningStoneBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    // ---- two-block placement (same idea as tall flowers and doors) ----

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(ctx)) {
            return defaultBlockState();
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            // The other half changed: if it is gone, this half goes too.
            return neighbor.is(this) && neighbor.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
        }
        if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // ---- interaction ----

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.AWAKENING_SHARD.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            AwakeningService.awaken(serverPlayer, stack, lowerPos(state, pos));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            AwakeningService.touch(serverPlayer, lowerPos(state, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    // ---- ambience (client only) ----

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER) return;

        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;

        // Slow motes drifting up around the crystal.
        if (random.nextInt(3) == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.4 + random.nextDouble() * 0.5;
            level.addParticle(ParticleTypes.END_ROD,
                    cx + Math.cos(angle) * radius, pos.getY() + 0.2 + random.nextDouble() * 1.6, cz + Math.sin(angle) * radius,
                    0.0, 0.015 + random.nextDouble() * 0.02, 0.0);
        }
        // Rare spark at a tip.
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    cx + (random.nextDouble() - 0.5) * 0.5, pos.getY() + 1.7 + random.nextDouble() * 0.3,
                    cz + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.0, 0.0);
        }
        // Low crystal hum now and then.
        if (random.nextInt(60) == 0) {
            level.playLocalSound(cx, pos.getY() + 1.0, cz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                    0.6F, 0.5F + random.nextFloat() * 0.3F, false);
        }
    }
}
