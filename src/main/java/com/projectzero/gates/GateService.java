package com.projectzero.gates;

import com.projectzero.ProjectZero;
import com.projectzero.fx.Fx;
import com.projectzero.registry.ModBlocks;
import com.projectzero.registry.ModItems;
import com.projectzero.titles.MilestoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Server-side manager for spawning Gates, entering/leaving {@code projectzero:dungeon},
 * gate aging (surges + rank-ups), dungeon breaks (5 waves + boss outside), and boss clear flow.
 */
public final class GateService {
    private GateService() {}

    public static final ResourceKey<Level> DUNGEON =
            ResourceKey.create(Registries.DIMENSION, ProjectZero.id("dungeon"));

    private static final String RETURN_KEY = ProjectZero.MOD_ID + ":gate_return";
    private static final String COOLDOWN_KEY = ProjectZero.MOD_ID + ":gate_cooldown_until";
    private static final String ACTIVE_SLOT_KEY = ProjectZero.MOD_ID + ":active_dungeon_slot";

    /** Spawns a Gate rift at {@code basePos} linked to a fresh slot in {@code projectzero:dungeon}. */
    public static @Nullable DungeonSavedData.DungeonRecord spawnGate(
            ServerLevel originLevel, BlockPos basePos, GateRank rank, GateSize size, boolean alongX) {
        return spawnGate(originLevel, basePos, rank, size, alongX, false);
    }

