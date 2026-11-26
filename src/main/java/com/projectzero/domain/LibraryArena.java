package com.projectzero.domain;

import com.projectzero.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The Library of Paths: a vast round hall in the void "library" dimension.
 *
 *   - The player stands on a narrow PEDESTAL rising out of a dark abyss (floor ~40 blocks down).
 *   - BOOKSHELF WALLS tower 70 blocks around them, ringed with balconies and pillars, fading into
 *     the dark so they read as endless.
 *   - Eight glowing STATUES of robed scholars stand on the balcony level of the pedestal,
 *     facing in, each holding a glowing book.
 *   - A DOMED ROOF closes the hall, with an oculus of Awakening Crystal and hanging lanterns.
 *   - Floating glowing specks come from {@link LibraryDomain} (particles), not blocks.
 *
 * Built once per slot and reused: a marker block records the arena version, so a design change
 * only needs {@link #VERSION} bumped. Built far from the v1 arenas (z = {@link #Z_OFFSET}).
 */
public final class LibraryArena {
    private LibraryArena() {}

    public static final int VERSION = 2;
    public static final int Z_OFFSET = 10_000;
    public static final int SLOT_SPACING = 256;

    public static final int ABYSS_Y = 60;           // hall floor, far below
    public static final int PEDESTAL_TOP = 100;     // player stands at PEDESTAL_TOP + 1
    public static final int WALL_TOP = 130;
    public static final int DOME_HEIGHT = 16;
    public static final int RADIUS = 24;            // inner face of the bookshelf wall
    public static final int BALCONY_Y = PEDESTAL_TOP - 4;
    private static final double PEDESTAL_RADIUS = 2.6;

    /** Flags: send to clients, skip shape updates (faster for big builds). */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public static BlockPos center(int slot) {
        return new BlockPos(slot * SLOT_SPACING, PEDESTAL_TOP + 1, Z_OFFSET);
    }

    /** Builds the hall for this slot unless the current version is already there. */
    public static void ensureBuilt(ServerLevel level, int slot) {
        BlockPos c = center(slot);
        BlockPos marker = new BlockPos(c.getX(), 1 + VERSION, c.getZ());
        if (level.getBlockState(marker).is(Blocks.LODESTONE)) return;
        build(level, c, RandomSource.create(slot * 31L + VERSION));
        level.setBlock(marker, Blocks.LODESTONE.defaultBlockState(), FLAGS);
    }

    private static void build(ServerLevel level, BlockPos c, RandomSource random) {
        int cx = c.getX(), cz = c.getZ();
        int outer = RADIUS + 3;

        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > outer + 0.5) continue;
                int x = cx + dx, z = cz + dz;
                double angle = Math.toDegrees(Math.atan2(dz, dx)) + 180.0;

                // --- abyss floor: dark tiles with two glowing rings ---
                BlockState floor = Math.abs(d - 9) < 0.55 || Math.abs(d - 17) < 0.55
                        ? core() : (((dx + dz) & 1) == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).defaultBlockState();
                set(level, x, ABYSS_Y, z, floor);

                // --- pedestal ---
                if (d <= PEDESTAL_RADIUS) {
                    for (int y = ABYSS_Y + 1; y < PEDESTAL_TOP; y++) {
                        boolean band = (PEDESTAL_TOP - y) % 7 == 0;
                        boolean edge = d > PEDESTAL_RADIUS - 1;
                        BlockState s = band && edge ? core()
                                : edge ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                        set(level, x, y, z, s);
                    }
                    set(level, x, PEDESTAL_TOP, z, d <= 1.0
                            ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                            : Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                    // Crystal crown around the top edge
                    if (d > PEDESTAL_RADIUS - 1) set(level, x, PEDESTAL_TOP - 1, z, core());
                }
                // Flared base of the pedestal
                if (d > PEDESTAL_RADIUS && d <= PEDESTAL_RADIUS + 1.5) {
                    for (int y = ABYSS_Y + 1; y <= ABYSS_Y + 3; y++) set(level, x, y, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                }

                // --- bookshelf wall (3 thick) with pillars every 30° ---
                if (d >= RADIUS - 0.5 && d <= RADIUS + 2.5) {
                    boolean pillar = (angle % 30) < 4 || (angle % 30) > 26;
                    for (int y = ABYSS_Y + 1; y <= WALL_TOP; y++) {
                        BlockState s;
                        if (pillar) s = Blocks.DARK_OAK_LOG.defaultBlockState();
                        else if ((y - ABYSS_Y) % 10 == 0) s = Blocks.DARK_OAK_PLANKS.defaultBlockState();
                        else s = random.nextInt(6) == 0 ? Blocks.CHISELED_BOOKSHELF.defaultBlockState()
                                : Blocks.BOOKSHELF.defaultBlockState();
                        set(level, x, y, z, s);
                    }
                }

                // --- balcony rings every 10 blocks, sticking out of the wall ---
                if (d >= RADIUS - 2.5 && d < RADIUS - 0.5) {
                    for (int y = ABYSS_Y + 10; y < WALL_TOP; y += 10) {
                        set(level, x, y, z, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                        if (d < RADIUS - 1.5 && (angle % 15) < 3) {
                            set(level, x, y + 1, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
                            set(level, x, y + 2, z, Blocks.SOUL_LANTERN.defaultBlockState());
                        }
                    }
                }

                // --- dome ---
                if (d <= RADIUS + 2.5) {
                    double h = DOME_HEIGHT * Math.sqrt(Math.max(0, 1 - (d * d) / ((RADIUS + 2.5) * (RADIUS + 2.5))));
                    int y = WALL_TOP + (int) Math.round(h);
                    if (d <= 3.5) {
                        set(level, x, y, z, ModBlocks.AWAKENING_CRYSTAL.get().defaultBlockState());   // oculus
                    } else {
                        boolean rib = (angle % 30) < 3;
                        set(level, x, y, z, rib ? Blocks.DARK_OAK_LOG.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
                        set(level, x, y + 1, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                    }
                }
            }
        }

        // Hanging lanterns from the dome
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30 + 15);
            int r = i % 2 == 0 ? 8 : 15;
            int x = cx + (int) Math.round(Math.cos(a) * r), z = cz + (int) Math.round(Math.sin(a) * r);
            int top = WALL_TOP + (int) Math.round(DOME_HEIGHT * Math.sqrt(1 - (double) (r * r) / ((RADIUS + 2.5) * (RADIUS + 2.5)))) - 1;
            int len = 6 + random.nextInt(8);
            for (int y = top; y > top - len; y--) set(level, x, y, z, Blocks.CHAIN.defaultBlockState());
            set(level, x, top - len, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }

        // Statues on a ring platform around the pedestal, at balcony height
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45 + 22.5);
            int r = 14;
            int x = cx + (int) Math.round(Math.cos(a) * r), z = cz + (int) Math.round(Math.sin(a) * r);
            // floating plinth under each statue, with a crystal light below
            for (int px = -1; px <= 1; px++) for (int pz = -1; pz <= 1; pz++) {
                set(level, x + px, BALCONY_Y, z + pz, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                set(level, x + px, BALCONY_Y - 1, z + pz, px == 0 && pz == 0 ? core() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
            statue(level, new BlockPos(x, BALCONY_Y + 1, z), facingTo(cx - x, cz - z));
        }
    }

    /** A robed scholar about 7 tall, holding a glowing book, facing the pedestal. */
    private static void statue(ServerLevel level, BlockPos base, Direction facing) {
        BlockState robe = Blocks.CALCITE.defaultBlockState();
        BlockState trim = Blocks.QUARTZ_PILLAR.defaultBlockState();
        Direction side = facing.getClockWise();
        // Robe: 3 wide at the bottom, narrowing
        for (int y = 0; y < 2; y++) for (int s = -1; s <= 1; s++) set(level, base.relative(side, s).above(y), robe);
        for (int y = 2; y < 5; y++) set(level, base.above(y), trim);
        // Shoulders and arms reaching forward
        for (int s = -1; s <= 1; s += 2) {
            set(level, base.relative(side, s).above(4), robe);
            set(level, base.relative(side, s).relative(facing).above(3), robe);
        }
        // The glowing book held in front
        set(level, base.relative(facing).above(3), core());
        // Head with a hood, and glowing eyes
        set(level, base.above(5), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        set(level, base.above(5).relative(facing), Blocks.SEA_LANTERN.defaultBlockState());
        set(level, base.above(6), Blocks.QUARTZ_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing.getOpposite()).setValue(StairBlock.HALF, Half.BOTTOM));
        // Halo of light behind the head
        set(level, base.above(6).relative(facing.getOpposite()), core());
    }

    private static Direction facingTo(int dx, int dz) {
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    private static BlockState core() {
        return ModBlocks.AWAKENING_CORE.get().defaultBlockState();
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, FLAGS);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, FLAGS);
    }
}
