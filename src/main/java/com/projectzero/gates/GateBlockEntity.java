package com.projectzero.gates;

import com.projectzero.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity attached to each {@link GateBlock}, storing rank, size, open timestamp,
 * linked dungeon slot, anchor position, aging surges/rank-ups, and dungeon-break wave state.
 */
public class GateBlockEntity extends BlockEntity {
    private GateRank rank = GateRank.E;
    private GateSize size = GateSize.SMALL;
    private long openTime = 0L;
    private int dungeonSlot = -1;
    private BlockPos anchorPos = BlockPos.ZERO;

    private int surges = 0;
    private long lastAgeCheck = 0L;
    private int rankUps = 0;

    private boolean breaking = false;
    private int breakWave = 0;
    private long nextWaveTime = 0L;

    public GateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GATE_PORTAL.get(), pos, state);
        this.anchorPos = pos;
    }

    public GateRank rank() { return rank; }
    public GateSize size() { return size; }
    public long openTime() { return openTime; }
    public int dungeonSlot() { return dungeonSlot; }
    public BlockPos anchorPos() { return anchorPos; }

    public int surges() { return surges; }
    public long lastAgeCheck() { return lastAgeCheck; }
    public int rankUps() { return rankUps; }
    public boolean isBreaking() { return breaking; }
    public int breakWave() { return breakWave; }
    public long nextWaveTime() { return nextWaveTime; }

    public void configure(GateRank rank, GateSize size, long openTime, int dungeonSlot, BlockPos anchorPos) {
        this.rank = rank;
        this.size = size;
        this.openTime = openTime;
        this.dungeonSlot = dungeonSlot;
        this.anchorPos = anchorPos;
        setChanged();
    }

    public void setAgingState(GateRank rank, long openTime, int surges, int rankUps, long lastAgeCheck) {
        this.rank = rank;
        this.openTime = openTime;
        this.surges = surges;
        this.rankUps = rankUps;
        this.lastAgeCheck = lastAgeCheck;
        setChanged();
    }

    public void setLastAgeCheck(long lastAgeCheck) {
        this.lastAgeCheck = lastAgeCheck;
        setChanged();
    }

    public void shiftOpenTime(long deltaTicks) {
        this.openTime += deltaTicks;
        setChanged();
    }

    public void setBreakState(boolean breaking, int breakWave, long nextWaveTime) {
        this.breaking = breaking;
        this.breakWave = breakWave;
        this.nextWaveTime = nextWaveTime;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GateBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (state.getValue(GateBlock.EXIT)) return;
        if (!pos.equals(be.anchorPos)) return;
        GateService.tickAnchorGate(serverLevel, pos, state, be);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Rank", rank.name());
        tag.putString("Size", size.name());
        tag.putLong("OpenTime", openTime);
        tag.putInt("DungeonSlot", dungeonSlot);
        tag.putLong("AnchorPos", anchorPos.asLong());
        tag.putInt("Surges", surges);
        tag.putLong("LastAgeCheck", lastAgeCheck);
        tag.putInt("RankUps", rankUps);
        tag.putBoolean("Breaking", breaking);
        tag.putInt("BreakWave", breakWave);
        tag.putLong("NextWaveTime", nextWaveTime);
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
        this.surges = Math.max(0, tag.getInt("Surges"));
        this.lastAgeCheck = tag.getLong("LastAgeCheck");
        this.rankUps = Math.max(0, tag.getInt("RankUps"));
        this.breaking = tag.getBoolean("Breaking");
        this.breakWave = Math.max(0, tag.getInt("BreakWave"));
        this.nextWaveTime = tag.getLong("NextWaveTime");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The rift is drawn from one block but spans the whole portal, so widen the cull box. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(5.0);
    }
}