    /** As above; {@code sea} makes a sea gate (teal rift, flooded dungeon). */
    public static @Nullable DungeonSavedData.DungeonRecord spawnGate(
            ServerLevel originLevel, BlockPos basePos, GateRank rank, GateSize size, boolean alongX, boolean sea) {
        MinecraftServer server = originLevel.getServer();
        ServerLevel dungeonLevel = server.getLevel(DUNGEON);
        if (dungeonLevel == null) {
            ProjectZero.LOGGER.error("Missing projectzero:dungeon dimension!");
            return null;
        }

        DungeonSavedData data = DungeonSavedData.get(dungeonLevel);
        DungeonSavedData.DungeonRecord record = data.allocate(
                rank, size, originLevel.dimension().location(), basePos, alongX, sea);

        int pw = size.portalWidth();
        int ph = size.portalHeight();
        long now = originLevel.getGameTime();

        BlockState portalState = ModBlocks.GATE_PORTAL.get().defaultBlockState()
                .setValue(GateBlock.RANK, rank)
                .setValue(GateBlock.EXIT, false)
                .setValue(GateBlock.SEA, sea)
                .setValue(GateBlock.BROKEN, false);

        for (int w = -1; w <= pw; w++) {
            for (int h = 0; h <= ph; h++) {
                BlockPos p = alongX ? basePos.offset(w, h, 0) : basePos.offset(0, h, w);
                boolean inside = w >= 0 && w < pw && h < ph;
                if (inside) {
                    originLevel.setBlock(p, portalState, Block.UPDATE_ALL);
                    if (originLevel.getBlockEntity(p) instanceof GateBlockEntity be) {
                        be.configure(rank, size, now, record.slot, basePos);
                        be.setLastAgeCheck(now);
                    }
                } else if (!originLevel.getBlockState(p).isAir()
                        && originLevel.getBlockState(p).getDestroySpeed(originLevel, p) >= 0) {
                    originLevel.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        originLevel.playSound(null, basePos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8F, 0.9F);
        return record;
    }

    /** Spawns an ancient gate: the rift set in a ring of carved stones dressed for the biome ({@link AncientGate}). */
    public static @Nullable DungeonSavedData.DungeonRecord spawnAncientGate(
            ServerLevel level, BlockPos ground, GateRank rank, GateSize size, boolean alongX) {
        BlockPos base = AncientGate.baseFromGround(ground, size, alongX);
        DungeonSavedData.DungeonRecord rec = spawnGate(level, base, rank, size, alongX, false);
        if (rec == null) return null;
        AncientGate.build(level, base, rank, size, alongX,
                AncientGate.paletteFor(level.getBiome(base), base), level.getRandom(), true);
        return rec;
    }

    public static boolean detectAlongX(Level level, BlockPos anchor) {
        return level.getBlockState(anchor.east()).getBlock() instanceof GateBlock
                || level.getBlockState(anchor.west()).getBlock() instanceof GateBlock;
    }

    private static DungeonSavedData.DungeonRecord ensureRecord(ServerLevel originLevel, GateBlockEntity anchorBe, BlockState state) {
        ServerLevel dungeonLevel = originLevel.getServer().getLevel(DUNGEON);
        if (dungeonLevel == null) return null;
        DungeonSavedData data = DungeonSavedData.get(dungeonLevel);
        int slot = anchorBe.dungeonSlot();
        DungeonSavedData.DungeonRecord rec = slot >= 0 ? data.record(slot) : null;
        if (rec == null) {
            BlockPos a = anchorBe.anchorPos();
            boolean alongX = detectAlongX(originLevel, a);
            boolean sea = state.getBlock() instanceof GateBlock && state.getValue(GateBlock.SEA);
            rec = data.allocate(anchorBe.rank(), anchorBe.size(), originLevel.dimension().location(), a, alongX, sea);
            rec.surges = anchorBe.surges();
            data.setDirty();
            for (int w = 0; w < anchorBe.size().portalWidth(); w++) {
                for (int h = 0; h < anchorBe.size().portalHeight(); h++) {
                    BlockPos p = alongX ? a.offset(w, h, 0) : a.offset(0, h, w);
                    if (originLevel.getBlockEntity(p) instanceof GateBlockEntity other) {
                        other.configure(anchorBe.rank(), anchorBe.size(), anchorBe.openTime(), rec.slot, a);
                    }
                }
            }
        }
        return rec;
    }

    // ---------------------------------------------------------------- gate aging & dungeon breaks

    /** Called every server tick on the anchor {@link GateBlockEntity}. */
    public static void tickAnchorGate(ServerLevel level, BlockPos anchorPos, BlockState state, GateBlockEntity anchorBe) {
        if (anchorBe.isBreaking() || state.getValue(GateBlock.BROKEN)) {
            tickDungeonBreak(level, anchorPos, state, anchorBe);
            return;
        }
        long now = level.getGameTime();
        if (!level.getGameRules().getBoolean(GateRules.RULE_GATE_AGING)) {
            // Accumulate paused ticks in memory and only flush once per AGING_CHECK_INTERVAL_TICKS.
            long paused = anchorBe.recordPausedAgingStep(now);
            if (paused >= GateRules.AGING_CHECK_INTERVAL_TICKS) {
                flushPausedAgingTicks(level, anchorPos, anchorBe, now);
            }
            return;
        }
        anchorBe.markAgingActiveTick(now);
        if (anchorBe.pausedAgingTicks() > 0L) {
            flushPausedAgingTicks(level, anchorPos, anchorBe, now);
        }
        if (now - anchorBe.lastAgeCheck() < GateRules.AGING_CHECK_INTERVAL_TICKS) return;
        anchorBe.setLastAgeCheck(now);
        evaluateAging(level, anchorPos, state, anchorBe);
    }

    private static void flushPausedAgingTicks(ServerLevel level, BlockPos anchorPos, GateBlockEntity anchorBe, long now) {
        long paused = anchorBe.pausedAgingTicks();
        anchorBe.clearPausedAgingTicks();
        if (paused > 0L) {
            shiftPortalOpenTime(level, anchorPos, anchorBe, paused);
        }
        anchorBe.setLastAgeCheck(now);
    }

    /** Evaluates elapsed open time against surge and rank-up thresholds immediately. */
    public static void evaluateAging(ServerLevel level, BlockPos anchorPos, BlockState state, GateBlockEntity anchorBe) {
        if (anchorBe.isBreaking() || state.getValue(GateBlock.BROKEN)) return;
        if (!level.getGameRules().getBoolean(GateRules.RULE_GATE_AGING)) return;

        long now = level.getGameTime();
        if (anchorBe.pausedAgingTicks() > 0L) {
            flushPausedAgingTicks(level, anchorPos, anchorBe, now);
        }
        long cycleTicks = (GateRules.AGING_MAX_SURGES + 1L) * GateRules.AGING_SURGE_TICKS; // 28 days

        boolean alongX = detectAlongX(level, anchorPos);
        ServerLevel dungeonLevel = level.getServer().getLevel(DUNGEON);
        DungeonSavedData data = dungeonLevel != null ? DungeonSavedData.get(dungeonLevel) : null;

        // Process completed 28-day rank-up cycles first.
        while (now - anchorBe.openTime() >= cycleTicks) {
            GateRank curRank = anchorBe.rank();
            int nextRankUps = anchorBe.rankUps() + 1;
            boolean shouldBreak = (curRank == GateRank.S || nextRankUps >= GateRules.BREAK_AFTER_RANKUPS);

            if (shouldBreak && level.getGameRules().getBoolean(GateRules.RULE_DUNGEON_BREAKS)) {
                if (!hasPlayerWithin(level, anchorPos, GateRules.BREAK_PLAYER_RADIUS)) {
                    // Wait until a player comes within 96 blocks; hold at max surges in the meantime.
                    if (anchorBe.surges() < GateRules.AGING_MAX_SURGES) {
                        applySurge(level, anchorPos, alongX, anchorBe, GateRules.AGING_MAX_SURGES, data, dungeonLevel);
                    }
                    return;
                }
                startDungeonBreak(level, anchorPos, state, anchorBe);
                return;
            }

            long newOpenTime = anchorBe.openTime() + cycleTicks;
            if (curRank == GateRank.S) {
                // S-rank with dungeonBreaks disabled: cap at S-rank max surges.
                syncPortalAgingState(level, anchorPos, alongX, anchorBe, GateRank.S, newOpenTime,
                        GateRules.AGING_MAX_SURGES, nextRankUps, now);
                return;
            }

            GateRank nextRank = curRank.next();
            applyRankUp(level, anchorPos, alongX, anchorBe, curRank, nextRank, newOpenTime, nextRankUps, now, data, dungeonLevel);
            state = level.getBlockState(anchorPos);
            if (!(state.getBlock() instanceof GateBlock)) return;
        }

        // Process intra-cycle surges (0..3, one per 7 in-game days).
        long elapsed = Math.max(0L, now - anchorBe.openTime());
        int targetSurges = (int) Math.min(GateRules.AGING_MAX_SURGES, elapsed / GateRules.AGING_SURGE_TICKS);
        if (targetSurges > anchorBe.surges()) {
            applySurge(level, anchorPos, alongX, anchorBe, targetSurges, data, dungeonLevel);
        }
    }

    private static void applySurge(ServerLevel level, BlockPos anchorPos, boolean alongX, GateBlockEntity anchorBe,
                                   int newSurges, @Nullable DungeonSavedData data, @Nullable ServerLevel dungeonLevel) {
        GateRank rank = anchorBe.rank();
        syncPortalAgingState(level, anchorPos, alongX, anchorBe, rank, anchorBe.openTime(),
                newSurges, anchorBe.rankUps(), level.getGameTime());

        if (data != null && dungeonLevel != null && anchorBe.dungeonSlot() >= 0) {
            data.updateAging(anchorBe.dungeonSlot(), rank, newSurges);
            DungeonSavedData.DungeonRecord rec = data.record(anchorBe.dungeonSlot());
            if (rec != null && rec.built && !rec.cleared) {
                GateDungeonArena.syncMonstersInSlot(dungeonLevel, rec.slot, rank, rec.size, rec.sea, newSurges);
            }
        }

        Component msg = Component.literal("The ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(rank.name() + "-Rank gate").withStyle(rank.chatColor()))
                .append(Component.literal(" grows unstable. (Surge " + newSurges + "/" + GateRules.AGING_MAX_SURGES + ")")
                        .withStyle(ChatFormatting.WHITE));
        for (ServerPlayer p : playersWithin(level, anchorPos, GateRules.AGING_MESSAGE_RADIUS)) {
            system(p, msg);
        }
        level.playSound(null, anchorPos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.9F, 0.75F);
    }

    private static void applyRankUp(ServerLevel level, BlockPos anchorPos, boolean alongX, GateBlockEntity anchorBe,
                                    GateRank oldRank, GateRank newRank, long newOpenTime, int newRankUps, long now,
                                    @Nullable DungeonSavedData data, @Nullable ServerLevel dungeonLevel) {
        GateSize size = anchorBe.size();
        syncPortalAgingState(level, anchorPos, alongX, anchorBe, newRank, newOpenTime, 0, newRankUps, now);

        // Recolor carved runes if this is an Ancient Gate.
        AncientGate.setRank(level, anchorPos, size, alongX, newRank);

        if (data != null && dungeonLevel != null && anchorBe.dungeonSlot() >= 0) {
            data.updateAging(anchorBe.dungeonSlot(), newRank, 0);
            DungeonSavedData.DungeonRecord rec = data.record(anchorBe.dungeonSlot());
            if (rec != null && rec.built && !rec.cleared) {
                BlockPos origin = GateDungeonArena.origin(rec.slot);
                boolean someoneInside = dungeonLevel.players().stream()
                        .anyMatch(p -> Math.abs(p.getX() - origin.getX()) < GateRules.DUNGEON_SLOT_STRIDE / 2.0);
                if (someoneInside) {
                    GateDungeonArena.syncMonstersInSlot(dungeonLevel, rec.slot, newRank, rec.size, rec.sea, 0);
                    GateDungeonArena.spawnRankUpReinforcements(dungeonLevel, rec.slot, newRank, rec.size, rec.sea, 0);
                } else {
                    rec.pendingReinforcements++;
                    data.setDirty();
                }
            }
        }

        Component title = Component.literal("GATE RANK UP")
                .withStyle(newRank.chatColor(), ChatFormatting.BOLD);
        Component sub = Component.literal(oldRank.name() + "-Rank → " + newRank.name() + "-Rank Gate")
                .withStyle(ChatFormatting.GRAY);
        Component chat = Component.literal("An uncleared ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(oldRank.name() + "-Rank Gate").withStyle(oldRank.chatColor()))
                .append(Component.literal(" has escalated to ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(newRank.name() + "-Rank").withStyle(newRank.chatColor(), ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.WHITE));

        for (ServerPlayer p : playersWithin(level, anchorPos, GateRules.AGING_MESSAGE_RADIUS)) {
            p.connection.send(new ClientboundSetTitleTextPacket(title));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            system(p, chat);
        }
        level.playSound(null, anchorPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    private static void syncPortalAgingState(ServerLevel level, BlockPos anchorPos, boolean alongX,
                                             GateBlockEntity anchorBe, GateRank rank, long openTime,
                                             int surges, int rankUps, long lastCheck) {
        GateSize size = anchorBe.size();
        for (int w = 0; w < size.portalWidth(); w++) {
            for (int h = 0; h < size.portalHeight(); h++) {
                BlockPos p = alongX ? anchorPos.offset(w, h, 0) : anchorPos.offset(0, h, w);
                BlockState oldState = level.getBlockState(p);
                if (!(oldState.getBlock() instanceof GateBlock)) continue;
                BlockState newState = oldState.setValue(GateBlock.RANK, rank);
                if (newState != oldState) {
                    level.setBlock(p, newState, Block.UPDATE_ALL);
                }
                if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                    be.setAgingState(rank, openTime, surges, rankUps, lastCheck);
                }
                level.sendBlockUpdated(p, oldState, newState, Block.UPDATE_ALL);
            }
        }
    }

    public static void shiftPortalOpenTime(ServerLevel level, BlockPos anchorPos, GateBlockEntity anchorBe, long deltaTicks) {
        boolean alongX = detectAlongX(level, anchorPos);
        GateSize size = anchorBe.size();
        for (int w = 0; w < size.portalWidth(); w++) {
            for (int h = 0; h < size.portalHeight(); h++) {
                BlockPos p = alongX ? anchorPos.offset(w, h, 0) : anchorPos.offset(0, h, w);
                if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                    be.shiftOpenTime(deltaTicks);
                }
            }
        }
    }

    /** Starts a Dungeon Break immediately on the gate anchored at {@code anchorPos}. */
    public static boolean startDungeonBreak(ServerLevel level, BlockPos anchorPos, BlockState state, GateBlockEntity anchorBe) {
        if (anchorBe.isBreaking()) return false;
        DungeonSavedData.DungeonRecord rec = ensureRecord(level, anchorBe, state);
        if (rec == null || rec.cleared) return false;

        ServerLevel dungeonLevel = level.getServer().getLevel(DUNGEON);
        if (dungeonLevel != null) {
            DungeonSavedData data = DungeonSavedData.get(dungeonLevel);
            data.markBroken(rec.slot);
            if (rec.built) {
                GateDungeonArena.clearDungeonMonsters(dungeonLevel, rec.slot, rec.size, rec.sea);
                BlockPos origin = GateDungeonArena.origin(rec.slot);
                for (ServerPlayer p : List.copyOf(dungeonLevel.players())) {
                    if (Math.abs(p.getX() - origin.getX()) < GateRules.DUNGEON_SLOT_STRIDE / 2.0) {
                        returnFromDungeon(p);
                    }
                }
            }
        }

        long now = level.getGameTime();
        boolean alongX = rec.alongX;
        GateSize size = anchorBe.size();
        for (int w = 0; w < size.portalWidth(); w++) {
            for (int h = 0; h < size.portalHeight(); h++) {
                BlockPos p = alongX ? anchorPos.offset(w, h, 0) : anchorPos.offset(0, h, w);
                BlockState oldSt = level.getBlockState(p);
                if (oldSt.getBlock() instanceof GateBlock) {
                    BlockState brokenSt = oldSt.setValue(GateBlock.BROKEN, true);
                    level.setBlock(p, brokenSt, Block.UPDATE_ALL);
                    if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                        be.setBreakState(true, 0, now);
                    }
                    level.sendBlockUpdated(p, oldSt, brokenSt, Block.UPDATE_ALL);
                }
            }
        }

        Component title = Component.literal("DUNGEON BREAK").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component sub = Component.literal("Monsters are pouring out of the " + anchorBe.rank().name() + "-Rank Gate!")
                .withStyle(ChatFormatting.GOLD);
        Component chat = Component.literal("Emergency! A ")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal(anchorBe.rank().name() + "-Rank Gate").withStyle(anchorBe.rank().chatColor(), ChatFormatting.BOLD))
                .append(Component.literal(" has suffered a Dungeon Break!").withStyle(ChatFormatting.RED));

        for (ServerPlayer p : playersWithin(level, anchorPos, GateRules.AGING_MESSAGE_RADIUS)) {
            p.connection.send(new ClientboundSetTitleTextPacket(title));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            system(p, chat);
        }
        level.playSound(null, anchorPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.9F, 0.8F);

        // Spawn Wave 1 right away.
        tickDungeonBreak(level, anchorPos, level.getBlockState(anchorPos), anchorBe);
        return true;
    }

    private static void tickDungeonBreak(ServerLevel level, BlockPos anchorPos, BlockState state, GateBlockEntity anchorBe) {
        long now = level.getGameTime();
        if (!anchorBe.isBreaking()) {
            anchorBe.setBreakState(true, anchorBe.breakWave(), now);
        }
        int wave = anchorBe.breakWave();
        if (wave >= GateRules.BREAK_WAVES) {
            checkMissingBreakBoss(level, anchorPos, state, anchorBe, now);
            return;
        }

        // Pause the wave timer while no player is within BREAK_PLAYER_RADIUS.
        if (!hasPlayerWithin(level, anchorPos, GateRules.BREAK_PLAYER_RADIUS)) {
            if (anchorBe.pausedBreakTicks() == 0L && now > anchorBe.nextWaveTime()) {
                pushPortalNextWaveTime(level, anchorPos, anchorBe, now);
            }
            long paused = anchorBe.recordPausedBreakStep(now);
            if (paused >= GateRules.AGING_CHECK_INTERVAL_TICKS) {
                anchorBe.clearPausedBreakTicks();
                pushPortalNextWaveTime(level, anchorPos, anchorBe, anchorBe.nextWaveTime() + paused);
            }
            return;
        }

        anchorBe.markBreakActiveTick(now);
        if (anchorBe.pausedBreakTicks() > 0L) {
            long pushed = anchorBe.nextWaveTime() + anchorBe.pausedBreakTicks();
            anchorBe.clearPausedBreakTicks();
            pushPortalNextWaveTime(level, anchorPos, anchorBe, pushed);
        }
        if (now < anchorBe.nextWaveTime()) return;

        DungeonSavedData.DungeonRecord rec = ensureRecord(level, anchorBe, state);
        if (rec == null || rec.cleared) return;

        int nextWave = wave + 1;
        long nextTime = now + GateRules.BREAK_WAVE_TICKS;
        boolean alongX = rec.alongX;
        GateSize size = anchorBe.size();
        for (int w = 0; w < size.portalWidth(); w++) {
            for (int h = 0; h < size.portalHeight(); h++) {
                BlockPos p = alongX ? anchorPos.offset(w, h, 0) : anchorPos.offset(0, h, w);
                if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                    be.setBreakState(true, nextWave, nextTime);
                }
            }
        }

        spawnBreakWave(level, anchorPos, rec, anchorBe.rank(), size, anchorBe.surges(),
                state.getValue(GateBlock.SEA), nextWave);
    }

    private static void pushPortalNextWaveTime(ServerLevel level, BlockPos anchorPos, GateBlockEntity anchorBe, long nextWaveTime) {
        boolean alongX = detectAlongX(level, anchorPos);
        GateSize size = anchorBe.size();
        for (int w = 0; w < size.portalWidth(); w++) {
            for (int h = 0; h < size.portalHeight(); h++) {
                BlockPos p = alongX ? anchorPos.offset(w, h, 0) : anchorPos.offset(0, h, w);
                if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                    be.setNextWaveTime(nextWaveTime);
                }
            }
        }
    }

    private static void checkMissingBreakBoss(ServerLevel level, BlockPos anchorPos, BlockState state,
                                              GateBlockEntity anchorBe, long now) {
        if (anchorBe.bossRespawned()) return;
        if (!hasPlayerWithin(level, anchorPos, GateRules.BREAK_PLAYER_RADIUS)) {
            anchorBe.resetBossCheckClock(now);
            return;
        }
        long elapsed = anchorBe.consumeBossCheckElapsed(now, 20L);
        if (elapsed <= 0L) return;

        DungeonSavedData.DungeonRecord rec = ensureRecord(level, anchorBe, state);
        if (rec == null || rec.cleared) return;

        if (hasLivingSlotBossWithin(level, anchorPos, rec.slot, GateRules.BREAK_BOSS_CHECK_RADIUS)) {
            anchorBe.resetMissingBossTicks();
            return;
        }

        long missing = anchorBe.addMissingBossTicks(elapsed);
        if (missing < GateRules.BREAK_BOSS_MISSING_TICKS) return;

        anchorBe.resetMissingBossTicks();
        GateSize size = anchorBe.size();
        for (int w = 0; w < size.portalWidth(); w++) {
            for (int h = 0; h < size.portalHeight(); h++) {
                BlockPos p = rec.alongX ? anchorPos.offset(w, h, 0) : anchorPos.offset(0, h, w);
                if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                    be.setBossRespawned(true);
                }
            }
        }

        Vec3 bossPos = breakSpawnSpot(level, anchorPos, level.getRandom());
        Mob boss = GateDungeonArena.createGateBoss(level, rec.slot, anchorBe.rank(),
                state.getValue(GateBlock.SEA), bossPos);
        if (boss != null) {
            ServerPlayer nearestTarget = level.players().stream()
                    .filter(p -> !p.isSpectator() && p.isAlive()
                            && p.blockPosition().closerThan(anchorPos, GateRules.AGING_MESSAGE_RADIUS))
                    .min(Comparator.comparingDouble(p -> p.distanceToSqr(Vec3.atCenterOf(anchorPos))))
                    .orElse(null);
            if (nearestTarget != null) boss.setTarget(nearestTarget);
            level.addFreshEntity(boss);
            ProjectZero.LOGGER.warn(
                    "Dungeon Break boss for slot {} ({}-Rank gate at {}) was missing for 60s; respawned boss once at {}",
                    rec.slot, anchorBe.rank(), anchorPos, boss.blockPosition());
        }
    }

    private static boolean hasLivingSlotBossWithin(ServerLevel level, BlockPos anchorPos, int slot, double radius) {
        Vec3 center = Vec3.atCenterOf(anchorPos);
        double r2 = radius * radius;
        AABB box = new AABB(center, center).inflate(radius);
        return !level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive()
                        && e.position().distanceToSqr(center) <= r2
                        && e.getPersistentData().getBoolean(MobScaling.GATE_BOSS_KEY)
                        && e.getPersistentData().contains(GateDungeonArena.DUNGEON_SLOT_KEY)
                        && e.getPersistentData().getInt(GateDungeonArena.DUNGEON_SLOT_KEY) == slot
        ).isEmpty();
    }

    private static void spawnBreakWave(ServerLevel level, BlockPos anchorPos, DungeonSavedData.DungeonRecord rec,
                                       GateRank rank, GateSize size, int surges, boolean sea, int waveNumber) {
        RandomSource random = level.getRandom();
        ServerPlayer nearestTarget = level.players().stream()
                .filter(p -> !p.isSpectator() && p.isAlive()
                        && p.blockPosition().closerThan(anchorPos, GateRules.AGING_MESSAGE_RADIUS))
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(Vec3.atCenterOf(anchorPos))))
                .orElse(null);

