package com.projectzero.gates;

import com.projectzero.ProjectZero;
import com.projectzero.equipment.EquipmentRules;
import com.projectzero.registry.ModBlocks;
import com.projectzero.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a gate's dungeon in {@code projectzero:dungeon}: a chain of rooms on a grid, joined by
 * corridors, from an entrance room to a boss hall, with side treasure rooms. Each rank has its
 * own look (mossy crypt, root hollow, deep vault, basalt forge, drowned sanctum, crimson throne).
 *
 * The room layout is deterministic from the slot and size, so {@link #exitPortalCenter} and
 * {@link #entranceSpawn} can recompute positions without saving the layout. Slots are never
 * reused (see {@link DungeonSavedData#allocate}), so nothing needs clearing first.
 */
public final class GateDungeonArena {
    private GateDungeonArena() {}

    public static final String DUNGEON_SLOT_KEY = ProjectZero.MOD_ID + ":dungeon_slot";

    private static final int CELL = 26;
    private static final int MAX_LATERAL_CELLS = 5;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    // ---------------------------------------------------------------- layout

    enum Kind { ENTRANCE, COMBAT, TREASURE, BOSS }

    /** A room in absolute block coordinates: walls at x0/x1/z0/z1, floor at y0, ceiling at y0 + height. */
    record Room(Kind kind, int cx, int cz, int x0, int z0, int x1, int z1, int y0, int height) {
        int midX() { return (x0 + x1) / 2; }
        int midZ() { return (z0 + z1) / 2; }
    }

    record Corridor(Room a, Room b) {}

    record Layout(List<Room> rooms, List<Corridor> corridors) {
        Room entrance() { return rooms.get(0); }
        Room boss() {
            for (Room r : rooms) if (r.kind == Kind.BOSS) return r;
            return rooms.get(rooms.size() - 1);
        }
    }

    public static BlockPos origin(int slot) {
        return new BlockPos(slot * GateRules.DUNGEON_SLOT_STRIDE, GateRules.DUNGEON_FLOOR_Y, 0);
    }

    static Layout layout(int slot, GateSize size) {
        RandomSource random = RandomSource.create(slot * 7919L + size.ordinal() * 31L + 17L);
        BlockPos o = origin(slot);
        int combat = switch (size) { case SMALL -> 3; case MEDIUM -> 5; case LARGE -> 7; };
        int treasure = switch (size) { case SMALL -> 1; case MEDIUM -> 2; case LARGE -> 3; };

        List<Room> rooms = new ArrayList<>();
        List<Corridor> corridors = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        Room entrance = room(o, Kind.ENTRANCE, 0, 0, 9, 9, 6);
        rooms.add(entrance);
        used.add(key(0, 0));

        // Main path: forward (+z) or sideways, never back, so the boss cell ahead is always free.
        Room prev = entrance;
        int cx = 0, cz = 0;
        List<Room> path = new ArrayList<>();
        for (int i = 0; i < combat; i++) {
            int nx = cx, nz = cz + 1;
            int roll = random.nextInt(5);
            if (roll == 0 && cx > -MAX_LATERAL_CELLS && !used.contains(key(cx - 1, cz))) { nx = cx - 1; nz = cz; }
            else if (roll == 1 && cx < MAX_LATERAL_CELLS && !used.contains(key(cx + 1, cz))) { nx = cx + 1; nz = cz; }
            cx = nx; cz = nz;
            used.add(key(cx, cz));
            int w = 9 + 2 * random.nextInt(4);      // 9..15
            int l = 9 + 2 * random.nextInt(4);
            Room r = room(o, Kind.COMBAT, cx, cz, w, l, 7 + random.nextInt(2));
            rooms.add(r);
            path.add(r);
            corridors.add(new Corridor(prev, r));
            prev = r;
        }

        // Boss hall straight ahead.
        cz += 1;
        used.add(key(cx, cz));
        Room boss = room(o, Kind.BOSS, cx, cz, 21, 21, 12);
        rooms.add(boss);
        corridors.add(new Corridor(prev, boss));

        // Treasure rooms off the side of combat rooms.
        for (int i = 0; i < treasure && !path.isEmpty(); i++) {
            Room host = path.get(random.nextInt(path.size()));
            int side = random.nextBoolean() ? 1 : -1;
            for (int attempt = 0; attempt < 2; attempt++, side = -side) {
                int tx = host.cx + side;
                if (Math.abs(tx) > MAX_LATERAL_CELLS || used.contains(key(tx, host.cz))) continue;
                used.add(key(tx, host.cz));
                Room t = room(o, Kind.TREASURE, tx, host.cz, 9, 9, 6);
                rooms.add(t);
                corridors.add(new Corridor(host, t));
                break;
            }
        }
        return new Layout(rooms, corridors);
    }

    private static Room room(BlockPos o, Kind kind, int cx, int cz, int w, int l, int h) {
        int midX = o.getX() + cx * CELL;
        int midZ = o.getZ() + cz * CELL + CELL / 2;
        return new Room(kind, cx, cz, midX - w / 2, midZ - l / 2, midX + w / 2, midZ + l / 2, o.getY(), h);
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    public static Vec3 entranceSpawn(int slot) {
        // Size does not move the entrance (always cell 0,0).
        Room e = layout(slot, GateSize.SMALL).entrance();
        return new Vec3(e.midX() + 0.5, e.y0 + 1.0, e.midZ() + 0.5);
    }

    /** Bottom middle of the exit frame, against the far wall of the boss hall. */
    public static BlockPos exitPortalCenter(int slot, GateSize size) {
        Room b = layout(slot, size).boss();
        return new BlockPos(b.midX(), b.y0 + 1, b.z1 - 2);
    }

    // ---------------------------------------------------------------- themes

    private record Theme(BlockState[] walls, BlockState[] floors, BlockState ceiling, BlockState pillar,
                         BlockState accent, BlockState light, boolean hanging, BlockState rug,
                         EntityType<?>[] mobs) {
        BlockState wall(RandomSource r) { return pick(walls, r); }
        BlockState floor(RandomSource r) { return pick(floors, r); }
        private static BlockState pick(BlockState[] a, RandomSource r) {
            // First entry is the main block; the rest are rarer variants.
            return a.length == 1 || r.nextInt(100) < 75 ? a[0] : a[1 + r.nextInt(a.length - 1)];
        }
    }

    private static Theme theme(GateRank rank) {
        return switch (rank) {
            case E -> new Theme( // mossy crypt
                    states(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS),
                    states(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.GRAVEL),
                    Blocks.STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(),
                    Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), Blocks.LANTERN.defaultBlockState(), true,
                    Blocks.MOSS_CARPET.defaultBlockState(),
                    new EntityType<?>[]{EntityType.ZOMBIE, EntityType.SPIDER, EntityType.SKELETON});
            case D -> new Theme( // root hollow
                    states(Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.MUD_BRICKS),
                    states(Blocks.PACKED_MUD, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT),
                    Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.MUD_BRICK_WALL.defaultBlockState(),
                    Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), Blocks.LANTERN.defaultBlockState(), true,
                    Blocks.BROWN_CARPET.defaultBlockState(),
                    new EntityType<?>[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER});
            case C -> new Theme( // deep vault
                    states(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES),
                    states(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.COBBLED_DEEPSLATE),
                    Blocks.DEEPSLATE_TILES.defaultBlockState(), Blocks.DEEPSLATE_TILE_WALL.defaultBlockState(),
                    Blocks.CHISELED_DEEPSLATE.defaultBlockState(), Blocks.SOUL_LANTERN.defaultBlockState(), true,
                    Blocks.CYAN_CARPET.defaultBlockState(),
                    new EntityType<?>[]{EntityType.SKELETON, EntityType.ZOMBIE, EntityType.VINDICATOR, EntityType.SPIDER});
            case B -> new Theme( // basalt forge
                    states(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE),
                    states(Blocks.POLISHED_BLACKSTONE, Blocks.SMOOTH_BASALT, Blocks.MAGMA_BLOCK),
                    Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.POLISHED_BASALT.defaultBlockState(),
                    Blocks.GILDED_BLACKSTONE.defaultBlockState(), Blocks.LANTERN.defaultBlockState(), true,
                    Blocks.ORANGE_CARPET.defaultBlockState(),
                    new EntityType<?>[]{EntityType.VINDICATOR, EntityType.SKELETON, EntityType.STRAY, EntityType.HUSK});
            case A -> new Theme( // drowned sanctum
                    states(Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE, Blocks.DARK_PRISMARINE),
                    states(Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS),
                    Blocks.DARK_PRISMARINE.defaultBlockState(), Blocks.PRISMARINE_WALL.defaultBlockState(),
                    Blocks.PRISMARINE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(), false,
                    Blocks.LIGHT_BLUE_CARPET.defaultBlockState(),
                    new EntityType<?>[]{EntityType.DROWNED, EntityType.STRAY, EntityType.VINDICATOR, EntityType.EVOKER});
            case S -> new Theme( // crimson throne
                    states(Blocks.RED_NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS),
                    states(Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.CRYING_OBSIDIAN),
                    Blocks.RED_NETHER_BRICKS.defaultBlockState(), Blocks.RED_NETHER_BRICK_WALL.defaultBlockState(),
                    Blocks.CRYING_OBSIDIAN.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(), false,
                    Blocks.RED_CARPET.defaultBlockState(),
                    new EntityType<?>[]{EntityType.WITHER_SKELETON, EntityType.VINDICATOR, EntityType.BLAZE, EntityType.SKELETON});
        };
    }

    private static BlockState[] states(Block... blocks) {
        BlockState[] out = new BlockState[blocks.length];
        for (int i = 0; i < blocks.length; i++) out[i] = blocks[i].defaultBlockState();
        return out;
    }

    // ---------------------------------------------------------------- build

    public static void buildAndPopulate(ServerLevel level, int slot, GateRank rank, GateSize size) {
        Layout layout = layout(slot, size);
        Theme theme = theme(rank);
        RandomSource random = RandomSource.create(slot * 104729L + rank.ordinal());

        for (Corridor c : layout.corridors) corridorShell(level, c, theme, random);
        for (Room r : layout.rooms) roomShell(level, r, theme, random);
        for (Corridor c : layout.corridors) corridorCarve(level, c, theme, random);

        for (Room r : layout.rooms) {
            switch (r.kind) {
                case ENTRANCE -> decorateEntrance(level, r, theme, rank, random);
                case COMBAT -> decorateCombat(level, r, theme, random);
                case TREASURE -> decorateTreasure(level, r, theme, rank, random);
                case BOSS -> decorateBoss(level, r, theme, rank, random);
            }
        }

        populate(level, slot, layout, theme, rank, size, random);
    }

    private static void roomShell(ServerLevel level, Room r, Theme t, RandomSource random) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = r.x0; x <= r.x1; x++) {
            for (int z = r.z0; z <= r.z1; z++) {
                for (int dy = 0; dy <= r.height; dy++) {
                    boolean wall = x == r.x0 || x == r.x1 || z == r.z0 || z == r.z1;
                    BlockState s;
                    if (dy == 0) s = t.floor(random);
                    else if (dy == r.height) s = t.ceiling;
                    else if (wall) s = (dy == 1 || dy == r.height - 1) && random.nextInt(3) > 0 ? t.accent : t.wall(random);
                    else s = air;
                    level.setBlock(new BlockPos(x, r.y0 + dy, z), s, FLAGS);
                }
            }
        }
        // Ceiling lights on a grid.
        for (int x = r.x0 + 3; x <= r.x1 - 3; x += 4) {
            for (int z = r.z0 + 3; z <= r.z1 - 3; z += 4) {
                if (t.hanging) {
                    level.setBlock(new BlockPos(x, r.y0 + r.height - 1, z), Blocks.CHAIN.defaultBlockState(), FLAGS);
                    level.setBlock(new BlockPos(x, r.y0 + r.height - 2, z),
                            t.light.setValue(LanternBlock.HANGING, true), FLAGS);
                } else {
                    level.setBlock(new BlockPos(x, r.y0 + r.height, z), t.light, FLAGS);
                }
            }
        }
        // Cobwebs in some upper corners.
        int[][] corners = {{r.x0 + 1, r.z0 + 1}, {r.x1 - 1, r.z0 + 1}, {r.x0 + 1, r.z1 - 1}, {r.x1 - 1, r.z1 - 1}};
        for (int[] c : corners) {
            if (random.nextInt(3) == 0) level.setBlock(new BlockPos(c[0], r.y0 + r.height - 1, c[1]),
                    Blocks.COBWEB.defaultBlockState(), FLAGS);
        }
    }

    /** Corridors run between the two room centres along one axis: 3 wide, 4 tall inside. */
    private static void corridorShell(ServerLevel level, Corridor c, Theme t, RandomSource random) {
        forCorridor(c, (x, z, cross) -> {
            for (int dy = 0; dy <= 5; dy++) {
                BlockState s = dy == 0 ? t.floor(random) : dy == 5 ? t.ceiling : t.wall(random);
                level.setBlock(new BlockPos(x, c.a.y0 + dy, z), s, FLAGS);
            }
        }, 2);
    }

    private static void corridorCarve(ServerLevel level, Corridor c, Theme t, RandomSource random) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int[] step = {0};
        forCorridor(c, (x, z, cross) -> {
            for (int dy = 1; dy <= 4; dy++) level.setBlock(new BlockPos(x, c.a.y0 + dy, z), air, FLAGS);
            level.setBlock(new BlockPos(x, c.a.y0, z), t.floor(random), FLAGS);
        }, 1);
        // A light every few blocks along the corridor ceiling.
        forCorridor(c, (x, z, cross) -> {
            if (cross == 0 && (step[0]++ % 5) == 2) {
                BlockPos p = new BlockPos(x, c.a.y0 + 4, z);
                level.setBlock(p, t.hanging ? t.light.setValue(LanternBlock.HANGING, true) : air, FLAGS);
                if (!t.hanging) level.setBlock(p.above(), t.light, FLAGS);
            }
        }, 0);
    }

    private interface CorridorCell { void at(int x, int z, int cross); }

    private static void forCorridor(Corridor c, CorridorCell cell, int halfWidth) {
        int ax = c.a.midX(), az = c.a.midZ(), bx = c.b.midX(), bz = c.b.midZ();
        if (ax == bx) {
            for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++)
                for (int w = -halfWidth; w <= halfWidth; w++) cell.at(ax + w, z, w);
        } else {
            for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++)
                for (int w = -halfWidth; w <= halfWidth; w++) cell.at(x, az + w, w);
        }
    }

    private static void pillars(ServerLevel level, Room r, Theme t, int inset) {
        int[][] spots = {{r.x0 + inset, r.z0 + inset}, {r.x1 - inset, r.z0 + inset},
                {r.x0 + inset, r.z1 - inset}, {r.x1 - inset, r.z1 - inset}};
        for (int[] s : spots) {
            for (int dy = 1; dy < r.height; dy++) {
                level.setBlock(new BlockPos(s[0], r.y0 + dy, s[1]), t.pillar, FLAGS);
            }
        }
    }

    private static void decorateEntrance(ServerLevel level, Room r, Theme t, GateRank rank, RandomSource random) {
        // Rug down the middle toward the way in.
        for (int z = r.midZ() - 2; z <= r.z1 - 1; z++) level.setBlock(new BlockPos(r.midX(), r.y0 + 1, z), t.rug, FLAGS);
        // The way back out, against the near wall: open for E-A, sealed for S (no retreat).
        BlockPos base = new BlockPos(r.midX(), r.y0 + 1, r.z0 + 1);
        boolean sealed = rank == GateRank.S;
        BlockState portal = ModBlocks.GATE_PORTAL.get().defaultBlockState()
                .setValue(GateBlock.RANK, rank).setValue(GateBlock.EXIT, true);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                boolean frame = dx != 0 || dy == 3;
                BlockPos p = base.offset(dx, dy, 0);
                level.setBlock(p, frame ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : sealed ? Blocks.IRON_BARS.defaultBlockState() : portal, Block.UPDATE_ALL);
                if (!frame && !sealed && level.getBlockEntity(p) instanceof GateBlockEntity be) {
                    be.configure(rank, GateSize.SMALL, level.getGameTime(), -1, base);
                }
            }
        }
    }

    private static void decorateCombat(ServerLevel level, Room r, Theme t, RandomSource random) {
        if (r.x1 - r.x0 >= 12 && r.z1 - r.z0 >= 12) pillars(level, r, t, 3);
        // Rubble and broken floor.
        for (int i = 0; i < 4; i++) {
            int x = r.x0 + 2 + random.nextInt(Math.max(1, r.x1 - r.x0 - 3));
            int z = r.z0 + 2 + random.nextInt(Math.max(1, r.z1 - r.z0 - 3));
            if (Math.abs(x - r.midX()) <= 1 || Math.abs(z - r.midZ()) <= 1) continue;   // keep paths clear
            level.setBlock(new BlockPos(x, r.y0 + 1, z), random.nextBoolean() ? t.wall(random) : t.accent, FLAGS);
        }
    }

    private static void decorateTreasure(ServerLevel level, Room r, Theme t, GateRank rank, RandomSource random) {
        for (int x = r.midX() - 1; x <= r.midX() + 1; x++)
            for (int z = r.midZ() - 1; z <= r.midZ() + 1; z++)
                level.setBlock(new BlockPos(x, r.y0 + 1, z), t.rug, FLAGS);
        // Chest on a plinth, facing the corridor side (toward the room's middle row).
        BlockPos plinth = new BlockPos(r.midX(), r.y0, r.midZ());
        level.setBlock(plinth, t.accent, FLAGS);
        BlockPos chestPos = plinth.above();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.withDefaultNamespace(rank.ordinal() >= GateRank.B.ordinal()
                            ? "chests/stronghold_corridor" : "chests/simple_dungeon"));
            chest.setLootTable(table, random.nextLong());
            Container box = chest;
            if (random.nextFloat() < 0.45F) {
                box.setItem(4, EquipmentRules.rollItem(random, EquipmentRules.randomDroppableSlot(random),
                        EquipmentRules.rollStandardRarity(random, false), rank));
            }
            if (random.nextFloat() < 0.10F + rank.ordinal() * 0.04F) {
                box.setItem(13, new ItemStack(ModItems.AWAKENING_SHARD.get()));
            }
        }
        level.setBlock(new BlockPos(r.x0 + 1, r.y0 + 1, r.z0 + 1), Blocks.BARREL.defaultBlockState(), FLAGS);
        level.setBlock(new BlockPos(r.x1 - 1, r.y0 + 1, r.z1 - 1), Blocks.BARREL.defaultBlockState(), FLAGS);
    }

    private static void decorateBoss(ServerLevel level, Room r, Theme t, GateRank rank, RandomSource random) {
        // Ring of pillars.
        int cx = r.midX(), cz = r.midZ();
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            int px = cx + (int) Math.round(Math.cos(a) * 7);
            int pz = cz + (int) Math.round(Math.sin(a) * 7);
            for (int dy = 1; dy < r.height; dy++) level.setBlock(new BlockPos(px, r.y0 + dy, pz), t.pillar, FLAGS);
            level.setBlock(new BlockPos(px, r.y0 + r.height - 1, pz), t.accent, FLAGS);
        }
        // Ring of the rank's glow set into the floor.
        BlockState glow = rankGlass(rank);
        for (int k = 0; k < 48; k++) {
            double a = k * Math.PI * 2 / 48;
            level.setBlock(new BlockPos(cx + (int) Math.round(Math.cos(a) * 5), r.y0,
                    cz + (int) Math.round(Math.sin(a) * 5)), glow, FLAGS);
        }
        // Raised dais at the far end with the sealed exit frame.
        for (int x = cx - 4; x <= cx + 4; x++)
            for (int z = r.z1 - 4; z <= r.z1 - 1; z++)
                level.setBlock(new BlockPos(x, r.y0 + 1, z), t.accent, FLAGS);
        BlockPos exit = new BlockPos(cx, r.y0 + 1, r.z1 - 2);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                boolean frame = dx != 0 || dy == 3;
                level.setBlock(exit.offset(dx, dy, 0),
                        frame ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState(), FLAGS);
            }
        }
        // Rug from the door to the dais.
        for (int z = r.z0 + 1; z < r.z1 - 4; z++) level.setBlock(new BlockPos(cx, r.y0 + 1, z), t.rug, FLAGS);
    }

    private static BlockState rankGlass(GateRank rank) {
        return switch (rank) {
            case E -> Blocks.BLUE_STAINED_GLASS.defaultBlockState();
            case D -> Blocks.LIME_STAINED_GLASS.defaultBlockState();
            case C -> Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
            case B -> Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
            case A -> Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
            case S -> Blocks.RED_STAINED_GLASS.defaultBlockState();
        };
    }

    // ---------------------------------------------------------------- mobs

    private static void populate(ServerLevel level, int slot, Layout layout, Theme theme, GateRank rank,
                                 GateSize size, RandomSource random) {
        List<Room> combat = new ArrayList<>();
        for (Room r : layout.rooms) if (r.kind == Kind.COMBAT) combat.add(r);
        int total = size.mobCount() + combat.size() * 2;
        for (int i = 0; i < total && !combat.isEmpty(); i++) {
            Room r = combat.get(i % combat.size());
            EntityType<?> type = theme.mobs[random.nextInt(theme.mobs.length)];
            if (!(type.create(level) instanceof Mob mob)) continue;
            double mx = r.x0 + 2 + random.nextDouble() * Math.max(1, r.x1 - r.x0 - 4);
            double mz = r.z0 + 2 + random.nextDouble() * Math.max(1, r.z1 - r.z0 - 4);
            mob.moveTo(mx, r.y0 + 1.0, mz, random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
            mob.setPersistenceRequired();
            mob.getPersistentData().putBoolean(MobScaling.GATE_MONSTER_KEY, true);
            mob.getPersistentData().putInt(DUNGEON_SLOT_KEY, slot);
            MobScaling.apply(mob, rank, rank.randomLevel(random));
            level.addFreshEntity(mob);
        }

        Room bossRoom = layout.boss();
        EntityType<? extends Mob> bossType = switch (rank) {
            case E, D -> EntityType.HUSK;
            case C, B -> EntityType.VINDICATOR;
            case A, S -> EntityType.WITHER_SKELETON;
        };
        Mob boss = bossType.create(level);
        if (boss != null) {
            boss.moveTo(bossRoom.midX() + 0.5, bossRoom.y0 + 1.0, bossRoom.midZ() + 0.5, 180.0F, 0.0F);
            boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.EVENT, null);
            boss.setPersistenceRequired();
            boss.getPersistentData().putBoolean(MobScaling.GATE_MONSTER_KEY, true);
            boss.getPersistentData().putBoolean(MobScaling.GATE_BOSS_KEY, true);
            boss.getPersistentData().putInt(DUNGEON_SLOT_KEY, slot);
            MobScaling.apply(boss, rank, rank.maxLevel());

            AttributeInstance hp = boss.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.addPermanentModifier(new AttributeModifier(
                        ProjectZero.id("gate_boss_hp"),
                        GateRules.BOSS_EXTRA_HEALTH_MULT - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                boss.setHealth(boss.getMaxHealth());
            }
            boss.setCustomName(Component.literal("[" + rank.name() + " · BOSS · Lv " + rank.maxLevel() + "] ")
                    .withStyle(rank.chatColor(), ChatFormatting.BOLD)
                    .append(boss.getType().getDescription().copy().withStyle(ChatFormatting.GOLD)));
            boss.setCustomNameVisible(true);
            level.addFreshEntity(boss);
        }
    }

    public static void openExitPortal(ServerLevel level, int slot, GateSize size, GateRank rank) {
        BlockPos exitBase = exitPortalCenter(slot, size);
        BlockState portal = ModBlocks.GATE_PORTAL.get().defaultBlockState()
                .setValue(GateBlock.RANK, rank)
                .setValue(GateBlock.EXIT, true);
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = exitBase.offset(0, dy, 0);
            level.setBlock(p, portal, Block.UPDATE_ALL);
            if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                be.configure(rank, size, level.getGameTime(), slot, exitBase);
            }
        }
    }
}
