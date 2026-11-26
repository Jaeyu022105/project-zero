package com.projectzero.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent registry of allocated Gate dungeon instances stored in the {@code projectzero:dungeon} dimension.
 */
public class DungeonSavedData extends SavedData {
    private static final String DATA_NAME = "projectzero_dungeons";

    public static final class DungeonRecord {
        public final int slot;
        public final GateRank rank;
        public final GateSize size;
        public final ResourceLocation originDim;
        public final BlockPos gateAnchor;
        public final boolean alongX;
        public boolean built;
        public boolean cleared;

        public DungeonRecord(int slot, GateRank rank, GateSize size, ResourceLocation originDim,
                             BlockPos gateAnchor, boolean alongX, boolean built, boolean cleared) {
            this.slot = slot;
            this.rank = rank;
            this.size = size;
            this.originDim = originDim;
            this.gateAnchor = gateAnchor;
            this.alongX = alongX;
            this.built = built;
            this.cleared = cleared;
        }
    }

    private int nextSlot = 0;
    private final Map<Integer, DungeonRecord> records = new HashMap<>();

    public static DungeonSavedData get(ServerLevel dungeonLevel) {
        return dungeonLevel.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DungeonSavedData::new, DungeonSavedData::load, null),
                DATA_NAME);
    }

    public DungeonRecord allocate(GateRank rank, GateSize size, ResourceLocation originDim, BlockPos gateAnchor, boolean alongX) {
        int slot = nextSlot++;
        DungeonRecord rec = new DungeonRecord(slot, rank, size, originDim, gateAnchor, alongX, false, false);
        records.put(slot, rec);
        setDirty();
        return rec;
    }

    public @Nullable DungeonRecord record(int slot) {
        return records.get(slot);
    }

    public void markBuilt(int slot) {
        DungeonRecord r = records.get(slot);
        if (r != null) {
            r.built = true;
            setDirty();
        }
    }

    public void markCleared(int slot) {
        DungeonRecord r = records.get(slot);
        if (r != null) {
            r.cleared = true;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("NextSlot", nextSlot);
        ListTag list = new ListTag();
        for (DungeonRecord r : records.values()) {
            CompoundTag rt = new CompoundTag();
            rt.putInt("Slot", r.slot);
            rt.putString("Rank", r.rank.name());
            rt.putString("Size", r.size.name());
            rt.putString("OriginDim", r.originDim.toString());
            rt.putLong("GateAnchor", r.gateAnchor.asLong());
            rt.putBoolean("AlongX", r.alongX);
            rt.putBoolean("Built", r.built);
            rt.putBoolean("Cleared", r.cleared);
            list.add(rt);
        }
        tag.put("Records", list);
        return tag;
    }

    public static DungeonSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonSavedData data = new DungeonSavedData();
        data.nextSlot = tag.getInt("NextSlot");
        ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag rt = list.getCompound(i);
            int slot = rt.getInt("Slot");
            GateRank rank = GateRank.byName(rt.getString("Rank"));
            GateSize size = GateSize.byName(rt.getString("Size"));
            ResourceLocation dim = ResourceLocation.tryParse(rt.getString("OriginDim"));
            if (rank != null && size != null && dim != null) {
                data.records.put(slot, new DungeonRecord(
                        slot, rank, size, dim,
                        BlockPos.of(rt.getLong("GateAnchor")),
                        rt.getBoolean("AlongX"),
                        rt.getBoolean("Built"),
                        rt.getBoolean("Cleared")));
            }
        }
        return data;
    }
}
