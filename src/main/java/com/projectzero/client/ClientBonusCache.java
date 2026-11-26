package com.projectzero.client;

import com.projectzero.leveling.Stat;

import java.util.EnumMap;
import java.util.Map;

/**
 * Local player's synced bonus stat points (from equipped Title + Accessories).
 * No client-only imports so packet handlers can safely reference this class on a dedicated server.
 */
public final class ClientBonusCache {
    private ClientBonusCache() {}

    private static Map<Stat, Integer> bonuses = Map.of();

    public static int get(Stat stat) {
        return bonuses.getOrDefault(stat, 0);
    }

    public static void set(Map<Stat, Integer> newBonuses) {
        EnumMap<Stat, Integer> copy = new EnumMap<>(Stat.class);
        copy.putAll(newBonuses);
        bonuses = copy;
    }

    public static void reset() {
        bonuses = Map.of();
    }
}
