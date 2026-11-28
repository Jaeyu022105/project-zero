package com.projectzero.companions;

public final class CompanionRules {
    private CompanionRules() {}

    public static final int MAX_LEVEL = 100;
    public static final int POINTS_PER_LEVEL = 3;
    public static final int BASE_STAT = 10;
    public static final int XP_PER_KILL = 25;
    public static final int OWNER_ASSIST_TICKS = 200;
    public static final int MAX_GEAR_SLOTS = 4;
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_STAT_VALUE = 100_000;
    public static final String[] GEAR_SLOTS = {"weapon", "armor", "charm", "relic"};

    public static long xpToNext(int level) {
        return Math.max(1, level) * 100L;
    }

    public static double houndHealth(CompanionEntry entry) {
        return 20.0 + entry.stat(com.projectzero.leveling.Stat.VITALITY) * 0.75 + entry.level() * 1.5;
    }

    public static double houndDamage(CompanionEntry entry) {
        return 4.0 + entry.stat(com.projectzero.leveling.Stat.STRENGTH) * 0.25 + entry.level() * 0.4;
    }
}
