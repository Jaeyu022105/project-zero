package com.projectzero.gates;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;

/**
 * Balance and spawning constants for Gates (DESIGN-DOCUMENT.md §9).
 */
public final class GateRules {
    private GateRules() {}

    /** Gamerule controlling natural random Gate spawning near players (off by default). */
    public static final GameRules.Key<GameRules.BooleanValue> RULE_GATE_SPAWNING =
            GameRules.register("projectzero:gateSpawning", GameRules.Category.SPAWNING,
                    GameRules.BooleanValue.create(false));

    public static final int SPAWN_CHECK_INTERVAL_TICKS = 20 * 60; // every 60s
    public static final double SPAWN_CHANCE_PER_CHECK = 0.25;
    public static final int SPAWN_MIN_DISTANCE = 20;
    public static final int SPAWN_MAX_DISTANCE = 44;

    public static final int PORTAL_COOLDOWN_TICKS = 40;
    /** Water breathing granted on entering a sea gate. Air pockets in the chambers cover the rest. */
    public static final int SEA_GATE_BREATHING_TICKS = 20 * 180;
    public static final int DUNGEON_SLOT_STRIDE = 512;
    public static final int DUNGEON_FLOOR_Y = 64;

    /** Extra health multiplier on a gate's final boss on top of {@link GateRank#multiplier()}. */
    public static final double BOSS_EXTRA_HEALTH_MULT = 2.5;

    /** Forces class initialization so {@link #RULE_GATE_SPAWNING} registers during mod setup. */
    public static void init() {}

    public static GateRank randomRank(RandomSource random) {
        int r = random.nextInt(100);
        if (r < 40) return GateRank.E;
        if (r < 68) return GateRank.D;
        if (r < 85) return GateRank.C;
        if (r < 94) return GateRank.B;
        if (r < 99) return GateRank.A;
        return GateRank.S;
    }

    public static GateSize randomSize(RandomSource random) {
        int r = random.nextInt(100);
        if (r < 55) return GateSize.SMALL;
        if (r < 88) return GateSize.MEDIUM;
        return GateSize.LARGE;
    }
}
