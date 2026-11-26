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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block of an Awakening Stone formation (the big crystal icebergs in villages).
 * Right-clicking any part of the formation works like the small Awakening Stone.
 * The core variant ({@code glowing = true}) is the bright vein: more light, sparkles and hum.
 *
 * Overrides are public on purpose (see AwakeningStoneBlock).
 */
public class AwakeningCrystalBlock extends Block {
    public static final MapCodec<AwakeningCrystalBlock> CODEC = simpleCodec(p -> new AwakeningCrystalBlock(p, false));

    private final boolean glowing;

    public AwakeningCrystalBlock(Properties properties, boolean glowing) {
        super(properties);
        this.glowing = glowing;
    }

    @Override
    public MapCodec<AwakeningCrystalBlock> codec() {
        return CODEC;
    }

    /** Hide faces between two crystal blocks, like glass, so a formation reads as one solid gem. */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.AWAKENING_SHARD.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            AwakeningService.awaken(serverPlayer, stack, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            AwakeningService.touch(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!glowing || random.nextInt(4) != 0) return;
        // Sparkle off a random exposed face.
        Direction face = Direction.getRandom(random);
        BlockPos out = pos.relative(face);
        if (!level.getBlockState(out).isAir()) return;
        double x = pos.getX() + 0.5 + face.getStepX() * 0.55 + (random.nextDouble() - 0.5) * 0.6;
        double y = pos.getY() + 0.5 + face.getStepY() * 0.55 + (random.nextDouble() - 0.5) * 0.6;
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.55 + (random.nextDouble() - 0.5) * 0.6;
        level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.01 + random.nextDouble() * 0.02, 0.0);
        if (random.nextInt(40) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                    0.5F, 0.5F + random.nextFloat() * 0.3F, false);
        }
    }
}
