package com.projectzero.gates;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import com.projectzero.fx.Fx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Rank-colored Gate portal block (and Dungeon Exit portal when {@code EXIT=true}).
 * Invisible blocks; the rift (ring + swirl) is drawn by client/GateRiftRenderer.
 */
public class GateBlock extends Block implements EntityBlock {
    public static final MapCodec<GateBlock> CODEC = simpleCodec(GateBlock::new);
    public static final EnumProperty<GateRank> RANK = EnumProperty.create("rank", GateRank.class);
    public static final BooleanProperty EXIT = BooleanProperty.create("exit");

    public GateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RANK, GateRank.E).setValue(EXIT, false));
    }

    @Override
    public MapCodec<GateBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RANK, EXIT);
    }

    /** Drawn by GateRiftRenderer as a swirling ring; the blocks themselves are invisible. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GateBlockEntity(pos, state);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer serverPlayer) {
            GateService.onEnterPortal(serverPlayer, pos, state);
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            GateService.onEnterPortal(serverPlayer, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            int rgb = state.getValue(EXIT) ? 0x9FF4FF : state.getValue(RANK).argb() & 0xFFFFFF;
            level.addParticle(random.nextInt(4) == 0 ? Fx.spark(rgb, 1.2F) : Fx.mote(rgb, 1.4F),
                    x, y, z, (random.nextDouble() - 0.5) * 0.2, 0.03, (random.nextDouble() - 0.5) * 0.2);
        }
        if (random.nextInt(90) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.35F, 0.75F + random.nextFloat() * 0.3F, false);
        }
    }
}
