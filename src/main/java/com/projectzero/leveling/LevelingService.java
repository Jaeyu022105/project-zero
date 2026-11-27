package com.projectzero.leveling;

import com.projectzero.ProjectZero;
import com.projectzero.jobs.JobService;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SyncStatsPacket;
import com.projectzero.ui.NoticeType;
import com.projectzero.ui.SystemNotices;
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

        boolean bypassCeiling = stats.ceilingBroken() || JobService.hasNoCeiling(player);
        if (!bypassCeiling && stats.level() >= LevelingRules.CEILING_LEVEL) {
            if (!stats.ceilingWarned()) {
                stats.setCeilingWarned(true);
                set(player, stats);
                sendCeilingWarning(player);
            }
            return;
        }

        double xpMult = 1.0
                + com.projectzero.titles.TitleService.xpBonusMultiplier(player)
                + com.projectzero.equipment.EquipmentService.xpBonusMultiplier(player);
        long finalAmount = Math.max(1L, Math.round(amount * xpMult));

        int levelsGained = stats.addXp(finalAmount, bypassCeiling);
        boolean reachedCeilingFirstTime = !bypassCeiling
                && stats.level() >= LevelingRules.CEILING_LEVEL
                && !stats.ceilingWarned();
        if (reachedCeilingFirstTime) {
            stats.setCeilingWarned(true);
        }
        set(player, stats);

        player.displayClientMessage(
                Component.literal("+" + finalAmount + " XP").withStyle(ChatFormatting.AQUA), true);

        if (levelsGained > 0) {
            onLevelUp(player, stats, levelsGained);
            MinecraftForge.EVENT_BUS.post(
                    new PlayerLevelUpEvent(player, stats.level() - levelsGained, stats.level()));
        }
        if (reachedCeilingFirstTime) {
            sendCeilingWarning(player);
        }
    }

    /**
     * Breaks the level 100 ceiling for {@code player} (DESIGN-DOCUMENT.md §2, §16).
     * Single entry point to be called by the high-rank NPC trial quest.
     */
    public static boolean breakCeiling(ServerPlayer player) {
        PlayerStats stats = get(player);
        if (stats.ceilingBroken()) return false;
        stats.setCeilingBroken(true);
        stats.setCeilingWarned(true);
        set(player, stats);

        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("LIMIT BROKEN").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("The level ceiling has shattered").withStyle(ChatFormatting.AQUA)));

        Component msg = Component.literal("Your level ceiling has been broken. Growth resumes.")
                .withStyle(ChatFormatting.WHITE);
        SystemNotices.system(player, NoticeType.LEVEL_UP, Component.literal("LIMIT BROKEN"), msg, msg);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.1F);
        return true;
    }

    /** Resets the level ceiling flag (debug/testing). */
    public static void resetCeiling(ServerPlayer player) {
        PlayerStats stats = get(player);
        stats.setCeilingBroken(false);
        stats.setCeilingWarned(false);
        set(player, stats);
    }

    /** Sets the player directly to {@code targetLevel} with base stats and matching unspent points (testing). */
    public static void setLevel(ServerPlayer player, int targetLevel) {
        PlayerStats prev = get(player);
        int oldLevel = prev.level();
        PlayerStats next = PlayerStats.resetToLevel(targetLevel);
        next.setCeilingBroken(prev.ceilingBroken());
        next.setCeilingWarned(prev.ceilingWarned());
        boolean bypass = next.ceilingBroken() || JobService.hasNoCeiling(player);
        boolean warn = !bypass && next.level() >= LevelingRules.CEILING_LEVEL && !next.ceilingWarned();
        if (warn) {
            next.setCeilingWarned(true);
        }
        set(player, next);
        player.setHealth(player.getMaxHealth());
        if (next.level() > oldLevel) {
            MinecraftForge.EVENT_BUS.post(new PlayerLevelUpEvent(player, oldLevel, next.level()));
        }
        if (warn) {
            sendCeilingWarning(player);
        }
    }

    private static void sendCeilingWarning(ServerPlayer player) {
        Component msg = Component.literal("Level ceiling reached. Someone of high rank must judge you.")
                .withStyle(ChatFormatting.RED);
        SystemNotices.system(player, NoticeType.WARNING, Component.literal("LEVEL CEILING"), msg, msg);
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
        PlayerStats prev = get(player);
        PlayerStats next = PlayerStats.resetToLevel(targetLevel);
        next.setCeilingBroken(prev.ceilingBroken());
        next.setCeilingWarned(prev.ceilingWarned());
        set(player, next);
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

        int bonusPts = levelsGained * LevelingRules.STAT_POINTS_PER_LEVEL;
        Component body = Component.literal("Level " + stats.level() + " · ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("+" + bonusPts + " stat points").withStyle(ChatFormatting.YELLOW));
        Component chat = Component.literal("You have leveled up! ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("+" + bonusPts + " stat points").withStyle(ChatFormatting.YELLOW));
        SystemNotices.system(player, NoticeType.LEVEL_UP, NoticeType.LEVEL_UP.defaultTitle(), body, chat);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Copies data from the old player to the new one when the player respawns or leaves the End. */
    public static void copy(Player from, Player to) {
        CompoundTag data = from.getPersistentData().getCompound(DATA_KEY);
        to.getPersistentData().put(DATA_KEY, data.copy());
    }
}
