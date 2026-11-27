package com.projectzero.waypoints;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Waypoint markers recorded independently in each dimension. */
public final class WaypointSavedData extends SavedData {
    private static final String DATA_NAME = "projectzero_waypoints";
    private final LinkedHashMap<BlockPos, Waypoint> waypoints = new LinkedHashMap<>();

    public record Waypoint(BlockPos pos, String name, UUID owner) {
        public Waypoint { pos = pos.immutable(); }
    }

    public static WaypointSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WaypointSavedData::new, WaypointSavedData::load, null), DATA_NAME);
    }

    public Collection<Waypoint> all() { return waypoints.values(); }
    public @Nullable Waypoint get(BlockPos pos) { return waypoints.get(pos); }

    public Waypoint add(BlockPos pos, String name, UUID owner) {
        Waypoint waypoint = new Waypoint(pos, name, owner);
        waypoints.put(waypoint.pos(), waypoint);
        setDirty();
        return waypoint;
    }

    public boolean rename(BlockPos pos, String name) {
        Waypoint old = waypoints.get(pos);
        if (old == null) return false;
        waypoints.put(pos.immutable(), new Waypoint(pos, name, old.owner()));
        setDirty();
        return true;
    }

    public void remove(BlockPos pos) {
        if (waypoints.remove(pos) != null) setDirty();
    }

    public String nextName(String biomeName) {
        int max = 0;
        String prefix = biomeName + " ";
        for (Waypoint waypoint : waypoints.values()) {
            if (!waypoint.name().startsWith(prefix)) continue;
            try { max = Math.max(max, Integer.parseInt(waypoint.name().substring(prefix.length()))); }
            catch (NumberFormatException ignored) { }
        }
        return prefix + (max + 1);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Waypoint waypoint : waypoints.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", waypoint.pos().asLong());
            entry.putString("Name", waypoint.name());
            entry.putUUID("Owner", waypoint.owner());
            list.add(entry);
        }
        tag.put("Waypoints", list);
        return tag;
    }

    public static WaypointSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WaypointSavedData data = new WaypointSavedData();
        ListTag list = tag.getList("Waypoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Owner")) continue;
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            data.waypoints.put(pos, new Waypoint(pos, entry.getString("Name"), entry.getUUID("Owner")));
        }
        return data;
    }
}
