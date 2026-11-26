package com.projectzero.leveling;

import com.projectzero.ProjectZero;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SyncStatsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

/**
 * Server-side API for the leveling system. Everything else (kill rewards, quests,
 * dungeon clears, boss drops) should go through here instead of editing data directly.
 */
public final class LevelingService {
    private LevelingService() {}

    /** Key under the player's persistent NBT. Saved with the player and copied on death. */
    public static final String DATA_KEY = ProjectZero.MOD_ID + ":stats";

    public static PlayerStats get(Player player) {
        return PlayerStats.load(player.getPersistentData().getCompound(DATA_KEY));
    }

    public static void set(ServerPlayer player, PlayerStats stats) {
        player.getPersistentData().put(DATA_KEY, stats.save());
        StatEffects.apply(player, stats);
        sync(player, stats);
    }

    public static void sync(ServerPlayer player) {
        sync(player, get(player));
    }

    private static void sync(ServerPlayer player, PlayerStats stats) {
        ModNetwork.sendToPlayer(player, new SyncStatsPacket(stats));
    }

    /** Grants XP, handles level ups, and tells the player. */
    public static void grantXp(ServerPlayer player, long amount) {
        if (amount <= 0) return;
        PlayerStats stats = get(player);
        if (stats.isMaxLevel()) return;

        double xpMult = 1.0 + com.projectzero.titles.TitleService.xpBonusMultiplier(player);
        long finalAmount = Math.max(1L, Math.round(amount * xpMult));

        int levelsGained = stats.addXp(finalAmount);
        set(player, stats);

        player.displayClientMessage(
                Component.literal("+" + finalAmount + " XP").withStyle(ChatFormatting.AQUA), true);

        if (levelsGained > 0) {
            onLevelUp(player, stats, levelsGained);
            MinecraftForge.EVENT_BUS.post(
                    new PlayerLevelUpEvent(player, stats.level() - levelsGained, stats.level()));
        }
    }

    public static boolean allocate(ServerPlayer player, Stat stat) {
        PlayerStats stats = get(player);
        if (!stats.allocate(stat)) return false;
        set(player, stats);
        if (stat == Stat.INTELLIGENCE) {
            com.projectzero.skills.SkillService.sync(player);
        }
        return true;
    }

    public static void reset(ServerPlayer player) {
        set(player, new PlayerStats());
        player.setHealth(player.getMaxHealth());
    }

    /**
     * Resets the player to {@code targetLevel} with 0 XP, base stats, and recomputed unspent points
     * ({@code (level - 1) * STAT_POINTS_PER_LEVEL}). Used by Awakening Stone job rerolls.
     */
    public static void resetToLevel(ServerPlayer player, int targetLevel) {
        set(player, PlayerStats.resetToLevel(targetLevel));
        player.setHealth(player.getMaxHealth());
    }

    private static void onLevelUp(ServerPlayer player, PlayerStats stats, int levelsGained) {
        // Leveling up fully restores the player.
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        com.projectzero.skills.SkillService.setMana(player, com.projectzero.skills.SkillService.maxMana(player));

        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("LEVEL UP!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("Level " + stats.level()).withStyle(ChatFormatting.WHITE)));

        player.sendSystemMessage(Component.literal("[System] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("You have leveled up! ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("+" + levelsGained * LevelingRules.STAT_POINTS_PER_LEVEL
                        + " stat points").withStyle(ChatFormatting.YELLOW)));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Copies data from the old player to the new one when the player respawns or leaves the End. */
    public static void copy(Player from, Player to) {
        CompoundTag data = from.getPersistentData().getCompound(DATA_KEY);
        to.getPersistentData().put(DATA_KEY, data.copy());
    }
}
