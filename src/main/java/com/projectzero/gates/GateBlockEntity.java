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
    private long pausedAgingTicks = 0L;
    private long lastAgingTickSeen = -1L;

    private boolean breaking = false;
    private int breakWave = 0;
    private long nextWaveTime = 0L;
    private boolean stormable = false;
    private long pausedBreakTicks = 0L;
    private long lastBreakTickSeen = -1L;
    private boolean bossRespawned = false;
    private long missingBossTicks = 0L;
    private long lastBossCheckTick = -1L;

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
    public long pausedAgingTicks() { return pausedAgingTicks; }
    public boolean isBreaking() { return breaking; }
    public int breakWave() { return breakWave; }
    public long nextWaveTime() { return nextWaveTime; }
    public boolean isStormable() { return stormable; }
    public long pausedBreakTicks() { return pausedBreakTicks; }
    public boolean bossRespawned() { return bossRespawned; }

    public void configure(GateRank rank, GateSize size, long openTime, int dungeonSlot, BlockPos anchorPos) {
        this.rank = rank;
        this.size = size;
        this.openTime = openTime;
        this.dungeonSlot = dungeonSlot;
        this.anchorPos = anchorPos;
        this.lastAgingTickSeen = openTime;
        setChanged();
    }

    public void setAgingState(GateRank rank, long openTime, int surges, int rankUps, long lastAgeCheck) {
        this.rank = rank;
        this.openTime = openTime;
        this.surges = surges;
        this.rankUps = rankUps;
        this.lastAgeCheck = lastAgeCheck;
        this.lastAgingTickSeen = lastAgeCheck;
        setChanged();
    }

    public void setLastAgeCheck(long lastAgeCheck) {
        this.lastAgeCheck = lastAgeCheck;
        this.lastAgingTickSeen = lastAgeCheck;
        setChanged();
    }

    /** Accumulates paused aging ticks in memory without dirtying the chunk every tick. */
    public long recordPausedAgingStep(long now) {
        long dt = (lastAgingTickSeen >= 0L && now >= lastAgingTickSeen) ? (now - lastAgingTickSeen) : 1L;
        lastAgingTickSeen = now;
        pausedAgingTicks += dt;
        return pausedAgingTicks;
    }

    public void markAgingActiveTick(long now) {
        this.lastAgingTickSeen = now;
    }

    public void clearPausedAgingTicks() {
        this.pausedAgingTicks = 0L;
    }

    public void shiftOpenTime(long deltaTicks) {
        this.openTime += deltaTicks;
        setChanged();
    }

    public void setBreakState(boolean breaking, int breakWave, long nextWaveTime) {
        this.breaking = breaking;
        this.breakWave = breakWave;
        this.nextWaveTime = nextWaveTime;
        this.stormable = false;
        this.lastBreakTickSeen = nextWaveTime;
        if (!breaking || breakWave == 0) {
            this.bossRespawned = false;
            this.missingBossTicks = 0L;
            this.lastBossCheckTick = -1L;
        }
        setChanged();
    }

    public void setStormable(boolean stormable) {
        this.stormable = stormable;
        setChanged();
    }

    public void setNextWaveTime(long nextWaveTime) {
        this.nextWaveTime = nextWaveTime;
        setChanged();
    }

    /** Accumulates paused break-wave ticks in memory without dirtying the chunk every tick. */
    public long recordPausedBreakStep(long now) {
        long dt = (lastBreakTickSeen >= 0L && now >= lastBreakTickSeen) ? (now - lastBreakTickSeen) : 1L;
        lastBreakTickSeen = now;
        pausedBreakTicks += dt;
        return pausedBreakTicks;
    }

    public void markBreakActiveTick(long now) {
        this.lastBreakTickSeen = now;
    }

    public void clearPausedBreakTicks() {
        this.pausedBreakTicks = 0L;
    }

    public void setBossRespawned(boolean bossRespawned) {
        this.bossRespawned = bossRespawned;
        setChanged();
    }

    public void resetBossCheckClock(long now) {
        this.lastBossCheckTick = now;
    }

    public long consumeBossCheckElapsed(long now, long minStepTicks) {
        if (lastBossCheckTick < 0L) {
            lastBossCheckTick = now;
            return 0L;
        }
        long elapsed = now - lastBossCheckTick;
        if (elapsed < minStepTicks) return 0L;
        lastBossCheckTick = now;
        return elapsed;
    }

    public long addMissingBossTicks(long delta) {
        this.missingBossTicks += delta;
        return this.missingBossTicks;
    }

    public void resetMissingBossTicks() {
        this.missingBossTicks = 0L;
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
        if (pausedAgingTicks > 0L) tag.putLong("PausedAgingTicks", pausedAgingTicks);
        tag.putBoolean("Breaking", breaking);
        tag.putInt("BreakWave", breakWave);
        tag.putLong("NextWaveTime", nextWaveTime);
        tag.putBoolean("Stormable", stormable);
        if (pausedBreakTicks > 0L) tag.putLong("PausedBreakTicks", pausedBreakTicks);
        if (bossRespawned) tag.putBoolean("BossRespawned", true);
        if (missingBossTicks > 0L) tag.putLong("MissingBossTicks", missingBossTicks);
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
        this.pausedAgingTicks = Math.max(0L, tag.getLong("PausedAgingTicks"));
        this.breaking = tag.getBoolean("Breaking");
        this.breakWave = Math.max(0, tag.getInt("BreakWave"));
        this.nextWaveTime = tag.getLong("NextWaveTime");
        this.stormable = tag.getBoolean("Stormable");
        this.pausedBreakTicks = Math.max(0L, tag.getLong("PausedBreakTicks"));
        this.bossRespawned = tag.getBoolean("BossRespawned");
        this.missingBossTicks = Math.max(0L, tag.getLong("MissingBossTicks"));
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
