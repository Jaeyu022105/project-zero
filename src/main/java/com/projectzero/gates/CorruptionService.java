package com.projectzero.gates;

import com.projectzero.ProjectZero;
import com.projectzero.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runtime manager for Dungeon Break corruption spread and receding cleanup (DESIGN-DOCUMENT.md §9).
 *
 * <p>Spreads step-by-step per wave up to {@link GateRules#corruptionRadius(GateRank)}, converting
 * only natural terrain in {@code #projectzero:corruptible} inside loaded chunks while a player is
 * within {@link GateRules#BREAK_PLAYER_RADIUS}. When the break is contained, original blocks
 * return outer-ring-first over {@link GateRules#CORRUPTION_RECEDE_TICKS}.
 */
public final class CorruptionService {
    private CorruptionService() {}

    public static final TagKey<Block> CORRUPTIBLE =
            TagKey.create(Registries.BLOCK, ProjectZero.id("corruptible"));

    /** Advances target corruption radius for wave {@code waveNumber} (1..BREAK_WAVES). */
    public static void onBreakWave(ServerLevel level, BlockPos anchorPos, int slot, GateRank rank,
                                   boolean sea, int waveNumber) {
        if (!level.getGameRules().getBoolean(GateRules.RULE_BREAK_CORRUPTION)) return;
        int maxR = GateRules.corruptionRadius(rank);
        int targetR = Math.max(3, Mth.clamp(
                (int) Math.round(maxR * (waveNumber / (double) GateRules.BREAK_WAVES)), 3, maxR));
        CorruptionSavedData data = CorruptionSavedData.get(level);
        data.getOrCreate(anchorPos, slot, rank, sea, targetR);
        // Apply an immediate burst on wave spawn so players see the pulse right away.
        CorruptionSavedData.CorruptionZone zone = data.zone(anchorPos);
        if (zone != null) {
            spreadStep(level, data, zone, GateRules.CORRUPTION_BLOCKS_PER_STEP * 3);
        }
    }

    /** Debug {@code /pz gate corruption grow}: expands radius by one wave step and spreads immediately. */
    public static int growNow(ServerLevel level, BlockPos anchorPos, int slot, GateRank rank, boolean sea) {
        int maxR = GateRules.corruptionRadius(rank);
        int stepR = Math.max(3, (int) Math.ceil(maxR / (double) GateRules.BREAK_WAVES));
        CorruptionSavedData data = CorruptionSavedData.get(level);
        CorruptionSavedData.CorruptionZone existing = data.zone(anchorPos);
        int nextR = existing == null ? stepR : Math.min(maxR, existing.targetRadius + stepR);
        CorruptionSavedData.CorruptionZone zone = data.getOrCreate(anchorPos, slot, rank, sea, nextR);
        return spreadStep(level, data, zone, 240);
    }

    /** Starts the ~30s outer-ring-first receding cleanup after a Dungeon Break is contained. */
    public static void startRecede(ServerLevel level, BlockPos anchorPos) {
        CorruptionSavedData data = CorruptionSavedData.get(level);
        CorruptionSavedData.CorruptionZone zone = data.zone(anchorPos);
        if (zone == null) return;
        if (zone.originalBlocks.isEmpty()) {
            data.removeZone(anchorPos);
            return;
        }
        zone.receding = true;
        zone.recedeTicksRemaining = GateRules.CORRUPTION_RECEDE_TICKS;
        data.setDirty();
    }

    /** Removes any recorded positions that {@link AncientGate#ruin} just wrote so ancient gate ruins stay. */
    public static void dropPositions(ServerLevel level, Iterable<BlockPos> keepAsRuins) {
        CorruptionSavedData data = CorruptionSavedData.get(level);
        boolean changed = false;
        for (CorruptionSavedData.CorruptionZone zone : data.zones()) {
            for (BlockPos p : keepAsRuins) {
                if (zone.originalBlocks.remove(p) != null) changed = true;
            }
        }
        if (changed) data.setDirty();
    }

    /** Debug {@code /pz gate corruption clear}: immediately restores all corrupted blocks for {@code anchorPos}. */
    public static int clearImmediate(ServerLevel level, BlockPos anchorPos) {
        CorruptionSavedData data = CorruptionSavedData.get(level);
        CorruptionSavedData.CorruptionZone zone = data.zone(anchorPos);
        if (zone == null) return 0;
        int restored = restoreBatch(level, zone, Integer.MAX_VALUE);
        if (zone.originalBlocks.isEmpty()) {
            data.removeZone(anchorPos);
        } else {
            data.setDirty();
        }
        return restored;
    }

    /** Called every server tick per dimension from {@link MobEvents#onLevelTick}. */
    public static void tickLevel(ServerLevel level) {
        CorruptionSavedData data = CorruptionSavedData.get(level);
        if (data.zones().isEmpty()) return;

        long now = level.getGameTime();
        List<BlockPos> finished = new ArrayList<>();

        for (CorruptionSavedData.CorruptionZone zone : new ArrayList<>(data.zones())) {
            if (zone.receding) {
                if (zone.originalBlocks.isEmpty()) {
                    finished.add(zone.anchor);
                    continue;
                }
                int remBlocks = zone.originalBlocks.size();
                int remTicks = Math.max(1, zone.recedeTicksRemaining);
                int batch = Math.max(1, (int) Math.ceil(remBlocks / (double) remTicks));
                int restored = restoreBatch(level, zone, batch);
                zone.recedeTicksRemaining = Math.max(1, zone.recedeTicksRemaining - 1);
                if (restored > 0) data.setDirty();
                if (zone.originalBlocks.isEmpty()) {
                    finished.add(zone.anchor);
                }
            } else {
                if (now % GateRules.CORRUPTION_SPREAD_INTERVAL_TICKS != 0L) continue;
                if (!level.getGameRules().getBoolean(GateRules.RULE_BREAK_CORRUPTION)) continue;
                if (!level.isLoaded(zone.anchor)) continue;
                if (!GateService.hasPlayerNear(level, zone.anchor, GateRules.BREAK_PLAYER_RADIUS)) continue;
                spreadStep(level, data, zone, GateRules.CORRUPTION_BLOCKS_PER_STEP);
            }
        }

        for (BlockPos done : finished) {
            data.removeZone(done);
        }
    }

    private static int spreadStep(ServerLevel level, CorruptionSavedData data,
                                  CorruptionSavedData.CorruptionZone zone, int maxConvert) {
        int r = Math.min(zone.targetRadius, GateRules.corruptionRadius(zone.rank));
        if (r <= 0 || maxConvert <= 0) return 0;

        GateDungeonArena.CorruptionPalette palette = GateDungeonArena.themePaletteFor(zone.slot, zone.rank, zone.sea);
        RandomSource random = level.getRandom();
        int converted = 0;
        int r2 = r * r;
        int ySpan = Math.min(10, Math.max(5, r / 2 + 2));

        // Sample candidate columns outward from the rift so inner rings fill first, then outer rings.
        int attempts = maxConvert * 14;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < attempts && converted < maxConvert; i++) {
            double frac = Math.sqrt(random.nextDouble());
            double angle = random.nextDouble() * Math.PI * 2.0;
            int dx = (int) Math.round(Math.cos(angle) * frac * r);
            int dz = (int) Math.round(Math.sin(angle) * frac * r);
            if (dx * dx + dz * dz > r2) continue;

            int x = zone.anchor.getX() + dx;
            int z = zone.anchor.getZ() + dz;
            mpos.set(x, zone.anchor.getY(), z);
            if (!level.isLoaded(mpos)) continue;

            int dy = random.nextInt(ySpan * 2 + 1) - ySpan;
            BlockPos pos = new BlockPos(x, zone.anchor.getY() + dy, z);
            if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight() - 1) continue;
            if (zone.originalBlocks.containsKey(pos)) continue;

            BlockState cur = level.getBlockState(pos);
            if (!canCorrupt(level, pos, cur)) continue;

            // Prefer exposed blocks (air or water or leaves neighbor) so visible surfaces convert cleanly.
            if (!isExposed(level, pos)) continue;

            BlockState replacement = pickCorruptedBlock(level, pos, cur, palette, random);
            if (replacement == cur) continue;

            zone.originalBlocks.put(pos, cur);
            level.setBlock(pos, replacement, Block.UPDATE_CLIENTS);
            converted++;
        }

        if (converted > 0) {
            data.setDirty();
        }
        return converted;
    }

    public static boolean canCorrupt(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (!state.is(CORRUPTIBLE)) return false;
        if (state.hasBlockEntity() || state.getBlock() instanceof EntityBlock || state.getBlock() instanceof Container) {
            return false;
        }
        if (level.getBlockEntity(pos) != null) return false;
        // Skip player-placed persistent leaves; only natural tree foliage converts.
        if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
            return false;
        }
        return true;
    }

    private static boolean isExposed(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos adj = pos.relative(d);
            if (!level.isLoaded(adj)) continue;
            BlockState st = level.getBlockState(adj);
            if (st.isAir() || !st.isSolidRender(level, adj) || !st.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static BlockState pickCorruptedBlock(ServerLevel level, BlockPos pos, BlockState current,
                                                 GateDungeonArena.CorruptionPalette palette, RandomSource random) {
        long hash = Mth.getSeed(pos.getX(), pos.getY(), pos.getZ());
        int roll = Math.floorMod((int) (hash ^ (hash >>> 16)), 100);

        if (palette.sea() && (level.getFluidState(pos.above()).is(FluidTags.WATER)
                || level.getFluidState(pos).is(FluidTags.WATER))) {
            return switch (roll % 4) {
                case 0 -> Blocks.PRISMARINE.defaultBlockState();
                case 1 -> Blocks.DARK_PRISMARINE.defaultBlockState();
                case 2 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                default -> Blocks.PRISMARINE_BRICKS.defaultBlockState();
            };
        }

        if (current.is(BlockTags.LEAVES)) {
            if (roll < 8) {
                return ModBlocks.MANA_CRYSTAL.get().defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING, Direction.UP)
                        .setValue(AmethystClusterBlock.WATERLOGGED, false);
            }
            BlockState[] wallPool = palette.subsurface();
            return wallPool[Math.floorMod(roll, wallPool.length)];
        }

        if (current.is(BlockTags.LOGS)) {
            BlockState[] ruinPool = palette.accents();
            return ruinPool[Math.floorMod(roll, ruinPool.length)];
        }

        boolean surface = level.getBlockState(pos.above()).isAir();
        if (surface && roll < 4) {
            return palette.lightBlock();
        }
        BlockState[] pool = surface ? palette.ground() : palette.subsurface();
        return pool[Math.floorMod(roll, pool.length)];
    }

    private static int restoreBatch(ServerLevel level, CorruptionSavedData.CorruptionZone zone, int maxCount) {
        if (zone.originalBlocks.isEmpty() || maxCount <= 0) return 0;

        // Sort outer-ring first (largest horizontal/3D distance from gate anchor first).
        List<BlockPos> ordered = new ArrayList<>(zone.originalBlocks.keySet());
        ordered.sort(Comparator.comparingDouble((BlockPos p) -> p.distSqr(zone.anchor)).reversed());

        int restored = 0;
        for (BlockPos pos : ordered) {
            if (restored >= maxCount) break;
            if (!level.isLoaded(pos)) continue;

            BlockState original = zone.originalBlocks.remove(pos);
            if (original == null) continue;

            BlockState cur = level.getBlockState(pos);
            // Keep ancient gate stones/ruins intact and never overwrite block entities/containers placed later.
            if (!cur.is(ModBlocks.ANCIENT_GATE_STONE.get())
                    && !cur.is(ModBlocks.GATE_PORTAL.get())
                    && !cur.hasBlockEntity()
                    && level.getBlockEntity(pos) == null) {
                level.setBlock(pos, original, Block.UPDATE_CLIENTS);
            }
            restored++;
        }
        return restored;
    }
}
