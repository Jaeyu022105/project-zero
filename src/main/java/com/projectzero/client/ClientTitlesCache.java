package com.projectzero.client;

import com.projectzero.titles.PlayerTitles;

/**
 * Local player's synced unlocked titles and currently equipped title. No client-only imports so
 * packet handlers can safely reference this class on a dedicated server.
 */
public final class ClientTitlesCache {
    private ClientTitlesCache() {}

    private static PlayerTitles titles = new PlayerTitles();

    public static PlayerTitles get() {
        return titles;
    }

    public static void set(PlayerTitles newTitles) {
        titles = newTitles;
    }

    public static void reset() {
        titles = new PlayerTitles();
    }
}
