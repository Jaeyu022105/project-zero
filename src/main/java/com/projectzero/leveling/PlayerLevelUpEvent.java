package com.projectzero.leveling;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Posted on the Forge event bus after a player levels up and the new level is saved.
 * Lets other systems (jobs now; titles and basic skills later) react to levels
 * without the leveling code knowing about them. Fired once per XP grant, even if
 * that grant crossed several levels.
 */
public class PlayerLevelUpEvent extends Event {
    private final ServerPlayer player;
    private final int oldLevel;
    private final int newLevel;

    public PlayerLevelUpEvent(ServerPlayer player, int oldLevel, int newLevel) {
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public ServerPlayer getPlayer() { return player; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
}
