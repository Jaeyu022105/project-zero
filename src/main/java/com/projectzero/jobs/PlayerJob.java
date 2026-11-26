package com.projectzero.jobs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A player's job state. Plain data, no game logic hooks.
 *
 * @param jobId         the current job, or null before the Job Change Quest
 * @param jobLevel      level inside the job (job skills will unlock by this)
 * @param firstJobLevel player level when they first got a job; Awakening Stone rerolls reset to it (§6)
 */
public record PlayerJob(@Nullable ResourceLocation jobId, int jobLevel, int firstJobLevel) {
    public static final PlayerJob NONE = new PlayerJob(null, 0, 0);

    public boolean hasJob() {
        return jobId != null;
    }

    /** Same history, different job. Used by the quest, rerolls and /pz job set. */
    public PlayerJob withJob(ResourceLocation newJob) {
        int first = hasJob() ? firstJobLevel : JobRules.JOB_CHANGE_LEVEL;
        return new PlayerJob(newJob, JobRules.STARTING_JOB_LEVEL, first);
    }

    // ---- persistence ----

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (jobId != null) {
            tag.putString("job", jobId.toString());
            tag.putInt("jobLevel", jobLevel);
            tag.putInt("firstJobLevel", firstJobLevel);
        }
        return tag;
    }

    public static PlayerJob load(CompoundTag tag) {
        ResourceLocation id = tag.contains("job") ? ResourceLocation.tryParse(tag.getString("job")) : null;
        if (id == null) return NONE;
        return new PlayerJob(id,
                Math.max(JobRules.STARTING_JOB_LEVEL, tag.getInt("jobLevel")),
                Math.max(1, tag.getInt("firstJobLevel")));
    }

    // ---- networking ----

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(jobId != null);
        if (jobId != null) {
            buf.writeResourceLocation(jobId);
            buf.writeVarInt(jobLevel);
            buf.writeVarInt(firstJobLevel);
        }
    }

    public static PlayerJob read(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return NONE;
        return new PlayerJob(buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt());
    }
}
