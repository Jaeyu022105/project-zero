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
     * and has no job yet. Opens the Library domain, where the player picks a face-down card;
     * falls back to an instant random job if the domain cannot open.
     *
     * @return true if a job was assigned right away (fallback only)
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
        // The job is chosen in the Library domain (face-down cards). If it cannot open,
        // fall back to the instant random job so nobody is left without one.
        if (com.projectzero.domain.LibraryDomain.isInDomain(player) || com.projectzero.domain.LibraryDomain.open(player)) {
            return false;
        }
        system(player, Component.literal("Quest complete.").withStyle(ChatFormatting.WHITE));
        assign(player, job);
        return true;
    }

    /** Sets the player's job, keeps their first-job level, grants its starter skills, and announces it. */
    public static void assign(ServerPlayer player, Job job) {
        PlayerJob updated = get(player).withJob(job.id());
        set(player, updated);
        com.projectzero.skills.SkillService.grantJobSkills(player, job.id(), updated.jobLevel(), true);
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
     * What an Awakening Stone reroll did: the new job, the level the player was reset to, and
     * the Awakening Shard charge tier (0..5) used for the roll (passed to the vision).
     */
    public record AwakeningResult(Job job, int resetLevel, int shardTier) {}

    public static @Nullable Job awakeningReroll(ServerPlayer player) {
        return awakeningReroll(player, 0);
    }

    public static @Nullable Job awakeningReroll(ServerPlayer player, int shardTier) {
        AwakeningResult result = awakeningRerollQuiet(player, shardTier);
        if (result == null) return null;
        announceAwakening(player, result);
        return result.job();
    }

    public static @Nullable AwakeningResult awakeningRerollQuiet(ServerPlayer player) {
        return awakeningRerollQuiet(player, 0);
    }

    /**
     * Applies the reroll without telling the player yet: draws a new job different from the
     * current one using {@code shardTier}'s rarity weights, retains up to
     * {@link JobRules#REROLL_KEPT_JOB_SKILLS} random skills from the old job, and resets the
     * player's level to {@code min(current level, firstJobLevel)}.
     */
    public static @Nullable AwakeningResult awakeningRerollQuiet(ServerPlayer player, int shardTier) {
        PlayerJob current = get(player);
        if (!current.hasJob()) return null;

        int clampedTier = Math.max(0, Math.min(5, shardTier));
        Job next = JobRegistry.random(player.getRandom(), current.jobId(),
                JobRules.weightsForShardTier(clampedTier));
        if (next == null || next.id().equals(current.jobId())) {
            ProjectZero.LOGGER.warn("Cannot awakening-reroll {}: no alternative job loaded",
                    player.getGameProfile().getName());
            return null;
        }

        int resetLevel = Math.min(LevelingService.get(player).level(), current.firstJobLevel());
        retainRandomJobSkillsOnReroll(player, current.jobId());
        LevelingService.resetToLevel(player, resetLevel);
        PlayerJob updated = get(player).withJob(next.id());
        set(player, updated);
        com.projectzero.skills.SkillService.grantJobSkills(player, next.id(), updated.jobLevel(), true);
        com.projectzero.titles.MilestoneTracker.recordReroll(player);
        return new AwakeningResult(next, resetLevel, clampedTier);
    }

    /** The title, chat line and sound for a finished reroll. */
    public static void announceAwakening(ServerPlayer player, AwakeningResult result) {
        announce(player, result.job(), "REAWAKENED");
        system(player, Component.literal("Level reset to " + result.resetLevel() + ". Ability points refunded.")
                .withStyle(ChatFormatting.WHITE));
    }

    /**
     * Keeps up to {@link JobRules#REROLL_KEPT_JOB_SKILLS} random skills from the player's previous
     * job during an Awakening Stone reroll (basic, milestone, and skillbook skills are always kept).
     */
    private static void retainRandomJobSkillsOnReroll(ServerPlayer player, ResourceLocation previousJobId) {
        com.projectzero.skills.SkillService.retainRandomJobSkillsOnReroll(player, previousJobId);
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
        announce(player, job, "JOB CHANGE");
    }

    private static void announce(ServerPlayer player, Job job, String header) {
        Component rarity = job.rarity().displayName().copy().withStyle(job.rarity().chatColor());

        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(header).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
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
