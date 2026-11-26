package com.projectzero.leveling;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumMap;
import java.util.Map;

/** A player's level, XP, unspent points and stat values. Plain data, no game logic hooks. */
public final class PlayerStats {
    private int level = 1;
    private long xp = 0;
    private int unspentPoints = 0;
    private final EnumMap<Stat, Integer> stats = new EnumMap<>(Stat.class);

    public PlayerStats() {
        for (Stat stat : Stat.values()) {
            stats.put(stat, Stat.BASE_VALUE);
        }
    }

    public int level() { return level; }
    public long xp() { return xp; }
    public int unspentPoints() { return unspentPoints; }
    public int get(Stat stat) { return stats.get(stat); }
    public int bonus(Stat stat) { return get(stat) - Stat.BASE_VALUE; }
    public boolean isMaxLevel() { return level >= LevelingRules.MAX_LEVEL; }
    public long xpToNext() { return LevelingRules.xpToNextLevel(level); }

    /** Adds XP and returns how many levels were gained. */
    public int addXp(long amount) {
        if (amount <= 0 || isMaxLevel()) return 0;
        xp += amount;
        int gained = 0;
        while (!isMaxLevel() && xp >= xpToNext()) {
            xp -= xpToNext();
            level++;
            unspentPoints += LevelingRules.STAT_POINTS_PER_LEVEL;
            gained++;
        }
        if (isMaxLevel()) xp = 0;
        return gained;
    }

    /** Spends one point on a stat. Returns false if there are no points to spend. */
    public boolean allocate(Stat stat) {
        if (unspentPoints <= 0) return false;
        unspentPoints--;
        stats.merge(stat, 1, Integer::sum);
        return true;
    }

    // ---- persistence ----

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putLong("xp", xp);
        tag.putInt("points", unspentPoints);
        CompoundTag statTag = new CompoundTag();
        for (Map.Entry<Stat, Integer> e : stats.entrySet()) {
            statTag.putInt(e.getKey().key(), e.getValue());
        }
        tag.put("stats", statTag);
        return tag;
    }

    public static PlayerStats load(CompoundTag tag) {
        PlayerStats s = new PlayerStats();
        if (tag.contains("level")) {
            s.level = Math.max(1, Math.min(LevelingRules.MAX_LEVEL, tag.getInt("level")));
            s.xp = Math.max(0, tag.getLong("xp"));
            s.unspentPoints = Math.max(0, tag.getInt("points"));
            CompoundTag statTag = tag.getCompound("stats");
            for (Stat stat : Stat.values()) {
                if (statTag.contains(stat.key())) {
                    s.stats.put(stat, Math.max(Stat.BASE_VALUE, statTag.getInt(stat.key())));
                }
            }
        }
        return s;
    }

    // ---- networking ----

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeVarLong(xp);
        buf.writeVarInt(unspentPoints);
        for (Stat stat : Stat.values()) {
            buf.writeVarInt(stats.get(stat));
        }
    }

    public static PlayerStats read(FriendlyByteBuf buf) {
        PlayerStats s = new PlayerStats();
        s.level = buf.readVarInt();
        s.xp = buf.readVarLong();
        s.unspentPoints = buf.readVarInt();
        for (Stat stat : Stat.values()) {
            s.stats.put(stat, buf.readVarInt());
        }
        return s;
    }
}
