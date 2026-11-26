package com.projectzero.jobs;

import com.projectzero.ProjectZero;
import com.projectzero.leveling.LevelingService;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SyncJobPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side API for jobs. Other systems (Awakening Stones, trials, job skills) go through
 * here instead of editing job data directly.
 */
public final class JobService {
    private JobService() {}

    /** Key under the player's persistent NBT, next to {@link LevelingService#DATA_KEY}. Copied on death. */
    public static final String DATA_KEY = ProjectZero.MOD_ID + ":job";

    /** The player's job data. Works on either side, but only the server copy is real. */
    public static PlayerJob get(Player player) {
        return PlayerJob.load(player.getPersistentData().getCompound(DATA_KEY));
    }

    /** The player's current job definition, or null if they have none or it was removed from the datapack. */
    public static @Nullable Job currentJob(Player player) {
        PlayerJob data = get(player);
        return data.hasJob() ? JobRegistry.get(data.jobId()) : null;
    }

    /** Saves the job data and sends it to the player's client. */
    public static void set(ServerPlayer player, PlayerJob job) {
        player.getPersistentData().put(DATA_KEY, job.save());
        sync(player, job);
    }

    /** Sends the player's job (and its rarity, which the client cannot look up) to their client. */
    public static void sync(ServerPlayer player) {
        sync(player, get(player));
    }

    private static void sync(ServerPlayer player, PlayerJob job) {
        Job def = job.hasJob() ? JobRegistry.get(job.jobId()) : null;
        ModNetwork.sendToPlayer(player, new SyncJobPacket(job, def != null ? def.rarity().ordinal() : -1));
    }

    /**
     * Gives the Job Change Quest if the player has reached {@link JobRules#JOB_CHANGE_LEVEL}
     * and has no job yet. For now the quest completes on the spot and rolls a random job;
     * the trial dungeon comes later.
     *
     * @return true if a job was assigned
     */
    public static boolean tryJobChangeQuest(ServerPlayer player) {
        if (get(player).hasJob()) return false;
        if (LevelingService.get(player).level() < JobRules.JOB_CHANGE_LEVEL) return false;

        Job job = JobRegistry.random(player.getRandom(), null);
        if (job == null) {
            ProjectZero.LOGGER.error("No jobs loaded; cannot give {} a job", player.getGameProfile().getName());
            return false;
        }

        system(player, Component.literal("You have received a Job Change Quest.").withStyle(ChatFormatting.WHITE));
        system(player, Component.literal("Quest complete.").withStyle(ChatFormatting.WHITE));
        assign(player, job);
        return true;
    }

    /** Sets the player's job, keeps their first-job level, and announces it. */
    public static void assign(ServerPlayer player, Job job) {
        set(player, get(player).withJob(job.id()));
        announce(player, job);
    }

    /**
     * Draws a different random job for the player. Testing only for now; the real reroll
     * (Awakening Stones, §6) uses {@link #awakeningReroll(ServerPlayer)}.
     *
     * @return the new job, or null if no jobs are loaded
     */
    public static @Nullable Job reroll(ServerPlayer player) {
        ResourceLocation current = get(player).jobId();
        Job job = JobRegistry.random(player.getRandom(), current);
        if (job != null) assign(player, job);
        return job;
    }

    /**
     * Real Awakening Stone reroll (DESIGN-DOCUMENT.md §6): draws a new job different from the
     * current one, retains up to {@link JobRules#REROLL_KEPT_JOB_SKILLS} random skills from the
     * old job, resets the player's level to {@link PlayerJob#firstJobLevel()} (0 XP, base stats,
     * refunded ability points), and announces the new job.
     *
     * @return the new job, or {@code null} if the player has no job or no alternative job can be drawn
     */
    public static @Nullable Job awakeningReroll(ServerPlayer player) {
        PlayerJob current = get(player);
        if (!current.hasJob()) return null;

        Job next = JobRegistry.random(player.getRandom(), current.jobId());
        if (next == null || next.id().equals(current.jobId())) {
            ProjectZero.LOGGER.warn("Cannot awakening-reroll {}: no alternative job loaded",
                    player.getGameProfile().getName());
            return null;
        }

        int resetLevel = current.firstJobLevel();
        retainRandomJobSkillsOnReroll(player, current.jobId());
        LevelingService.resetToLevel(player, resetLevel);
        assign(player, next);
        system(player, Component.literal("Level reset to " + resetLevel + ". Ability points refunded.")
                .withStyle(ChatFormatting.WHITE));
        return next;
    }

    /**
     * Keeps up to {@link JobRules#REROLL_KEPT_JOB_SKILLS} random skills from the player's previous
     * job during an Awakening Stone reroll (basic, milestone, and skillbook skills are always kept).
     */
    private static void retainRandomJobSkillsOnReroll(ServerPlayer player, ResourceLocation previousJobId) {
        // TODO(skills): once the skill system exists, keep up to JobRules.REROLL_KEPT_JOB_SKILLS
        // random learned skills belonging to previousJobId and remove the remaining skills of that job.
    }

    /** Removes the player's job, so the Job Change Quest can trigger again. Testing only. */
    public static void clear(ServerPlayer player) {
        set(player, PlayerJob.NONE);
    }

    /** Copies job data from the old player to the new one on respawn or leaving the End. */
    public static void copy(Player from, Player to) {
        CompoundTag data = from.getPersistentData().getCompound(DATA_KEY);
        to.getPersistentData().put(DATA_KEY, data.copy());
    }

    // ---- messages ----

    /** "Hexblade" in its rarity color. */
    public static MutableComponent coloredName(Job job) {
        return job.displayName().copy().withStyle(job.rarity().chatColor());
    }

    private static void announce(ServerPlayer player, Job job) {
        Component rarity = job.rarity().displayName().copy().withStyle(job.rarity().chatColor());

        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("JOB CHANGE").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                coloredName(job).append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                        .append(rarity)
                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY))));

        system(player, Component.literal("Your job is now ").withStyle(ChatFormatting.WHITE)
                .append(coloredName(job))
                .append(Component.literal(" [").withStyle(ChatFormatting.GRAY))
                .append(rarity)
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void system(ServerPlayer player, Component message) {
        player.sendSystemMessage(Component.literal("[System] ").withStyle(ChatFormatting.AQUA).append(message));
    }
}
