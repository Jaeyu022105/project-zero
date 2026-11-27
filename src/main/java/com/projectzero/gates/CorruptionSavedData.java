package com.projectzero.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-dimension SavedData recording every terrain block converted by Dungeon Break corruption
 * along with its original {@link BlockState}, keyed by gate anchor position.
 */
public class CorruptionSavedData extends SavedData {
    private static final String DATA_NAME = "projectzero_corruption";

    public static final class CorruptionZone {
        public final BlockPos anchor;
        public int slot;
        public GateRank rank;
        public boolean sea;
        public int targetRadius;
        public boolean receding;
        public int recedeTicksRemaining;
        /** Original block states prior to corruption, keyed by immutable world position. */
        public final LinkedHashMap<BlockPos, BlockState> originalBlocks = new LinkedHashMap<>();

        public CorruptionZone(BlockPos anchor, int slot, GateRank rank, boolean sea, int targetRadius) {
            this.anchor = anchor.immutable();
            this.slot = slot;
            this.rank = rank;
            this.sea = sea;
            this.targetRadius = targetRadius;
        }
    }

    private final LinkedHashMap<BlockPos, CorruptionZone> zones = new LinkedHashMap<>();

    public static CorruptionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CorruptionSavedData::new, CorruptionSavedData::load, null),
                DATA_NAME);
    }

    public Collection<CorruptionZone> zones() {
        return zones.values();
    }

    public @Nullable CorruptionZone zone(BlockPos anchor) {
        return zones.get(anchor);
    }

    public CorruptionZone getOrCreate(BlockPos anchor, int slot, GateRank rank, boolean sea, int targetRadius) {
        CorruptionZone z = zones.get(anchor);
        if (z == null) {
            z = new CorruptionZone(anchor, slot, rank, sea, targetRadius);
            zones.put(anchor.immutable(), z);
            setDirty();
        } else {
            z.slot = slot;
            z.rank = rank;
            z.sea = sea;
            if (targetRadius > z.targetRadius) {
                z.targetRadius = Math.min(GateRules.corruptionRadius(rank), targetRadius);
            }
            z.receding = false;
            setDirty();
        }
        return z;
    }

    public void removeZone(BlockPos anchor) {
        if (zones.remove(anchor) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag zoneList = new ListTag();
        for (CorruptionZone z : zones.values()) {
            CompoundTag zt = new CompoundTag();
            zt.putLong("Anchor", z.anchor.asLong());
            zt.putInt("Slot", z.slot);
            zt.putString("Rank", z.rank.name());
            zt.putBoolean("Sea", z.sea);
            zt.putInt("TargetRadius", z.targetRadius);
            zt.putBoolean("Receding", z.receding);
            zt.putInt("RecedeTicks", z.recedeTicksRemaining);

            ListTag blocks = new ListTag();
            for (Map.Entry<BlockPos, BlockState> e : z.originalBlocks.entrySet()) {
                CompoundTag bt = new CompoundTag();
                bt.putLong("Pos", e.getKey().asLong());
                bt.put("State", NbtUtils.writeBlockState(e.getValue()));
                blocks.add(bt);
            }
            zt.put("Blocks", blocks);
            zoneList.add(zt);
        }
        tag.put("Zones", zoneList);
        return tag;
    }

    public static CorruptionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CorruptionSavedData data = new CorruptionSavedData();
        HolderGetter<Block> blockLookup = registries.lookupOrThrow(Registries.BLOCK);
        ListTag zoneList = tag.getList("Zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < zoneList.size(); i++) {
            CompoundTag zt = zoneList.getCompound(i);
            BlockPos anchor = BlockPos.of(zt.getLong("Anchor"));
            int slot = zt.getInt("Slot");
            GateRank rank = GateRank.byName(zt.getString("Rank"));
            if (rank == null) rank = GateRank.E;
            boolean sea = zt.getBoolean("Sea");
            int targetRadius = zt.getInt("TargetRadius");

            CorruptionZone z = new CorruptionZone(anchor, slot, rank, sea, targetRadius);
            z.receding = zt.getBoolean("Receding");
            z.recedeTicksRemaining = zt.getInt("RecedeTicks");

            ListTag blocks = zt.getList("Blocks", Tag.TAG_COMPOUND);
            for (int j = 0; j < blocks.size(); j++) {
                CompoundTag bt = blocks.getCompound(j);
                BlockPos pos = BlockPos.of(bt.getLong("Pos"));
                BlockState st = NbtUtils.readBlockState(blockLookup, bt.getCompound("State"));
                z.originalBlocks.put(pos, st);
            }
            data.zones.put(anchor, z);
        }
        return data;
    }
}
