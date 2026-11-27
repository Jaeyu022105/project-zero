package com.projectzero.gates;

import com.projectzero.gates.AncientGateStoneBlock.Material;
import com.projectzero.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ancient gates (DESIGN-DOCUMENT.md §9 "Ancient gates"): a rift set in a ring of carved stones on a
 * stepped dais, dressed for its biome. They generate rarely with the terrain, make up a share of
 * natural gate spawns, and can be spawned with {@code /pz gate spawn <rank> <size> ancient}.
 *
 * When the gate is cleared, the rift closes but the stones stay: the runes go dark, a few pieces
 * fall, the dais cracks and a dark seal is left where the rift hung ({@link #ruin}).
 *
 * Geometry: {@code base} is the portal's lowest corner (same as the gate anchor). {@code u} runs
 * along the portal's width, {@code n} along its normal (the walk-through direction), {@code h} up.
 * The dais top is the layer just under {@code base}, so the ring stands on it.
 */
public final class AncientGate {
    private AncientGate() {}

    /** Blocks used for one biome's version of the structure. */
    public record Palette(Material material, BlockState main, BlockState alt, BlockState accent,
                          BlockState stairs, BlockState slab, BlockState foundation, BlockState wall,
                          Dressing dressing) {}

    /** Which biome dressing pass runs after the stones are placed. */
    public enum Dressing { PLAIN, FOREST, JUNGLE, DESERT, BADLANDS, SNOW, SWAMP }

    /** Paving half-depth along the normal: two blocks either side of the rift. */
    private static final int PAVE_HALF_DEPTH = 2;
    /** Share of upper ring stones already fallen when the gate is found (they are old). */
    private static final float WEATHERED_MISSING = 0.25F;
    /** Share of ring stones that fall when the gate is cleared. */
    private static final float RUIN_FALL = 0.3F;

    // ---------------------------------------------------------------------------------------------
    // Palettes

    public static Palette paletteFor(Holder<Biome> biome, BlockPos pos) {
        if (biome.is(BiomeTags.IS_JUNGLE)) return palette(Material.MOSSY, Dressing.JUNGLE);
        if (biome.is(BiomeTags.IS_BADLANDS)) return palette(Material.RED_SANDSTONE, Dressing.BADLANDS);
        if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BEACH) && !biome.value().coldEnoughToSnow(pos)) {
            return palette(Material.SANDSTONE, Dressing.DESERT);
        }
        if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) return palette(Material.MUD, Dressing.SWAMP);
        if (biome.value().coldEnoughToSnow(pos)) return palette(Material.DEEPSLATE, Dressing.SNOW);
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(Biomes.DARK_FOREST)) {
            return palette(Material.MOSSY, Dressing.FOREST);
        }
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL)) return palette(Material.STONE, Dressing.PLAIN);
        if (biome.is(BiomeTags.IS_SAVANNA) || biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS)) {
            return palette(Material.STONE, Dressing.PLAIN);
        }
        return palette(Material.MOSSY, Dressing.FOREST);
    }

    public static Palette palette(Material material, Dressing dressing) {
        return switch (material) {
            case STONE -> new Palette(material, Blocks.STONE_BRICKS.defaultBlockState(),
                    Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
                    Blocks.STONE_BRICK_STAIRS.defaultBlockState(), Blocks.STONE_BRICK_SLAB.defaultBlockState(),
                    Blocks.COBBLESTONE.defaultBlockState(), Blocks.STONE_BRICK_WALL.defaultBlockState(), dressing);
            case MOSSY -> new Palette(material, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
                    Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState(), Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState(),
                    Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(), dressing);
            case SANDSTONE -> new Palette(material, Blocks.CUT_SANDSTONE.defaultBlockState(),
                    Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.CHISELED_SANDSTONE.defaultBlockState(),
                    Blocks.SANDSTONE_STAIRS.defaultBlockState(), Blocks.SANDSTONE_SLAB.defaultBlockState(),
                    Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE_WALL.defaultBlockState(), dressing);
            case RED_SANDSTONE -> new Palette(material, Blocks.CUT_RED_SANDSTONE.defaultBlockState(),
                    Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState(), Blocks.CHISELED_RED_SANDSTONE.defaultBlockState(),
                    Blocks.RED_SANDSTONE_STAIRS.defaultBlockState(), Blocks.RED_SANDSTONE_SLAB.defaultBlockState(),
                    Blocks.RED_SANDSTONE.defaultBlockState(), Blocks.RED_SANDSTONE_WALL.defaultBlockState(), dressing);
            case MUD -> new Palette(material, Blocks.MUD_BRICKS.defaultBlockState(),
                    Blocks.PACKED_MUD.defaultBlockState(), Blocks.CHISELED_TUFF_BRICKS.defaultBlockState(),
                    Blocks.MUD_BRICK_STAIRS.defaultBlockState(), Blocks.MUD_BRICK_SLAB.defaultBlockState(),
                    Blocks.PACKED_MUD.defaultBlockState(), Blocks.MUD_BRICK_WALL.defaultBlockState(), dressing);
            case DEEPSLATE -> new Palette(material, Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
                    Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), Blocks.CHISELED_DEEPSLATE.defaultBlockState(),
                    Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState(), Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState(),
                    Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(), dressing);
        };
    }

    // ---------------------------------------------------------------------------------------------
    // Geometry

    /** Ring cells for a portal of this size (precomputed with the models, see AncientStonePiece). */
    public static List<AncientStonePiece.Cell> ringCells(GateSize size) {
        return AncientStonePiece.ring(size);
    }

    private static int ringMinU(GateSize size) {
        return ringCells(size).stream().mapToInt(AncientStonePiece.Cell::u).min().orElse(-2);
    }

    private static int ringMaxU(GateSize size) {
        return ringCells(size).stream().mapToInt(AncientStonePiece.Cell::u).max().orElse(size.portalWidth() + 1);
    }

    private static int ringTop(GateSize size) {
        return ringCells(size).stream().mapToInt(AncientStonePiece.Cell::h).max().orElse(size.portalHeight() + 1);
    }

    private static BlockPos at(BlockPos base, boolean alongX, int u, int h, int n) {
        return alongX ? base.offset(u, h, n) : base.offset(n, h, u);
    }

    /** Portal anchor for a gate centred over {@code ground} (the top solid block): the rift stands on the land. */
    public static BlockPos baseFromGround(BlockPos ground, GateSize size, boolean alongX) {
        return ground.above()
                .relative(alongX ? Direction.WEST : Direction.NORTH, size.portalWidth() / 2);
    }

    /**
     * Places the portal blocks of a gate that has no dungeon slot yet (world generation runs before
     * any dungeon data exists). The slot is allocated on first entry, see GateService#onEnterPortal.
     */
    public static void placeUnlinkedPortal(LevelAccessor level, BlockPos base, GateRank rank, GateSize size,
                                           boolean alongX, long openTime) {
        BlockState portal = ModBlocks.GATE_PORTAL.get().defaultBlockState()
                .setValue(GateBlock.RANK, rank)
                .setValue(GateBlock.EXIT, false)
                .setValue(GateBlock.SEA, false);
        for (int u = 0; u < size.portalWidth(); u++) {
            for (int h = 0; h < size.portalHeight(); h++) {
                BlockPos p = at(base, alongX, u, h, 0);
                level.setBlock(p, portal, Block.UPDATE_CLIENTS);
                if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                    be.configure(rank, size, openTime, -1, base);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Building

    /**
     * Builds the stone ring, worn paving and biome dressing around a portal whose lowest corner is
     * {@code base}. The ring stands at ground level with its lower arc sunk into the earth, framed by
     * paving a quarter or an eighth of a block tall, much of it gone. Never touches gate portal
     * blocks, so it can run before or after they are placed. Works on a live level or in worldgen.
     */
    public static void build(LevelAccessor level, BlockPos base, GateRank rank, GateSize size, boolean alongX,
                             Palette palette, RandomSource random, boolean lit) {
        int pw = size.portalWidth();
        int uMin = ringMinU(size) - 1;
        int uMax = ringMaxU(size) + 1;
        int top = ringTop(size);
        BlockState stone = stone(palette.material(), alongX, rank, lit);

        // 1. Clear plants, trees and a little hillside out of the footprint and the ring's span.
        for (int u = uMin - 1; u <= uMax + 1; u++) {
            for (int n = -PAVE_HALF_DEPTH - 1; n <= PAVE_HALF_DEPTH + 1; n++) {
                int height = n == 0 && u >= uMin && u <= uMax ? top + 2 : 3;
                for (int h = 0; h <= height; h++) clear(level, at(base, alongX, u, h, n));
            }
        }

        // 2. Solid ground under the ring line, so no stone floats where the land dips.
        for (int u = uMin; u <= uMax; u++) {
            foundation(level, at(base, alongX, u, -1, 0), palette.foundation());
        }

        // 3. The ring, weathered: some stones are already gone and lie around as rubble. The low
        //    stones and the keystones hold, so the circle still reads.
        for (AncientStonePiece.Cell c : ringCells(size)) {
            boolean holds = c.piece().kind() == AncientStonePiece.Kind.KEYSTONE || c.h() == 0;
            if (!holds && random.nextFloat() < WEATHERED_MISSING) {
                dropRubble(level, base, alongX, size, stone, random);
                continue;
            }
            level.setBlock(at(base, alongX, c.u(), c.h(), 0), stone.setValue(AncientGateStoneBlock.PIECE, c.piece()),
                    Block.UPDATE_CLIENTS);
        }

        // 4. Worn paving: quarter-block plates near the ring, eighth-block plates at the edge, many
        //    missing. Each plate follows the ground under it. Rune plates flank the rift.
        BlockState high = stone.setValue(AncientGateStoneBlock.PIECE, AncientStonePiece.PLATE_HIGH);
        BlockState low = stone.setValue(AncientGateStoneBlock.PIECE, AncientStonePiece.PLATE_LOW);
        BlockState seal = stone.setValue(AncientGateStoneBlock.PIECE, AncientStonePiece.SEAL);
        for (int u = uMin; u <= uMax; u++) {
            for (int n = -PAVE_HALF_DEPTH; n <= PAVE_HALF_DEPTH; n++) {
                if (n == 0) continue;   // the ring line itself
                boolean flank = Math.abs(n) == 1 && u >= 0 && u < pw;
                boolean edge = Math.abs(n) == PAVE_HALF_DEPTH || u == uMin || u == uMax;
                if (!flank && random.nextFloat() < (edge ? 0.6F : 0.35F)) continue;
                BlockPos p = settle(level, at(base, alongX, u, 1, n), 3);
                if (p == null) continue;
                BlockState plate = flank ? seal : edge || random.nextFloat() < 0.25F ? low : high;
                // Dark plates use the cracked texture; they never glow (see AncientGateStoneBlock).
                if (!flank && random.nextFloat() < 0.35F) plate = plate.setValue(AncientGateStoneBlock.LIT, false);
                level.setBlock(p, plate, Block.UPDATE_CLIENTS);
            }
        }

        // 5. A broken pillar stub or two at the corners of the old floor.
        for (int u : new int[] {uMin, uMax}) {
            for (int n : new int[] {-PAVE_HALF_DEPTH, PAVE_HALF_DEPTH}) {
                if (random.nextFloat() > 0.3F) continue;
                BlockPos p = settle(level, at(base, alongX, u, 1, n), 3);
                if (p != null) level.setBlock(p, palette.wall(), Block.UPDATE_CLIENTS);
            }
        }

        // 6. Biome dressing.
        dress(level, base, alongX, size, palette, random, uMin, uMax);
    }

    /** The default ring stone for this gate. */
    public static BlockState stone(Material material, boolean alongX, GateRank rank, boolean lit) {
        return ModBlocks.ANCIENT_GATE_STONE.get().defaultBlockState()
                .setValue(AncientGateStoneBlock.MATERIAL, material)
                .setValue(AncientGateStoneBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z)
                .setValue(AncientGateStoneBlock.RANK, rank)
                .setValue(AncientGateStoneBlock.LIT, lit);
    }

    private static void clear(LevelAccessor level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        if (st.isAir() || st.getBlock() instanceof GateBlock) return;
        if (st.getDestroySpeed(level, p) < 0) return;
        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Fills down from {@code top} until it meets solid ground, so the dais never floats. */
    private static void foundation(LevelAccessor level, BlockPos top, BlockState block) {
        BlockPos.MutableBlockPos p = top.mutable();
        for (int i = 0; i < 12; i++) {
            BlockState st = level.getBlockState(p);
            if (!isLoose(st)) return;
            if (p.getY() <= level.getMinBuildHeight()) return;
            level.setBlock(p, block, Block.UPDATE_CLIENTS);
            p.move(Direction.DOWN);
        }
    }

    private static boolean isLoose(BlockState st) {
        return st.isAir() || st.canBeReplaced() || !st.getFluidState().isEmpty()
                || st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || !st.blocksMotion();
    }

    /** Puts a fallen chunk on a free spot of the dais top or the ground around it. */
    private static void dropRubble(LevelAccessor level, BlockPos base, boolean alongX, GateSize size,
                                   BlockState stone, RandomSource random) {
        int uMin = ringMinU(size) - 1;
        int uMax = ringMaxU(size) + 1;
        for (int tries = 0; tries < 6; tries++) {
            int u = uMin + random.nextInt(uMax - uMin + 1);
            int n = (1 + random.nextInt(PAVE_HALF_DEPTH + 1)) * (random.nextBoolean() ? 1 : -1);
            BlockPos p = settle(level, at(base, alongX, u, 2, n), 8);
            if (p != null) {
                level.setBlock(p, stone.setValue(AncientGateStoneBlock.PIECE, AncientStonePiece.RUBBLE), Block.UPDATE_CLIENTS);
                return;
            }
        }
    }

    /** First air block resting on something sturdy, searching down from {@code from}. */
    private static @Nullable BlockPos settle(LevelAccessor level, BlockPos from, int depth) {
        BlockPos.MutableBlockPos p = from.mutable();
        for (int i = 0; i < depth; i++) {
            BlockState here = level.getBlockState(p);
            BlockPos below = p.below();
            if (!here.isAir() && !here.canBeReplaced()) return null;
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return here.isAir() || here.canBeReplaced() ? p.immutable() : null;
            }
            p.move(Direction.DOWN);
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // Biome dressing

    private static void dress(LevelAccessor level, BlockPos base, boolean alongX, GateSize size, Palette palette,
                              RandomSource random, int uMin, int uMax) {
        int reachN = PAVE_HALF_DEPTH + 3;
        Dressing d = palette.dressing();

        // Ground around the ring and in the gaps of the paving (plates are not air, so they are skipped).
        for (int u = uMin - 3; u <= uMax + 3; u++) {
            for (int n = -reachN; n <= reachN; n++) {
                if (n == 0 && u >= uMin && u <= uMax) continue;
                BlockPos spot = settle(level, at(base, alongX, u, 1, n), 6);
                if (spot == null) continue;
                boolean inFootprint = Math.abs(n) <= PAVE_HALF_DEPTH && u >= uMin && u <= uMax;
                if (inFootprint) daisDressing(level, spot, d, palette, random, false);
                else groundDressing(level, spot, d, palette, random);
            }
        }

        // Overgrowth on the ring itself: leaf clumps hugging its shoulders with vines hanging off.
        if (d == Dressing.JUNGLE || d == Dressing.SWAMP) {
            BlockState leaves = (d == Dressing.JUNGLE ? Blocks.JUNGLE_LEAVES : Blocks.MANGROVE_LEAVES)
                    .defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            for (AncientStonePiece.Cell c : ringCells(size)) {
                if (c.piece().kind() == AncientStonePiece.Kind.KEYSTONE || c.h() < 1) continue;
                if (random.nextFloat() > (d == Dressing.JUNGLE ? 0.4F : 0.2F)) continue;
                int side = c.u() < size.portalWidth() / 2.0 ? -1 : 1;
                BlockPos p = at(base, alongX, c.u() + side, c.h(), 0);
                if (c.h() >= size.portalHeight()) p = at(base, alongX, c.u(), c.h() + 1, 0);   // crown: sit on top
                if (!level.getBlockState(p).isAir()) continue;
                level.setBlock(p, leaves, Block.UPDATE_CLIENTS);
                hangVines(level, p, 1 + random.nextInt(3), random);
            }
        }

        // Desert: half-buried. Sand drifts against the low ring stones.
        if (d == Dressing.DESERT || d == Dressing.BADLANDS) {
            BlockState sand = (d == Dressing.DESERT ? Blocks.SAND : Blocks.RED_SAND).defaultBlockState();
            for (AncientStonePiece.Cell c : ringCells(size)) {
                if (c.h() == 0 && random.nextFloat() < 0.5F) {
                    level.setBlock(at(base, alongX, c.u(), 0, 0), sand, Block.UPDATE_CLIENTS);
                }
            }
            // Drifts over the paving too: a few plates are buried outright.
            for (int u = uMin; u <= uMax; u++) {
                for (int n = -PAVE_HALF_DEPTH; n <= PAVE_HALF_DEPTH; n++) {
                    BlockPos p = at(base, alongX, u, 0, n);
                    if (n != 0 && random.nextFloat() < 0.2F
                            && level.getBlockState(p).getBlock() instanceof AncientGateStoneBlock) {
                        level.setBlock(p, sand, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static void groundDressing(LevelAccessor level, BlockPos p, Dressing d, Palette palette, RandomSource random) {
        float r = random.nextFloat();
        BlockPos below = p.below();
        switch (d) {
            case JUNGLE -> {
                if (r < 0.18F) leafBush(level, p, Blocks.JUNGLE_LEAVES.defaultBlockState(), random);
                else if (r < 0.3F) tallPlant(level, p, Blocks.LARGE_FERN.defaultBlockState());
                else if (r < 0.45F) place(level, p, Blocks.FERN.defaultBlockState());
                else if (r < 0.6F) place(level, p, Blocks.MOSS_CARPET.defaultBlockState());
                else if (r < 0.65F) level.setBlock(below, Blocks.MOSS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            case FOREST -> {
                if (r < 0.15F) place(level, p, Blocks.MOSS_CARPET.defaultBlockState());
                else if (r < 0.25F) place(level, p, Blocks.FERN.defaultBlockState());
                else if (r < 0.31F) place(level, p, palette.foundation());
                else if (r < 0.34F) place(level, p, palette.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            }
            case PLAIN -> {
                if (r < 0.2F) place(level, p, Blocks.SHORT_GRASS.defaultBlockState());
                else if (r < 0.26F) place(level, p, palette.foundation());
                else if (r < 0.3F) place(level, p, palette.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                else if (r < 0.36F) place(level, p, Blocks.MOSS_CARPET.defaultBlockState());
            }
            case DESERT, BADLANDS -> {
                BlockState sand = (d == Dressing.DESERT ? Blocks.SAND : Blocks.RED_SAND).defaultBlockState();
                if (r < 0.3F) {
                    level.setBlock(p, sand, Block.UPDATE_CLIENTS);
                    if (random.nextFloat() < 0.3F && level.getBlockState(p.above()).isAir()) {
                        level.setBlock(p.above(), sand, Block.UPDATE_CLIENTS);
                    }
                } else if (r < 0.38F) {
                    place(level, p, Blocks.DEAD_BUSH.defaultBlockState());
                } else if (r < 0.43F) {
                    place(level, p, palette.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                } else if (d == Dressing.BADLANDS && r < 0.48F) {
                    place(level, p, Blocks.TERRACOTTA.defaultBlockState());
                }
            }
            case SNOW -> {
                if (r < 0.55F) snow(level, p, 1 + random.nextInt(3));
                else if (r < 0.62F) level.setBlock(below, Blocks.PACKED_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
                else if (r < 0.66F) place(level, p, palette.foundation());
            }
            case SWAMP -> {
                if (r < 0.25F && level.getBlockState(below).is(BlockTags.DIRT)) {
                    level.setBlock(below, Blocks.MUD.defaultBlockState(), Block.UPDATE_CLIENTS);
                } else if (r < 0.37F) {
                    int tall = 1 + random.nextInt(3);
                    for (int i = 0; i < tall && level.getBlockState(p.above(i)).isAir(); i++) {
                        level.setBlock(p.above(i), Blocks.MANGROVE_ROOTS.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                } else if (r < 0.5F) {
                    place(level, p, Blocks.MOSS_CARPET.defaultBlockState());
                }
            }
        }
    }

    private static void daisDressing(LevelAccessor level, BlockPos p, Dressing d, Palette palette, RandomSource random,
                                     boolean walkLine) {
        float r = random.nextFloat();
        switch (d) {
            case JUNGLE, SWAMP, FOREST -> {
                float moss = d == Dressing.FOREST ? 0.3F : 0.5F;
                if (r < moss) place(level, p, Blocks.MOSS_CARPET.defaultBlockState());
                else if (!walkLine && d == Dressing.JUNGLE && r < moss + 0.1F) place(level, p, Blocks.FERN.defaultBlockState());
            }
            case PLAIN -> {
                if (r < 0.15F) place(level, p, Blocks.MOSS_CARPET.defaultBlockState());
            }
            case DESERT, BADLANDS -> {
                if (r < 0.3F) {
                    level.setBlock(p, (d == Dressing.DESERT ? Blocks.SAND : Blocks.RED_SAND).defaultBlockState(),
                            Block.UPDATE_CLIENTS);
                }
            }
            case SNOW -> {
                if (r < 0.7F) snow(level, p, 1 + random.nextInt(2));
            }
        }
    }

    private static void place(LevelAccessor level, BlockPos p, BlockState st) {
        if (!level.getBlockState(p).isAir()) return;
        if (!st.canSurvive(level, p)) return;
        level.setBlock(p, st, Block.UPDATE_CLIENTS);
    }

    private static void tallPlant(LevelAccessor level, BlockPos p, BlockState st) {
        if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) return;
        if (!st.canSurvive(level, p)) return;
        level.setBlock(p, st.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
        level.setBlock(p.above(), st.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
    }

    private static void leafBush(LevelAccessor level, BlockPos p, BlockState leaves, RandomSource random) {
        BlockState st = leaves.setValue(LeavesBlock.PERSISTENT, true);
        if (!level.getBlockState(p).isAir()) return;
        level.setBlock(p, st, Block.UPDATE_CLIENTS);
        if (random.nextBoolean() && level.getBlockState(p.above()).isAir()) {
            level.setBlock(p.above(), st, Block.UPDATE_CLIENTS);
        }
    }

    private static void snow(LevelAccessor level, BlockPos p, int layers) {
        BlockState st = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
        place(level, p, st);
    }

    /** A vine on {@code p}, clinging to the wall in direction {@code wall}. */
    private static void vine(LevelAccessor level, BlockPos p, Direction wall) {
        if (!level.getBlockState(p).isAir()) return;
        BlockPos support = p.relative(wall);
        if (!level.getBlockState(support).isFaceSturdy(level, support, wall.getOpposite())) return;
        level.setBlock(p, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(wall), true),
                Block.UPDATE_CLIENTS);
    }

    /**
     * A strand of vines down the side of a leaf clump: the top vine clings to the leaves, each one
     * below hangs from the vine above (same face), which is how vanilla vines stay up.
     */
    private static void hangVines(LevelAccessor level, BlockPos leaves, int length, RandomSource random) {
        Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos top = leaves.relative(side);
        if (!level.getBlockState(top).isAir()) return;
        BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(side.getOpposite()), true);
        level.setBlock(top, vine, Block.UPDATE_CLIENTS);
        for (int i = 1; i < length; i++) {
            BlockPos p = top.below(i);
            if (!level.getBlockState(p).isAir()) return;
            level.setBlock(p, vine, Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Live gate hooks

    /** True when the gate anchored at {@code base} stands in an ancient stone ring. */
    public static boolean isAncient(LevelAccessor level, BlockPos base, GateSize size, boolean alongX) {
        for (AncientStonePiece.Cell c : ringCells(size)) {
            if (level.getBlockState(at(base, alongX, c.u(), c.h(), 0)).getBlock() instanceof AncientGateStoneBlock) {
                return true;
            }
        }
        return false;
    }

    /** Recolors the runes, e.g. after gate aging ranks the gate up. Safe on non-ancient gates. */
    public static void setRank(LevelAccessor level, BlockPos base, GateSize size, boolean alongX, GateRank rank) {
        forEachStone(level, base, size, alongX, (p, st) ->
                level.setBlock(p, st.setValue(AncientGateStoneBlock.RANK, rank), Block.UPDATE_ALL));
    }

    /**
     * The gate was cleared: the rift is gone, so the runes die, some stones fall, the dais cracks
     * and a dark seal marks where the rift hung. Call after the portal blocks are removed.
     */
    public static void ruin(ServerLevel level, BlockPos base, GateSize size, boolean alongX) {
        if (!isAncient(level, base, size, alongX)) return;
        RandomSource random = level.getRandom();
        List<BlockPos> fell = new ArrayList<>();
        BlockState[] sample = new BlockState[1];

        forEachStone(level, base, size, alongX, (p, st) -> {
            AncientStonePiece piece = st.getValue(AncientGateStoneBlock.PIECE);
            BlockState dark = st.setValue(AncientGateStoneBlock.LIT, false);
            sample[0] = dark;
            boolean ringPiece = piece.kind() == AncientStonePiece.Kind.RING;
            if (ringPiece && random.nextFloat() < RUIN_FALL) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState()),
                        p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.05);
                fell.add(p);
            } else {
                level.setBlock(p, dark, Block.UPDATE_ALL);
            }
        });
        if (sample[0] == null) return;

        // Fallen stones land as rubble.
        for (int i = 0; i < fell.size(); i++) {
            dropRubble(level, base, alongX, size, sample[0], random);
        }

        // A dark seal where the rift hung, and cracks across the dais top.
        BlockState seal = sample[0].setValue(AncientGateStoneBlock.PIECE, AncientStonePiece.SEAL);
        for (int u = 0; u < size.portalWidth(); u++) {
            BlockPos p = at(base, alongX, u, 0, 0);
            if (level.getBlockState(p).isAir()) level.setBlock(p, seal, Block.UPDATE_ALL);
        }
        int uMin = ringMinU(size) - 1;
        int uMax = ringMaxU(size) + 1;
        for (int u = uMin; u <= uMax; u++) {
            for (int n = -PAVE_HALF_DEPTH; n <= PAVE_HALF_DEPTH; n++) {
                if (random.nextFloat() > 0.35F) continue;
                BlockPos p = at(base, alongX, u, -1, n);
                BlockState cracked = cracked(level.getBlockState(p));
                if (cracked != null) level.setBlock(p, cracked, Block.UPDATE_ALL);
            }
        }

        BlockPos mid = at(base, alongX, size.portalWidth() / 2, size.portalHeight() / 2, 0);
        level.playSound(null, mid, SoundEvents.DEEPSLATE_BRICKS_BREAK, SoundSource.BLOCKS, 1.4F, 0.6F);
        level.playSound(null, mid, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, mid.getX() + 0.5, base.getY() + 0.2, mid.getZ() + 0.5,
                24, 1.5, 0.2, 1.5, 0.02);
    }

    private static @Nullable BlockState cracked(BlockState st) {
        if (st.is(Blocks.STONE_BRICKS) || st.is(Blocks.MOSSY_STONE_BRICKS)) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        if (st.is(Blocks.DEEPSLATE_BRICKS)) return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        if (st.is(Blocks.CUT_SANDSTONE)) return Blocks.SANDSTONE.defaultBlockState();
        if (st.is(Blocks.CUT_RED_SANDSTONE)) return Blocks.RED_SANDSTONE.defaultBlockState();
        if (st.is(Blocks.MUD_BRICKS)) return Blocks.PACKED_MUD.defaultBlockState();
        return null;
    }

    private interface StoneVisitor {
        void visit(BlockPos pos, BlockState state);
    }

    private static void forEachStone(LevelAccessor level, BlockPos base, GateSize size, boolean alongX, StoneVisitor v) {
        int uMin = ringMinU(size) - 2;
        int uMax = ringMaxU(size) + 2;
        int top = ringTop(size) + 1;
        for (int u = uMin; u <= uMax; u++) {
            for (int n = -PAVE_HALF_DEPTH - 4; n <= PAVE_HALF_DEPTH + 4; n++) {
                for (int h = -4; h <= top; h++) {
                    BlockPos p = at(base, alongX, u, h, n);
                    BlockState st = level.getBlockState(p);
                    if (st.getBlock() instanceof AncientGateStoneBlock) v.visit(p, st);
                }
            }
        }
    }
}
