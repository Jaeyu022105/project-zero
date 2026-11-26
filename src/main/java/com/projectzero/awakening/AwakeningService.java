package com.projectzero.awakening;

import com.projectzero.jobs.Job;
import com.projectzero.jobs.JobService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * What happens when a player uses an Awakening Stone (DESIGN-DOCUMENT.md §6).
 *
 * The presentation (messages, sound, particles) is done. The reroll itself is still the
 * test version from {@link JobService#reroll}: the real rules (reset level to the first-job
 * level, keep 3 random job skills) belong in JobService and are not built yet.
 */
public final class AwakeningService {
    private AwakeningService() {}

    /** Right-click with an empty hand or another item. */
    public static void touch(ServerPlayer player, BlockPos stonePos) {
        system(player, Component.literal("An Awakening Stone. Offer an Awakening Shard to reawaken your job.")
                .withStyle(ChatFormatting.WHITE));
        player.level().playSound(null, stonePos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.7F);
    }

    /** Right-click holding an Awakening Shard. */
    public static void awaken(ServerPlayer player, ItemStack shard, BlockPos stonePos) {
        if (!JobService.get(player).hasJob()) {
            system(player, Component.literal("The stone does not answer. You have no job to reawaken.")
                    .withStyle(ChatFormatting.WHITE));
            return;
        }

        // TODO(gemini): replace with the real reroll: reset level to firstJobLevel, keep 3 random job skills.
        Job job = JobService.reroll(player);
        if (job == null) return;

        if (!player.getAbilities().instabuild) {
            shard.shrink(1);
        }

        ServerLevel level = player.serverLevel();
        double x = stonePos.getX() + 0.5;
        double y = stonePos.getY() + 1.0;
        double z = stonePos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 80, 0.6, 1.0, 0.6, 0.08);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.8, z, 30, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, stonePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.4F);
        level.playSound(null, stonePos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.5F);
    }

    private static void system(ServerPlayer player, Component message) {
        player.sendSystemMessage(Component.literal("[System] ").withStyle(ChatFormatting.AQUA).append(message));
    }
}
