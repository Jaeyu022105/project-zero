package com.projectzero.client;

import com.projectzero.jobs.JobRarity;
import com.projectzero.jobs.PlayerJob;
import org.jetbrains.annotations.Nullable;

/** The local player's job as last sent by the server. No client-only imports on purpose. */
public final class ClientJobCache {
    private ClientJobCache() {}

    private static PlayerJob job = PlayerJob.NONE;
    private static @Nullable JobRarity rarity;
    private static boolean noCeiling;
    private static boolean companions;

    public static PlayerJob get() {
        return job;
    }

    /** Null when the player has no job, or their job is missing from the server's datapack. */
    public static @Nullable JobRarity rarity() {
        return rarity;
    }

    public static boolean noCeiling() {
        return noCeiling;
    }

    public static boolean hasCompanions() { return companions; }

    public static void set(PlayerJob newJob, @Nullable JobRarity newRarity) {
        set(newJob, newRarity, false, false);
    }

    public static void set(PlayerJob newJob, @Nullable JobRarity newRarity, boolean newNoCeiling) {
        set(newJob, newRarity, newNoCeiling, false);
    }

    public static void set(PlayerJob newJob, @Nullable JobRarity newRarity, boolean newNoCeiling, boolean newCompanions) {
        job = newJob;
        rarity = newRarity;
        noCeiling = newNoCeiling;
        companions = newCompanions;
    }
}
