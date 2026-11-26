package com.projectzero.leveling;

/**
 * Tunable numbers for the leveling system, kept in one place so balancing
 * (or moving these into a config file later) only touches this class.
 */
public final class LevelingRules {
    private LevelingRules() {}

    public static final int MAX_LEVEL = 100;
    public static final int STAT_POINTS_PER_LEVEL = 5;

    /**
     * XP needed to go from {@code level} to {@code level + 1}. Grows faster than linear so
     * every level is harder than the last: about 180 XP at level 1, 5.7k at 10, 111k at 50.
     */
    public static long xpToNextLevel(int level) {
        return Math.round(100 + 80 * Math.pow(level, 1.85));
    }

    // Kill XP multipliers (see XpRewards)
    public static final double VANILLA_XP_MULTIPLIER = 0.3;
    public static final double GATE_MONSTER_XP_MULTIPLIER = 3.0;
    public static final double BOSS_XP_MULTIPLIER = 10.0;

    // Stat effect strengths (per point above Stat.BASE_VALUE)
    public static final double DAMAGE_PER_STRENGTH = 0.5;
    public static final double HEALTH_PER_VITALITY = 1.0;       // half a heart
    public static final double SPEED_PER_AGILITY = 0.0015;      // base player speed is 0.1
    public static final double ATTACK_SPEED_PER_AGILITY = 0.02;
    public static final double EVADE_CHANCE_PER_SENSE = 0.004;  // 0.4% per point
    public static final double MAX_EVADE_CHANCE = 0.30;
    public static final int MANA_PER_INTELLIGENCE = 10;
}
