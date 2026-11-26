package com.projectzero.client;

import com.projectzero.leveling.PlayerStats;

/** The local player's stats as last sent by the server. Read by the status window and HUD. */
public final class ClientStatsCache {
    private ClientStatsCache() {}

    private static PlayerStats stats = new PlayerStats();

    public static PlayerStats get() {
        return stats;
    }

    public static void set(PlayerStats newStats) {
        stats = newStats;
    }
}
