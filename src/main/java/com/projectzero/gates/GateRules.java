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

    /** Gamerule controlling uncleared Gate aging (surges every 7 days, rank-up every 28 days; on by default). */
    public static final GameRules.Key<GameRules.BooleanValue> RULE_GATE_AGING =
            GameRules.register("projectzero:gateAging", GameRules.Category.SPAWNING,
                    GameRules.BooleanValue.create(true));

    /** Gamerule controlling Dungeon Breaks on the 2nd rank-up or S-rank's next rank-up (on by default). */
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DUNGEON_BREAKS =
            GameRules.register("projectzero:dungeonBreaks", GameRules.Category.SPAWNING,
                    GameRules.BooleanValue.create(true));

    public static final int SPAWN_CHECK_INTERVAL_TICKS = 20 * 60; // every 60s
    public static final double SPAWN_CHANCE_PER_CHECK = 0.25;
    public static final int SPAWN_MIN_DISTANCE = 20;
    public static final int SPAWN_MAX_DISTANCE = 44;

    // ---- Gate aging & Dungeon break balance knobs (DESIGN-DOCUMENT.md §9) ----
    public static final int AGING_CHECK_INTERVAL_TICKS = 1200;       // check once per 60s
    public static final long AGING_SURGE_TICKS = 7L * 24000L;        // 7 in-game days per surge step
    public static final int AGING_MAX_SURGES = 3;                    // 3 surges, then rank-up on 4th 7-day mark (28d)
    public static final int AGING_SURGE_MOB_LEVELS = 3;              // +3 monster levels per surge
    public static final double AGING_SURGE_LOOT_BONUS = 0.05;        // +5% chest gear/shard roll per surge
    public static final double AGING_RANKUP_EXTRA_MOBS = 0.25;       // +25% extra mobs when ranking up a built dungeon
    public static final double AGING_MESSAGE_RADIUS = 128.0;

    public static final int BREAK_AFTER_RANKUPS = 2;                 // break on 2nd rank-up (56d) or S-rank next rank-up
    public static final int BREAK_WAVES = 5;                         // 5 waves, boss on last wave
    public static final int BREAK_WAVE_TICKS = 1200;                 // 60s between waves
    public static final double BREAK_PLAYER_RADIUS = 96.0;           // requires a player within 96 blocks to rupture
    public static final double BREAK_BOSS_CHECK_RADIUS = 64.0;       // living boss check radius after final wave
    public static final int BREAK_BOSS_MISSING_TICKS = 1200;         // 60s missing while player nearby -> respawn once

    /** Share of natural (non-sea) gate spawns that come as ancient gates in a stone ring. */
    public static final double ANCIENT_NATURAL_SHARE = 0.1;
    /**
     * Ancient gates also generate with the terrain: about one per this many chunks. The live value
     * is the rarity_filter in data/projectzero/worldgen/placed_feature/ancient_gate.json.
     */
    public static final int ANCIENT_WORLDGEN_RARITY_CHUNKS = 700;

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