        int count = size.mobCount() / 2 + 2;
        for (int i = 0; i < count; i++) {
            Vec3 pos = breakSpawnSpot(level, anchorPos, random);
            Mob mob = GateDungeonArena.createGateMonster(level, rec.slot, rank, surges, sea, pos, random);
            if (mob != null) {
                if (nearestTarget != null) mob.setTarget(nearestTarget);
                level.addFreshEntity(mob);
            }
        }

        boolean finalWave = (waveNumber >= GateRules.BREAK_WAVES);
        if (finalWave) {
            Vec3 bossPos = breakSpawnSpot(level, anchorPos, random);
            Mob boss = GateDungeonArena.createGateBoss(level, rec.slot, rank, sea, bossPos);
            if (boss != null) {
                if (nearestTarget != null) boss.setTarget(nearestTarget);
                level.addFreshEntity(boss);
            }
        }

        Component waveMsg = finalWave
                ? Component.literal("Dungeon Break Wave " + waveNumber + "/" + GateRules.BREAK_WAVES
                        + ": The Dungeon Boss has emerged from the rift!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                : Component.literal("Dungeon Break Wave " + waveNumber + "/" + GateRules.BREAK_WAVES + " emerging!")
                        .withStyle(ChatFormatting.GOLD);
        for (ServerPlayer p : playersWithin(level, anchorPos, GateRules.AGING_MESSAGE_RADIUS)) {
            system(p, waveMsg);
        }
        level.playSound(null, anchorPos, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.2F, 0.85F);
    }

