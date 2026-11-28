package com.projectzero.client;

import com.projectzero.companions.PlayerCompanions;

/** Synced local roster. This class intentionally has no Minecraft client imports. */
public final class ClientCompanionsCache {
    private ClientCompanionsCache() {}

    private static PlayerCompanions companions = new PlayerCompanions();

    public static PlayerCompanions get() { return companions; }
    public static void set(PlayerCompanions value) { companions = value; }
    public static void reset() { companions = new PlayerCompanions(); }
}
