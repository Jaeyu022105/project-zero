package com.projectzero.gates;

import net.minecraft.world.phys.AABB;
import com.projectzero.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity attached to each {@link GateBlock}, storing rank, size, open timestamp,
 * linked dungeon slot, and the gate frame's lower-left anchor position.
 */
public class GateBlockEntity extends BlockEntity {
    private GateRank rank = GateRank.E;
    private GateSize size = GateSize.SMALL;
    private long openTime = 0L;
    private int dungeonSlot = -1;
    private BlockPos anchorPos = BlockPos.ZERO;

    public GateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GATE_PORTAL.get(), pos, state);
        this.anchorPos = pos;
    }

    public GateRank rank() { return rank; }
    public GateSize size() { return size; }
    public long openTime() { return openTime; }
    public int dungeonSlot() { return dungeonSlot; }
    public BlockPos anchorPos() { return anchorPos; }

    public void configure(GateRank rank, GateSize size, long openTime, int dungeonSlot, BlockPos anchorPos) {
        this.rank = rank;
        this.size = size;
        this.openTime = openTime;
        this.dungeonSlot = dungeonSlot;
        this.anchorPos = anchorPos;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Rank", rank.name());
        tag.putString("Size", size.name());
        tag.putLong("OpenTime", openTime);
        tag.putInt("DungeonSlot", dungeonSlot);
        tag.putLong("AnchorPos", anchorPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        GateRank r = GateRank.byName(tag.getString("Rank"));
        this.rank = r != null ? r : GateRank.E;
        GateSize s = GateSize.byName(tag.getString("Size"));
        this.size = s != null ? s : GateSize.SMALL;
        this.openTime = tag.getLong("OpenTime");
        this.dungeonSlot = tag.contains("DungeonSlot") ? tag.getInt("DungeonSlot") : -1;
        if (tag.contains("AnchorPos")) {
            this.anchorPos = BlockPos.of(tag.getLong("AnchorPos"));
        }
    }

    /** The rift is drawn from one block but spans the whole portal, so widen the cull box. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(5.0);
    }
}
