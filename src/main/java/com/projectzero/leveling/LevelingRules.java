package com.projectzero.leveling;

/**
 * Tunable numbers for the leveling system, kept in one place so balancing
 * (or moving these into a config file later) only touches this class.
 */
public final class LevelingRules {
    private LevelingRules() {}

    public static final int CEILING_LEVEL = 100;
    /** Raised past 100 for post-ceiling play; owner still has to confirm the real cap. */
    public static final int MAX_LEVEL = 200;
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

    // Level-gap XP penalty (DESIGN-DOCUMENT.md §9): -5% per level beyond a 5-level gap, floor 10%.
    public static final int LEVEL_GAP_GRACE = 5;
    public static final double LEVEL_GAP_PENALTY_PER_LEVEL = 0.05;
    public static final double LEVEL_GAP_MIN_MULTIPLIER = 0.10;

    public static double levelGapXpMultiplier(int playerLevel, int mobLevel) {
        int extra = (playerLevel - mobLevel) - LEVEL_GAP_GRACE;
        if (extra <= 0) return 1.0;
        return Math.max(LEVEL_GAP_MIN_MULTIPLIER, 1.0 - extra * LEVEL_GAP_PENALTY_PER_LEVEL);
    }

    // Stat soft-cap curves (DESIGN-DOCUMENT.md §3). p = points above Stat.BASE_VALUE (10).
    // soft(p, k) = p / (1 + p / k). Values at 45 / 100 / 495 points above base:
    //   Strength: +0.02 * soft(p, 200) total attack damage  -> +73% / +133% / +285%
    //   Vitality: +0.50 * soft(p, 300) max health           -> +20 HP / +38 HP / +93 HP
    //   Agility:  +0.40 * p / (p + 60) movement speed       -> +17% / +25% / +36%
    //             +1.50 * p / (p + 60) attack speed         -> +0.64 / +0.94 / +1.34
    //   Sense:     0.30 * p / (p + 100) evade chance        -> 9% / 15% / 25%
    public static final double STRENGTH_MULT_PER_SOFT_POINT = 0.02;
    public static final double STRENGTH_SOFT_K = 200.0;

    public static final double VITALITY_HEALTH_PER_SOFT_POINT = 0.5;
    public static final double VITALITY_SOFT_K = 300.0;

    public static final double AGILITY_MAX_SPEED_BONUS = 0.40;
    public static final double AGILITY_MAX_ATTACK_SPEED_BONUS = 1.5;
    public static final double AGILITY_HALF_CAP = 60.0;

    public static final double MAX_EVADE_CHANCE = 0.30;
    public static final double SENSE_HALF_CAP = 100.0;

    public static final int MANA_PER_INTELLIGENCE = 10;

    public static double soft(double pointsAboveBase, double k) {
        if (pointsAboveBase <= 0) return 0.0;
        return pointsAboveBase / (1.0 + pointsAboveBase / k);
    }

    public static double strengthDamageMultiplier(int pointsAboveBase) {
        return STRENGTH_MULT_PER_SOFT_POINT * soft(pointsAboveBase, STRENGTH_SOFT_K);
    }

    public static double vitalityHealthBonus(int pointsAboveBase) {
        return VITALITY_HEALTH_PER_SOFT_POINT * soft(pointsAboveBase, VITALITY_SOFT_K);
    }

    public static double agilitySpeedMultiplier(int pointsAboveBase) {
        if (pointsAboveBase <= 0) return 0.0;
        return AGILITY_MAX_SPEED_BONUS * pointsAboveBase / (pointsAboveBase + AGILITY_HALF_CAP);
    }

    public static double agilityAttackSpeedBonus(int pointsAboveBase) {
        if (pointsAboveBase <= 0) return 0.0;
        return AGILITY_MAX_ATTACK_SPEED_BONUS * pointsAboveBase / (pointsAboveBase + AGILITY_HALF_CAP);
    }

    public static double senseEvadeChance(int pointsAboveBase) {
        if (pointsAboveBase <= 0) return 0.0;
        return MAX_EVADE_CHANCE * pointsAboveBase / (pointsAboveBase + SENSE_HALF_CAP);
    }
}
