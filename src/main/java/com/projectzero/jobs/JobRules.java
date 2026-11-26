package com.projectzero.jobs;

import java.util.EnumMap;
import java.util.Map;

/** Every balance number for the job system. Default rarity weights live in {@link JobRarity}. */
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

    /** Maximum number of random skills kept from the previous job on an Awakening Stone reroll (§6). */
    public static final int REROLL_KEPT_JOB_SKILLS = 3;

    /**
     * Rarity weight tables by Awakening Shard charge tier 0..5 (DESIGN-DOCUMENT.md §6):
     * columns are COMMON, UNCOMMON, RARE, EPIC, LEGENDARY.
     */
    private static final int[][] SHARD_TIER_WEIGHTS = {
            {50, 28, 15,  6, 1}, // 0: Uncharged
            {44, 30, 17,  7, 2}, // 1: Dim
            {38, 31, 20,  9, 2}, // 2: Glowing
            {31, 31, 23, 12, 3}, // 3: Radiant
            {25, 30, 26, 15, 4}, // 4: Blazing
            {20, 28, 29, 18, 5}  // 5: Resplendent
    };

    public static Map<JobRarity, Integer> weightsForShardTier(int tier) {
        int clamped = Math.max(0, Math.min(SHARD_TIER_WEIGHTS.length - 1, tier));
        int[] row = SHARD_TIER_WEIGHTS[clamped];
        EnumMap<JobRarity, Integer> map = new EnumMap<>(JobRarity.class);
        JobRarity[] rarities = JobRarity.values();
        for (int i = 0; i < rarities.length; i++) {
            map.put(rarities[i], row[i]);
        }
        return map;
    }
}
