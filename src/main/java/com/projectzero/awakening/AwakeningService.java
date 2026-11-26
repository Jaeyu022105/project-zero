package com.projectzero.awakening;

import com.projectzero.domain.LibraryDomain;
import com.projectzero.domain.ReawakeningVision;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What happens when a player uses an Awakening Stone (DESIGN-DOCUMENT.md §6).
 *
 * Delegates the reroll rules to {@link JobService#awakeningReroll(ServerPlayer)} and only
 * consumes the Awakening Shard (in survival) and plays the effects on success.
 */
public final class AwakeningService {
    private AwakeningService() {}

    /** Ticks between two "touch" hints, so holding right-click on a formation does not spam chat. */
    private static final int TOUCH_COOLDOWN_TICKS = 40;
    private static final Map<UUID, Long> LAST_TOUCH = new HashMap<>();

    /** Right-click with an empty hand or another item. */
    public static void touch(ServerPlayer player, BlockPos stonePos) {
        long now = player.serverLevel().getGameTime();
        Long last = LAST_TOUCH.get(player.getUUID());
        if (last != null && now - last < TOUCH_COOLDOWN_TICKS && now >= last) return;
        LAST_TOUCH.put(player.getUUID(), now);

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

        if (ReawakeningVision.isInVision(player) || LibraryDomain.isInDomain(player)) return;

        JobService.AwakeningResult result = JobService.awakeningRerollQuiet(player);
        if (result == null) {
            system(player, Component.literal("The stone does not answer. No other job could be drawn.")
                    .withStyle(ChatFormatting.WHITE));
            return;
        }

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

        // The reroll is already saved; the vision only shows it. Without it, announce at once.
        if (!ReawakeningVision.begin(player, result)) {
            JobService.announceAwakening(player, result);
            ReawakeningVision.auraBurst(player, result.job().rarity());
        }
    }

    private static void system(ServerPlayer player, Component message) {
        player.sendSystemMessage(Component.literal("[System] ").withStyle(ChatFormatting.AQUA).append(message));
    }
}
