package com.projectzero.jobs;

/** Every balance number for the job system. Rarity weights live in {@link JobRarity}. */
public final class JobRules {
    private JobRules() {}

    /**
     * Level at which the System gives the Job Change Quest (§5). Solo Leveling uses 40;
     * 10 keeps the early game short. Also recorded as the first-job level, which a future
     * Awakening Stone reroll resets the player back to (§6).
     */
    public static final int JOB_CHANGE_LEVEL = 10;

    /** Job level a fresh job starts at. Job XP does not exist yet. */
    public static final int STARTING_JOB_LEVEL = 1;
}