    private static Vec3 breakSpawnSpot(ServerLevel level, BlockPos anchorPos, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 1.8 + random.nextDouble() * 4.2; // within 6 blocks of the rift
        int x = (int) Math.round(anchorPos.getX() + 0.5 + Math.cos(angle) * dist);
        int z = (int) Math.round(anchorPos.getZ() + 0.5 + Math.sin(angle) * dist);
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (Math.abs(topY - anchorPos.getY()) > 10) {
            topY = anchorPos.getY();
        }
        return new Vec3(x + 0.5, topY, z + 0.5);
    }

    private static boolean hasPlayerWithin(ServerLevel level, BlockPos pos, double radius) {
        return !playersWithin(level, pos, radius).isEmpty();
    }

    private static List<ServerPlayer> playersWithin(ServerLevel level, BlockPos pos, double radius) {
        Vec3 c = Vec3.atCenterOf(pos);
        double r2 = radius * radius;
        return level.players().stream()
                .filter(p -> !p.isSpectator() && p.position().distanceToSqr(c) <= r2)
                .toList();
    }

    // ---------------------------------------------------------------- portal entry & clear flow

    public static void onEnterPortal(ServerPlayer player, BlockPos pos, BlockState state) {
        long now = player.serverLevel().getGameTime();
        long until = player.getPersistentData().getLong(COOLDOWN_KEY);
        if (now < until) return;
        player.getPersistentData().putLong(COOLDOWN_KEY, now + GateRules.PORTAL_COOLDOWN_TICKS);

        if (state.getValue(GateBlock.EXIT)) {
            returnFromDungeon(player);
            return;
        }

        if (state.getValue(GateBlock.BROKEN)) {
            system(player, Component.literal("Dungeon break in progress! The rift cannot be entered.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        if (!(player.level().getBlockEntity(pos) instanceof GateBlockEntity touchedBe)) return;
        BlockPos anchorPos = touchedBe.anchorPos();
        GateBlockEntity anchorBe = player.level().getBlockEntity(anchorPos) instanceof GateBlockEntity abe ? abe : touchedBe;
        if (anchorBe.isBreaking()) {
            system(player, Component.literal("Dungeon break in progress! The rift cannot be entered.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel dungeonLevel = server.getLevel(DUNGEON);
        if (dungeonLevel == null) return;

        DungeonSavedData data = DungeonSavedData.get(dungeonLevel);
        DungeonSavedData.DungeonRecord rec = ensureRecord(player.serverLevel(), anchorBe, state);
        if (rec == null) return;

        if (rec.cleared) {
            system(player, Component.literal("This gate has already collapsed.").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (rec.broken) {
            system(player, Component.literal("Dungeon break in progress! The rift cannot be entered.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Sync record rank/surges with the gate's live state.
        if (rec.rank != anchorBe.rank() || rec.surges != anchorBe.surges()) {
            data.updateAging(rec.slot, anchorBe.rank(), anchorBe.surges());
        }

        if (!rec.built) {
            GateDungeonArena.buildAndPopulate(dungeonLevel, rec.slot, rec.rank, rec.size, rec.sea, rec.surges);
            rec.pendingReinforcements = 0;
            data.markBuilt(rec.slot);
        }

        saveReturnPoint(player);
        player.getPersistentData().putInt(ACTIVE_SLOT_KEY, rec.slot);

        Vec3 target = GateDungeonArena.entranceSpawn(rec.slot);
        player.teleportTo(dungeonLevel, target.x, target.y, target.z, 0.0F, 0.0F);
        player.getPersistentData().putLong(COOLDOWN_KEY, dungeonLevel.getGameTime() + GateRules.PORTAL_COOLDOWN_TICKS);

        if (rec.built) {
            GateDungeonArena.syncMonstersInSlot(dungeonLevel, rec.slot, rec.rank, rec.size, rec.sea, rec.surges);
            while (rec.pendingReinforcements > 0) {
                GateDungeonArena.spawnRankUpReinforcements(dungeonLevel, rec.slot, rec.rank, rec.size, rec.sea, rec.surges);
                rec.pendingReinforcements--;
                data.setDirty();
            }
        }

        if (rec.sea) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WATER_BREATHING, GateRules.SEA_GATE_BREATHING_TICKS, 0, false, true));
            system(player, Component.literal("Sea gate. The System grants you water breathing for "
                    + GateRules.SEA_GATE_BREATHING_TICKS / 20 + " seconds.").withStyle(ChatFormatting.WHITE));
        }
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(rec.rank.name() + "-RANK GATE").withStyle(rec.rank.chatColor(), ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("Slay the dungeon boss to open the exit").withStyle(ChatFormatting.GRAY)));
        system(player, Component.literal("You have entered a ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(rec.rank.name() + "-Rank (" + rec.size.name() + ") Gate")
                        .withStyle(rec.rank.chatColor()))
                .append(Component.literal(".").withStyle(ChatFormatting.WHITE)));
    }

    public static void onDungeonBossKilled(ServerLevel level, LivingEntity boss, @Nullable ServerPlayer killer) {
        if (!boss.getPersistentData().getBoolean(MobScaling.GATE_BOSS_KEY)) return;
        if (!boss.getPersistentData().contains(GateDungeonArena.DUNGEON_SLOT_KEY)) return;

        int slot = boss.getPersistentData().getInt(GateDungeonArena.DUNGEON_SLOT_KEY);
        MinecraftServer server = level.getServer();
        ServerLevel dungeonLevel = server.getLevel(DUNGEON);
        if (dungeonLevel == null) return;

        DungeonSavedData data = DungeonSavedData.get(dungeonLevel);
        DungeonSavedData.DungeonRecord rec = data.record(slot);
        if (rec == null || rec.cleared) return;

        data.markCleared(slot);
        if (!rec.broken && rec.built) {
            GateDungeonArena.openExitPortal(dungeonLevel, slot, rec.size, rec.rank, rec.sea);
        }

        // Guaranteed Awakening Shard drop from Gate Boss (DESIGN-DOCUMENT.md §6)
        ItemEntity shardDrop = new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(),
                new ItemStack(ModItems.AWAKENING_SHARD.get()));
        shardDrop.setDefaultPickUpDelay();
        level.addFreshEntity(shardDrop);

        // Seal/close the Overworld Gate
        closeOverworldGate(server, rec);

        // Notify and credit all players inside this dungeon slot (or nearby in the overworld for a Dungeon Break)
        java.util.Set<java.util.UUID> credited = new java.util.HashSet<>();
        BlockPos origin = GateDungeonArena.origin(slot);
        for (ServerPlayer p : dungeonLevel.players()) {
            if (Math.abs(p.getX() - origin.getX()) < GateRules.DUNGEON_SLOT_STRIDE / 2.0) {
                MilestoneTracker.recordGateCleared(p);
                credited.add(p.getUUID());
                system(p, Component.literal("Dungeon boss defeated! The exit gate has opened and the outer Gate has sealed.")
                        .withStyle(ChatFormatting.GOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        if (rec.broken) {
            ServerLevel originLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, rec.originDim));
            if (originLevel != null) {
                for (ServerPlayer p : playersWithin(originLevel, rec.gateAnchor, GateRules.AGING_MESSAGE_RADIUS)) {
                    if (credited.add(p.getUUID())) {
                        MilestoneTracker.recordGateCleared(p);
                    }
                    system(p, Component.literal("Dungeon Break contained! The " + rec.rank.name() + "-Rank Gate has collapsed.")
                            .withStyle(ChatFormatting.GOLD));
                    p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
        if (killer != null && credited.add(killer.getUUID())) {
            MilestoneTracker.recordGateCleared(killer);
        }
    }

    private static void closeOverworldGate(MinecraftServer server, DungeonSavedData.DungeonRecord rec) {
        ServerLevel originLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, rec.originDim));
        if (originLevel == null) return;

        int pw = rec.size.portalWidth();
        int ph = rec.size.portalHeight();
        for (int w = -1; w <= pw; w++) {
            for (int h = -1; h <= ph; h++) {
                BlockPos p = rec.alongX ? rec.gateAnchor.offset(w, h, 0) : rec.gateAnchor.offset(0, h, w);
                BlockState st = originLevel.getBlockState(p);
                if (st.is(ModBlocks.GATE_PORTAL.get()) || st.is(Blocks.OBSIDIAN) || st.is(Blocks.CRYING_OBSIDIAN)) {
                    originLevel.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        // Ancient gates leave their ruined stone ring behind (dark runes, fallen stones, a seal).
        AncientGate.ruin(originLevel, rec.gateAnchor, rec.size, rec.alongX);
        originLevel.sendParticles(Fx.mote(rec.rank.argb() & 0xFFFFFF, 1.6F),
                rec.gateAnchor.getX() + 1.0, rec.gateAnchor.getY() + 1.5, rec.gateAnchor.getZ() + 0.5,
                40, 1.0, 1.2, 1.0, 0.08);
    }

    public static void returnFromDungeon(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        CompoundTag back = player.getPersistentData().getCompound(RETURN_KEY);
        ServerLevel target = null;
        if (back.contains("dim")) {
            ResourceLocation dim = ResourceLocation.tryParse(back.getString("dim"));
            if (dim != null) target = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        }
        if (target == null) {
            target = server.overworld();
            BlockPos sp = target.getSharedSpawnPos();
            player.teleportTo(target, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0.0F, 0.0F);
        } else {
            player.teleportTo(target, back.getDouble("x"), back.getDouble("y"), back.getDouble("z"),
                    back.getFloat("yaw"), back.getFloat("pitch"));
        }
        player.getPersistentData().remove(RETURN_KEY);
        player.getPersistentData().remove(ACTIVE_SLOT_KEY);
        player.getPersistentData().putLong(COOLDOWN_KEY, target.getGameTime() + GateRules.PORTAL_COOLDOWN_TICKS);
    }

    private static void saveReturnPoint(ServerPlayer player) {
        if (player.level().dimension().equals(DUNGEON)) return;
        Vec3 backPos = player.position().subtract(player.getLookAngle().normalize().scale(1.5));
        CompoundTag back = new CompoundTag();
        back.putString("dim", player.level().dimension().location().toString());
        back.putDouble("x", backPos.x);
        back.putDouble("y", player.getY());
        back.putDouble("z", backPos.z);
        back.putFloat("yaw", player.getYRot());
        back.putFloat("pitch", player.getXRot());
        player.getPersistentData().put(RETURN_KEY, back);
    }

    /** Periodic natural Gate spawning near overworld players when {@code projectzero:gateSpawning} is true. */
    public static void tickNaturalSpawning(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        if (level.getGameTime() % GateRules.SPAWN_CHECK_INTERVAL_TICKS != 0) return;
        if (!level.getGameRules().getBoolean(GateRules.RULE_GATE_SPAWNING)) return;

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        RandomSource random = level.getRandom();
        if (random.nextDouble() >= GateRules.SPAWN_CHANCE_PER_CHECK) return;

        ServerPlayer anchor = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = GateRules.SPAWN_MIN_DISTANCE
                + random.nextDouble() * (GateRules.SPAWN_MAX_DISTANCE - GateRules.SPAWN_MIN_DISTANCE);
        int gx = (int) Math.round(anchor.getX() + Math.cos(angle) * dist);
        int gz = (int) Math.round(anchor.getZ() + Math.sin(angle) * dist);
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz) + 1;
        boolean sea = level.getBiome(new BlockPos(gx, gy, gz)).is(net.minecraft.tags.BiomeTags.IS_OCEAN)
                && level.getFluidState(new BlockPos(gx, gy - 1, gz)).is(net.minecraft.tags.FluidTags.WATER);
        if (sea) gy += 1;   // hang just above the waves
        if (gy <= level.getMinBuildHeight() + 2 || gy >= level.getMaxBuildHeight() - 10) return;

        GateRank rank = GateRules.randomRank(random);
        GateSize size = GateRules.randomSize(random);
        BlockPos pos = new BlockPos(gx, gy, gz);
        boolean ancient = !sea && random.nextDouble() < GateRules.ANCIENT_NATURAL_SHARE;
        boolean alongX = random.nextBoolean();
        var rec = ancient ? spawnAncientGate(level, pos.below(), rank, size, alongX)
                : spawnGate(level, pos, rank, size, alongX, sea);
        if (rec != null) {
            String kind = sea ? "Sea Gate" : ancient ? "Ancient Gate" : "Gate";
            system(anchor, Component.literal("A ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(rank.name() + "-Rank " + kind).withStyle(rank.chatColor()))
                    .append(Component.literal(" has opened nearby!").withStyle(ChatFormatting.WHITE)));
        }
    }

    public static void system(ServerPlayer player, Component msg) {
        player.sendSystemMessage(Component.literal("[System] ").withStyle(ChatFormatting.AQUA).append(msg));
    }
}
