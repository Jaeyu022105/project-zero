package com.projectzero.client;

import com.projectzero.jobs.JobRarity;
import com.projectzero.jobs.PlayerJob;
import org.jetbrains.annotations.Nullable;

/** The local player's job as last sent by the server. No client-only imports on purpose. */
public final class ClientJobCache {
    private ClientJobCache() {}

    private static PlayerJob job = PlayerJob.NONE;
    private static @Nullable JobRarity rarity;

    public static PlayerJob get() {
        return job;
    }

    /** Null when the player has no job, or their job is missing from the server's datapack. */
    public static @Nullable JobRarity rarity() {
        return rarity;
    }

    public static void set(PlayerJob newJob, @Nullable JobRarity newRarity) {
        job = newJob;
        rarity = newRarity;
    }
}
