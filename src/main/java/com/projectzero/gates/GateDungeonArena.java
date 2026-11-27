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
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a gate's dungeon in {@code projectzero:dungeon} (DESIGN-DOCUMENT.md §9).
 *
 * A dungeon is a remnant of another world: a big cave system of domed chambers joined by
 * winding tunnels, from an entrance cavern to a huge boss cavern, with side caves. Each dungeon
 * rolls a "world" (crystal grotto, sunken ruins, fungal hollow, frozen tomb, ember depths, bone
 * ossuary, sculk abyss, mossy crypt) weighted by rank. Walls carry mineable mana crystals,
 * chambers hold broken ruins of whatever lived there, and chests are scattered around.
 *
 * Even a small gate is big: 4 chambers and a boss cavern, roughly 130 blocks end to end.
 *
 * The layout is deterministic from the slot and size, so {@link #exitPortalCenter} and
 * {@link #entranceSpawn} recompute positions without saving anything. The world type also
 * comes from the slot, so a gate keeps its look. Slots are never reused.
 *
 * Only a 3-block shell around the caves is placed (the dimension is a void), which keeps a
 * large dungeon to a few hundred thousand block writes on first entry.
 */
public final class GateDungeonArena {
    private GateDungeonArena() {}

    public static final String DUNGEON_SLOT_KEY = ProjectZero.MOD_ID + ":dungeon_slot";

    /** Bulk writes: tell clients, skip neighbour shape updates (we place whole shapes at once). */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int SHELL = 3;
    private static final int ENTRANCE_Z = 18;

    // ---------------------------------------------------------------- layout

    enum Kind { ENTRANCE, CHAMBER, SIDE, TREASURE, BOSS }

    /** A domed cave: floor at {@code floor}, horizontal radii rx/rz, dome height h. */
    record Cave(Kind kind, int x, int floor, int z, int rx, int rz, int h) {
        double reach() { return Math.max(rx, rz); }
    }

    /** A tunnel from a to b through a bend point, radius w, height hgt. */
    record Tunnel(Cave a, Cave b, double bendX, double bendZ, int w, int hgt) {}

    record Layout(List<Cave> caves, List<Tunnel> tunnels) {
        Cave entrance() { return caves.get(0); }
        Cave boss() {
            for (Cave c : caves) if (c.kind == Kind.BOSS) return c;
            return caves.get(caves.size() - 1);
        }
    }

    public static BlockPos origin(int slot) {
        return new BlockPos(slot * GateRules.DUNGEON_SLOT_STRIDE, GateRules.DUNGEON_FLOOR_Y, 0);
    }

    static Layout layout(int slot, GateSize size) {
        return layout(slot, size, false);
    }

    /**
     * Sea dungeons: same plan, but every cave floor sits below the entrance floor and the whole
     * dungeon is flooded up to it (see {@link #waterLevel}); chambers keep an air pocket under
     * their domes to come up for breath.
     */
    static Layout layout(int slot, GateSize size, boolean sea) {
        RandomSource random = RandomSource.create(slot * 7919L + size.ordinal() * 31L + 17L);
        BlockPos o = origin(slot);
        int chain = switch (size) { case SMALL -> 4; case MEDIUM -> 6; case LARGE -> 8; };
        int sides = switch (size) { case SMALL -> 1; case MEDIUM -> 2; case LARGE -> 4; };
        int treasures = switch (size) { case SMALL -> 1; case MEDIUM -> 2; case LARGE -> 3; };
        int maxR = switch (size) { case SMALL -> 13; case MEDIUM -> 15; case LARGE -> 18; };

        List<Cave> caves = new ArrayList<>();
        List<Tunnel> tunnels = new ArrayList<>();
        Cave entrance = new Cave(Kind.ENTRANCE, o.getX(), o.getY(), o.getZ() + ENTRANCE_Z, 10, 10, 9);
        caves.add(entrance);

        // Main chain winding forward (+z), up and down a little.
        Cave prev = entrance;
        double heading = 0;
        List<Cave> chainCaves = new ArrayList<>();
        for (int i = 0; i < chain; i++) {
            int r = 9 + random.nextInt(maxR - 8);
            Cave next = null;
            for (int attempt = 0; attempt < 12 && next == null; attempt++) {
                double h = Math.max(-1.0, Math.min(1.0, heading + (random.nextDouble() - 0.5) * 1.4));
                // Stay inside this slot's lane (slots are 512 apart on X).
                if (prev.x - o.getX() > 110) h = -Math.abs(h);
                else if (prev.x - o.getX() < -110) h = Math.abs(h);
                double dist = prev.reach() + r + 10 + random.nextInt(10);
                int nx = (int) Math.round(prev.x + Math.sin(h) * dist);
                int nz = (int) Math.round(prev.z + Math.cos(h) * dist);
                int floor = sea ? o.getY() - 6 - random.nextInt(8)
                        : Math.max(o.getY() - 8, Math.min(o.getY() + 10, prev.floor + random.nextInt(9) - 4));
                int dome = sea ? (o.getY() - floor) + 5 + random.nextInt(6) : 9 + random.nextInt(8);
                Cave c = new Cave(Kind.CHAMBER, nx, floor, nz, r + random.nextInt(3) - 1, r + random.nextInt(3) - 1, dome);
                if (fits(caves, c, 6)) { next = c; heading = h * 0.6; }
            }
            if (next == null) continue;
            caves.add(next);
            chainCaves.add(next);
            tunnels.add(tunnel(prev, next, random));
            prev = next;
        }

        // Boss cavern straight-ish ahead.
        Cave boss = null;
        for (int attempt = 0; attempt < 20 && boss == null; attempt++) {
            double h = (random.nextDouble() - 0.5) * 0.6;
            int r = 22 + random.nextInt(4);
            double dist = prev.reach() + r + 12 + attempt * 2;
            Cave c = new Cave(Kind.BOSS, (int) Math.round(prev.x + Math.sin(h) * dist), sea ? o.getY() - 10 : prev.floor,
                    (int) Math.round(prev.z + Math.cos(h) * dist), r, r, sea ? 22 : 20);
            if (fits(caves, c, 6)) boss = c;
        }
        if (boss == null) {
            boss = new Cave(Kind.BOSS, prev.x, prev.floor, (int) (prev.z + prev.reach() + 48), 22, 22, 20);
        }
        caves.add(boss);
        tunnels.add(tunnel(prev, boss, random));

        // Side caves and treasure caves branch off the chain.
        for (int i = 0; i < sides + treasures && !chainCaves.isEmpty(); i++) {
            Kind kind = i < treasures ? Kind.TREASURE : Kind.SIDE;
            Cave host = chainCaves.get(random.nextInt(chainCaves.size()));
            for (int attempt = 0; attempt < 10; attempt++) {
                double a = (random.nextBoolean() ? 1 : -1) * (Math.PI / 2 + (random.nextDouble() - 0.5) * 0.9);
                int r = kind == Kind.TREASURE ? 7 + random.nextInt(3) : 9 + random.nextInt(5);
                double dist = host.reach() + r + 9 + random.nextInt(8);
                int floor = sea ? o.getY() - 5 - random.nextInt(6) : host.floor + random.nextInt(5) - 2;
                int h = sea ? (o.getY() - floor) + 4 + random.nextInt(4) : kind == Kind.TREASURE ? 7 : 9 + random.nextInt(5);
                Cave c = new Cave(kind, (int) Math.round(host.x + Math.sin(a) * dist), floor,
                        (int) Math.round(host.z + Math.cos(a) * dist), r, r, h);
                if (fits(caves, c, 6)) {
                    caves.add(c);
                    tunnels.add(tunnel(host, c, random));
                    break;
                }
            }
        }
        return new Layout(caves, tunnels);
    }

    private static boolean fits(List<Cave> caves, Cave c, int gap) {
        for (Cave o : caves) {
            double d = Math.hypot(o.x - c.x, o.z - c.z);
            if (d < o.reach() + c.reach() + gap) return false;
        }
        return true;
    }

    private static Tunnel tunnel(Cave a, Cave b, RandomSource random) {
        double mx = (a.x + b.x) / 2.0, mz = (a.z + b.z) / 2.0;
        double dx = b.x - a.x, dz = b.z - a.z, len = Math.max(1, Math.hypot(dx, dz));
        double off = (random.nextDouble() - 0.5) * Math.min(16, len * 0.35);
        return new Tunnel(a, b, mx - dz / len * off, mz + dx / len * off, 3 + random.nextInt(2), 5 + random.nextInt(2));
    }

    public static Vec3 entranceSpawn(int slot) {
        BlockPos o = origin(slot);
        return new Vec3(o.getX() + 0.5, o.getY() + 1.0, o.getZ() + ENTRANCE_Z + 0.5);
    }

    /** Bottom-left block of the 2x3 exit rift, near the far side of the boss cavern. */
    public static BlockPos exitPortalCenter(int slot, GateSize size) {
        return exitPortalCenter(slot, size, false);
    }

    public static BlockPos exitPortalCenter(int slot, GateSize size, boolean sea) {
        Cave b = layout(slot, size, sea).boss();
        // Sea: on top of the island that rises out of the water.
        return new BlockPos(b.x, sea ? waterLevel(slot) + 1 : b.floor + 2, b.z + b.rz - 7);
    }

    /** Sea dungeons are flooded up to (and including) this Y: the entrance floor. */
    static int waterLevel(int slot) {
        return origin(slot).getY();
    }

    // ---------------------------------------------------------------- worlds

    /** One "other world" look. Blocks are picked with noise so patches read as strata. */
    private record World(String name, BlockState[] walls, BlockState[] floors, BlockState ceiling, BlockState deep,
                         BlockState[] ruin, BlockState ceilingLight, BlockState floorLight, BlockState hanging,
                         BlockState[] flora, BlockState wallGrowth, double crystals) {}

    private static World world(int index) {
        return switch (index) {
            case 0 -> new World("Crystal Grotto",
                    s(Blocks.TUFF, Blocks.CALCITE, Blocks.SMOOTH_BASALT), s(Blocks.TUFF, Blocks.CALCITE),
                    b(Blocks.TUFF), b(Blocks.DEEPSLATE),
                    s(Blocks.TUFF_BRICKS, Blocks.CHISELED_TUFF_BRICKS, Blocks.POLISHED_TUFF),
                    b(Blocks.AMETHYST_BLOCK), b(Blocks.AMETHYST_CLUSTER), null,
                    s(Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD), Blocks.GLOW_LICHEN.defaultBlockState(), 2.2);
            case 1 -> new World("Sunken Ruins",
                    s(Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS), s(Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY),
                    b(Blocks.DARK_PRISMARINE), b(Blocks.STONE),
                    s(Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE),
                    b(Blocks.SEA_LANTERN), b(Blocks.SEA_LANTERN), null,
                    s(Blocks.DEAD_BRAIN_CORAL, Blocks.DEAD_TUBE_CORAL, Blocks.DEAD_FIRE_CORAL), null, 1.0);
            case 2 -> new World("Fungal Hollow",
                    s(Blocks.MUD, Blocks.MOSS_BLOCK, Blocks.PACKED_MUD), s(Blocks.MOSS_BLOCK, Blocks.MYCELIUM, Blocks.ROOTED_DIRT),
                    b(Blocks.MOSS_BLOCK), b(Blocks.DIRT),
                    s(Blocks.MUD_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.PACKED_MUD),
                    b(Blocks.SHROOMLIGHT), b(Blocks.SHROOMLIGHT), b(Blocks.HANGING_ROOTS),
                    s(Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.MOSS_CARPET, Blocks.SHORT_GRASS), Blocks.GLOW_LICHEN.defaultBlockState(), 0.8);
            case 3 -> new World("Frozen Tomb",
                    s(Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SNOW_BLOCK), s(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE),
                    b(Blocks.PACKED_ICE), b(Blocks.STONE),
                    s(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.POLISHED_DIORITE),
                    b(Blocks.SEA_LANTERN), b(Blocks.SOUL_LANTERN), null,
                    s(Blocks.SNOW), null, 1.2);
            case 4 -> new World("Ember Depths",
                    s(Blocks.BLACKSTONE, Blocks.BASALT, Blocks.SMOOTH_BASALT), s(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.MAGMA_BLOCK),
                    b(Blocks.BLACKSTONE), b(Blocks.BASALT),
                    s(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE),
                    b(Blocks.SHROOMLIGHT), b(Blocks.LANTERN), null,
                    s(Blocks.CRIMSON_ROOTS), null, 1.0);
            case 5 -> new World("Bone Ossuary",
                    s(Blocks.SOUL_SOIL, Blocks.BONE_BLOCK, Blocks.DEEPSLATE), s(Blocks.SOUL_SOIL, Blocks.SOUL_SAND),
                    b(Blocks.DEEPSLATE), b(Blocks.DEEPSLATE),
                    s(Blocks.DEEPSLATE_BRICKS, Blocks.BONE_BLOCK, Blocks.CRACKED_DEEPSLATE_BRICKS),
                    b(Blocks.OCHRE_FROGLIGHT), b(Blocks.SOUL_LANTERN), null,
                    s(Blocks.COBWEB, Blocks.SKELETON_SKULL), null, 1.0);
            case 6 -> new World("Sculk Abyss",
                    s(Blocks.DEEPSLATE, Blocks.SCULK, Blocks.COBBLED_DEEPSLATE), s(Blocks.SCULK, Blocks.DEEPSLATE),
                    b(Blocks.DEEPSLATE), b(Blocks.DEEPSLATE),
                    s(Blocks.REINFORCED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE),
                    b(Blocks.VERDANT_FROGLIGHT), b(Blocks.SOUL_LANTERN), null,
                    s(Blocks.SCULK_SENSOR), Blocks.SCULK_VEIN.defaultBlockState(), 1.4);
            default -> new World("Mossy Crypt",
                    s(Blocks.STONE, Blocks.MOSSY_COBBLESTONE, Blocks.ANDESITE), s(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.GRAVEL),
                    b(Blocks.STONE), b(Blocks.STONE),
                    s(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS),
                    b(Blocks.SHROOMLIGHT), b(Blocks.LANTERN), b(Blocks.CAVE_VINES_PLANT),
                    s(Blocks.MOSS_CARPET, Blocks.COBWEB), Blocks.GLOW_LICHEN.defaultBlockState(), 1.0);
        };
    }

    /** Worlds each rank can roll: index into {@link #world(int)}. */
    private static int[] worldsFor(GateRank rank) {
        return switch (rank) {
            case E -> new int[]{7, 2, 0};
            case D -> new int[]{2, 7, 1, 0};
            case C -> new int[]{0, 1, 3, 5};
            case B -> new int[]{4, 3, 5};
            case A -> new int[]{1, 4, 6};
            case S -> new int[]{6, 4, 5};
        };
    }

    private static EntityType<?>[] mobsFor(GateRank rank) {
        return switch (rank) {
            case E -> new EntityType<?>[]{EntityType.ZOMBIE, EntityType.SPIDER, EntityType.SKELETON};
            case D -> new EntityType<?>[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER};
            case C -> new EntityType<?>[]{EntityType.SKELETON, EntityType.ZOMBIE, EntityType.VINDICATOR, EntityType.SPIDER};
            case B -> new EntityType<?>[]{EntityType.VINDICATOR, EntityType.SKELETON, EntityType.STRAY, EntityType.HUSK};
            case A -> new EntityType<?>[]{EntityType.DROWNED, EntityType.STRAY, EntityType.VINDICATOR, EntityType.EVOKER};
            case S -> new EntityType<?>[]{EntityType.WITHER_SKELETON, EntityType.VINDICATOR, EntityType.BLAZE, EntityType.SKELETON};
        };
    }

    private static BlockState b(Block block) { return block.defaultBlockState(); }

    private static BlockState[] s(Block... blocks) {
        BlockState[] out = new BlockState[blocks.length];
        for (int i = 0; i < blocks.length; i++) out[i] = blocks[i].defaultBlockState();
        return out;
    }

    // ---------------------------------------------------------------- carving

    /** Boolean air grid over the dungeon's bounding box (plus the shell margin). */
    private static final class Grid {
        final int x0, y0, z0, sx, sy, sz;
        final byte[] cells;   // 0 solid/void, 1 air, 2..(1+SHELL) shell depth
        Grid(int x0, int y0, int z0, int x1, int y1, int z1) {
            this.x0 = x0; this.y0 = y0; this.z0 = z0;
            sx = x1 - x0 + 1; sy = y1 - y0 + 1; sz = z1 - z0 + 1;
            cells = new byte[sx * sy * sz];
        }
        boolean in(int x, int y, int z) {
            return x >= x0 && y >= y0 && z >= z0 && x < x0 + sx && y < y0 + sy && z < z0 + sz;
        }
        int idx(int x, int y, int z) { return ((x - x0) * sz + (z - z0)) * sy + (y - y0); }
        byte get(int x, int y, int z) { return in(x, y, z) ? cells[idx(x, y, z)] : 0; }
        void set(int x, int y, int z, byte v) { if (in(x, y, z)) cells[idx(x, y, z)] = v; }
        boolean air(int x, int y, int z) { return get(x, y, z) == 1; }
        boolean solid(int x, int y, int z) { byte v = get(x, y, z); return v >= 2; }
    }

    private static Grid carve(Layout layout, SimplexNoise noise) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Cave c : layout.caves) {
            int r = (int) Math.ceil(c.reach() * 1.25);
            minX = Math.min(minX, c.x - r); maxX = Math.max(maxX, c.x + r);
            minZ = Math.min(minZ, c.z - r); maxZ = Math.max(maxZ, c.z + r);
            minY = Math.min(minY, c.floor - 2); maxY = Math.max(maxY, c.floor + c.h + 3);
        }
        int m = SHELL + 2;
        Grid g = new Grid(minX - m, minY - m, minZ - m, maxX + m, maxY + m, maxZ + m);

        for (Cave c : layout.caves) carveCave(g, c, noise);
        for (Tunnel t : layout.tunnels) {
            carveTunnel(g, t.a, t.bendX, t.bendZ, t, noise, true);
            carveTunnel(g, t.b, t.bendX, t.bendZ, t, noise, false);
        }

        // Shell: SHELL layers of solid around every air cell.
        for (int layer = 0; layer < SHELL; layer++) {
            byte from = (byte) (layer + 1), to = (byte) (layer + 2);
            for (int x = g.x0; x < g.x0 + g.sx; x++)
                for (int z = g.z0; z < g.z0 + g.sz; z++)
                    for (int y = g.y0; y < g.y0 + g.sy; y++) {
                        if (g.get(x, y, z) != 0) continue;
                        if (g.get(x + 1, y, z) == from || g.get(x - 1, y, z) == from || g.get(x, y + 1, z) == from
                                || g.get(x, y - 1, z) == from || g.get(x, y, z + 1) == from || g.get(x, y, z - 1) == from) {
                            g.set(x, y, z, to);
                        }
                    }
        }
        return g;
    }

    private static void carveCave(Grid g, Cave c, SimplexNoise noise) {
        int r = (int) Math.ceil(c.reach() * 1.25);
        boolean flat = c.kind == Kind.BOSS || c.kind == Kind.ENTRANCE;
        for (int x = c.x - r; x <= c.x + r; x++) {
            for (int z = c.z - r; z <= c.z + r; z++) {
                double nx = (x - c.x) / (double) c.rx, nz = (z - c.z) / (double) c.rz;
                double wobble = 0.18 * noise.getValue(x * 0.07, c.floor * 0.07, z * 0.07);
                double d = Math.sqrt(nx * nx + nz * nz) / (1 + wobble);
                if (d >= 1) continue;
                int floorBump = flat ? 0 : (int) Math.round(1.2 * noise.getValue(x * 0.11, 7.3, z * 0.11));
                int top = (int) Math.round(c.h * Math.sqrt(1 - d * d) * (0.9 + 0.2 * noise.getValue(x * 0.09, 3.1, z * 0.09)));
                top = Math.max(top, 4);
                for (int y = c.floor + 1 + Math.max(0, floorBump); y <= c.floor + top; y++) g.set(x, y, z, (byte) 1);
            }
        }
    }

    /** One leg of a tunnel: from cave {@code c}'s centre to the bend point. */
    private static void carveTunnel(Grid g, Cave c, double bx, double bz, Tunnel t, SimplexNoise noise, boolean firstLeg) {
        double ax = c.x, az = c.z;
        double len = Math.hypot(bx - ax, bz - az);
        int steps = (int) Math.ceil(len);
        Cave other = firstLeg ? t.b : t.a;
        for (int i = 0; i <= steps; i++) {
            double f = steps == 0 ? 0 : i / (double) steps;
            double px = ax + (bx - ax) * f, pz = az + (bz - az) * f;
            // Floor ramps from this cave's floor to the midpoint between both floors.
            double floor = c.floor + (((c.floor + other.floor) / 2.0) - c.floor) * f;
            double w = t.w * (1 + 0.25 * noise.getValue(px * 0.1, 11.0, pz * 0.1));
            int ri = (int) Math.ceil(w);
            for (int x = (int) Math.floor(px) - ri; x <= (int) Math.ceil(px) + ri; x++) {
                for (int z = (int) Math.floor(pz) - ri; z <= (int) Math.ceil(pz) + ri; z++) {
                    double d = Math.hypot(x - px, z - pz) / w;
                    if (d >= 1) continue;
                    int fy = (int) Math.round(floor);
                    int top = Math.max(3, (int) Math.round(t.hgt * Math.sqrt(1 - d * d)));
                    for (int y = fy + 1; y <= fy + top; y++) g.set(x, y, z, (byte) 1);
                }
            }
        }
    }

    // ---------------------------------------------------------------- build

    public static void buildAndPopulate(ServerLevel level, int slot, GateRank rank, GateSize size) {
        buildAndPopulate(level, slot, rank, size, false);
    }

    public static void buildAndPopulate(ServerLevel level, int slot, GateRank rank, GateSize size, boolean sea) {
        long started = System.nanoTime();
        Layout layout = layout(slot, size, sea);
        RandomSource random = RandomSource.create(slot * 104729L + rank.ordinal());
        int[] pool = worldsFor(rank);
        World world = world(sea ? 1 : pool[Math.floorMod(slot * 31 + 7, pool.length)]);
        SimplexNoise noise = new SimplexNoise(RandomSource.create(slot * 13L + 5L));
        int water = sea ? waterLevel(slot) : Integer.MIN_VALUE;

        Grid g = carve(layout, noise);
        int placed = placeShell(level, g, world, noise, random);
        if (sea) placed += flood(level, g, water);
        decorate(level, g, layout, world, rank, size, random, water);
        populate(level, slot, layout, g, rank, size, random, sea);

        ProjectZero.LOGGER.info("[Gates] Built {} dungeon slot {} ({} {}): {} blocks in {} ms", world.name, slot,
                rank, size, placed, (System.nanoTime() - started) / 1_000_000);
    }

    private static BlockState pick(BlockState[] palette, SimplexNoise noise, int x, int y, int z, double scale) {
        double v = noise.getValue(x * scale, y * scale * 1.6, z * scale);
        if (palette.length == 1 || v < 0.15) return palette[0];
        if (palette.length == 2 || v < 0.5) return palette[1];
        return palette[2];
    }

    private static int placeShell(ServerLevel level, Grid g, World w, SimplexNoise noise, RandomSource random) {
        int count = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = g.x0; x < g.x0 + g.sx; x++) {
            for (int z = g.z0; z < g.z0 + g.sz; z++) {
                for (int y = g.y0; y < g.y0 + g.sy; y++) {
                    byte v = g.get(x, y, z);
                    if (v < 2) continue;
                    BlockState s;
                    if (v == 2) {
                        if (g.air(x, y + 1, z)) s = pick(w.floors, noise, x, y, z, 0.12);
                        else if (g.air(x, y - 1, z)) s = w.ceiling;
                        else s = pick(w.walls, noise, x, y, z, 0.08);
                    } else {
                        s = v == 2 + SHELL - 1 ? w.deep : pick(w.walls, noise, x, y, z, 0.08);
                    }
                    level.setBlock(p.set(x, y, z), s, FLAGS);
                    count++;
                }
            }
        }
        return count;
    }

    /** Fills every cave cell at or below {@code water} with still water. */
    private static int flood(ServerLevel level, Grid g, int water) {
        BlockState w = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int n = 0;
        for (int x = g.x0; x < g.x0 + g.sx; x++)
            for (int z = g.z0; z < g.z0 + g.sz; z++)
                for (int y = g.y0; y <= Math.min(water, g.y0 + g.sy - 1); y++)
                    if (g.air(x, y, z)) { level.setBlock(p.set(x, y, z), w, FLAGS); n++; }
        return n;
    }

    // ---------------------------------------------------------------- decoration

    private static void decorate(ServerLevel level, Grid g, Layout layout, World w, GateRank rank, GateSize size,
                                 RandomSource random, int water) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        Direction[] dirs = Direction.values();

        // Surface pass: lights, growth, flora, crystal seeds.
        List<BlockPos> crystalSeeds = new ArrayList<>();
        for (int x = g.x0; x < g.x0 + g.sx; x++) {
            for (int z = g.z0; z < g.z0 + g.sz; z++) {
                for (int y = g.y0; y < g.y0 + g.sy; y++) {
                    if (!g.air(x, y, z)) continue;
                    boolean wet = y <= water;
                    boolean floorBelow = g.solid(x, y - 1, z);
                    if (wet) {
                        // Underwater: seagrass, kelp and sea pickles on the floor, nothing on walls.
                        if (floorBelow) {
                            int roll = random.nextInt(1000);
                            if (roll < 12) {
                                level.setBlock(p.set(x, y, z), Blocks.SEA_PICKLE.defaultBlockState()
                                        .setValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES, 1 + random.nextInt(4)), FLAGS);
                            } else if (roll < 140) {
                                level.setBlock(p.set(x, y, z), Blocks.SEAGRASS.defaultBlockState(), FLAGS);
                            } else if (roll < 175) {
                                int top = y;
                                while (top + 1 <= water - 1 && g.air(x, top + 1, z) && top - y < 6) top++;
                                for (int ky = y; ky < top; ky++) level.setBlock(p.set(x, ky, z), Blocks.KELP_PLANT.defaultBlockState(), FLAGS);
                                level.setBlock(p.set(x, top, z), Blocks.KELP.defaultBlockState(), FLAGS);
                            }
                        }
                        if (random.nextInt(10000) < 15 * w.crystals) {
                            for (Direction d : dirs) {
                                if (d.getAxis() != Direction.Axis.Y && g.solid(x + d.getStepX(), y, z + d.getStepZ())) {
                                    crystalSeeds.add(new BlockPos(x, y, z));
                                    break;
                                }
                            }
                        }
                        continue;
                    }
                    boolean ceilingAbove = g.solid(x, y + 1, z);
                    int roll = random.nextInt(1000);
                    if (ceilingAbove && !floorBelow) {
                        if (roll < 7) {
                            level.setBlock(p.set(x, y + 1, z), w.ceilingLight, FLAGS);
                        } else if (roll < 14 && w.hanging != null && g.air(x, y - 1, z)) {
                            level.setBlock(p.set(x, y, z), w.hanging, FLAGS);
                        } else if (roll < 22 && g.air(x, y - 1, z) && g.air(x, y - 2, z) && y - 2 > water) {
                            level.setBlock(p.set(x, y, z), Blocks.CHAIN.defaultBlockState(), FLAGS);
                            level.setBlock(p.set(x, y - 1, z), hangingLantern(w), FLAGS);
                        }
                    } else if (floorBelow && !ceilingAbove) {
                        if (roll < 4) {
                            level.setBlock(p.set(x, y, z), w.floorLight, FLAGS);
                        } else if (roll < 40 && w.flora.length > 0) {
                            BlockState f = w.flora[random.nextInt(w.flora.length)];
                            level.setBlock(p.set(x, y, z), f, FLAGS);
                        }
                    }
                    // Walls: growth (lichen / sculk veins) and crystal seeds.
                    for (Direction d : dirs) {
                        if (d.getAxis() == Direction.Axis.Y) continue;
                        if (!g.solid(x + d.getStepX(), y, z + d.getStepZ())) continue;
                        if (w.wallGrowth != null && random.nextInt(1000) < 25) {
                            level.setBlock(p.set(x, y, z),
                                    w.wallGrowth.setValue(MultifaceBlock.getFaceProperty(d), true), FLAGS);
                        }
                        if (random.nextInt(10000) < 22 * w.crystals) crystalSeeds.add(new BlockPos(x, y, z));
                        break;
                    }
                }
            }
        }

        // Mana crystal clusters around each seed: 4-9 crystals on nearby surfaces.
        BlockState crystal = ModBlocks.MANA_CRYSTAL.get().defaultBlockState();
        for (BlockPos seed : crystalSeeds) {
            int n = 4 + random.nextInt(6);
            for (int i = 0; i < n * 3 && n > 0; i++) {
                int x = seed.getX() + random.nextInt(5) - 2, y = seed.getY() + random.nextInt(5) - 2,
                        z = seed.getZ() + random.nextInt(5) - 2;
                if (!g.air(x, y, z)) continue;
                for (Direction d : dirs) {
                    if (!g.solid(x - d.getStepX(), y - d.getStepY(), z - d.getStepZ())) continue;
                    level.setBlock(p.set(x, y, z), crystal.setValue(AmethystClusterBlock.FACING, d)
                            .setValue(AmethystClusterBlock.WATERLOGGED, y <= water), FLAGS);
                    g.set(x, y, z, (byte) 0);   // occupied
                    // The rock it grows from turns to calcite, like a geode lining.
                    level.setBlock(p.set(x - d.getStepX(), y - d.getStepY(), z - d.getStepZ()),
                            Blocks.CALCITE.defaultBlockState(), FLAGS);
                    n--;
                    break;
                }
            }
        }

        // Ruins and chests per cave.
        for (Cave c : layout.caves) {
            switch (c.kind) {
                case ENTRANCE -> decorateEntrance(level, g, c, w, rank, random);
                case CHAMBER, SIDE -> {
                    int ruins = 1 + random.nextInt(c.kind == Kind.CHAMBER ? 3 : 2);
                    for (int i = 0; i < ruins; i++) ruin(level, g, c, w, random);
                    if (random.nextFloat() < (c.kind == Kind.SIDE ? 0.6F : 0.3F)) chestNearWall(level, g, c, rank, random, water);
                }
                case TREASURE -> {
                    for (int i = 0; i < 2 + random.nextInt(2); i++) chestNearWall(level, g, c, rank, random, water);
                    // A treasure cave glitters: extra crystals ring its walls.
                    for (int i = 0; i < 40; i++) {
                        double a = random.nextDouble() * Math.PI * 2;
                        int x = c.x + (int) Math.round(Math.sin(a) * (c.rx - 1)), z = c.z + (int) Math.round(Math.cos(a) * (c.rz - 1));
                        int y = c.floor + 1 + random.nextInt(4);
                        if (g.air(x, y, z)) {
                            for (Direction d : dirs) {
                                if (g.solid(x - d.getStepX(), y - d.getStepY(), z - d.getStepZ())) {
                                    level.setBlock(p.set(x, y, z), crystal.setValue(AmethystClusterBlock.FACING, d)
                                            .setValue(AmethystClusterBlock.WATERLOGGED, y <= water), FLAGS);
                                    g.set(x, y, z, (byte) 0);
                                    break;
                                }
                            }
                        }
                    }
                }
                case BOSS -> decorateBoss(level, g, c, w, rank, random, water);
            }
        }
    }

    private static BlockState hangingLantern(World w) {
        BlockState l = w.floorLight;
        return l.getBlock() instanceof LanternBlock ? l.setValue(LanternBlock.HANGING, true)
                : Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    /** First air cell above the floor at (x, z), searching up from around {@code fromY}. */
    private static int floorAt(Grid g, int x, int z, int fromY) {
        for (int y = fromY - 3; y <= fromY + 8; y++) {
            if (g.air(x, y, z) && g.solid(x, y - 1, z)) return y;
        }
        return Integer.MIN_VALUE;
    }

    /** Broken remnants of whoever lived in this world: a pillar, an arch stub or a wall piece. */
    private static void ruin(ServerLevel level, Grid g, Cave c, World w, RandomSource random) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double a = random.nextDouble() * Math.PI * 2;
        double rr = 0.35 + random.nextDouble() * 0.4;
        int x = c.x + (int) Math.round(Math.sin(a) * c.rx * rr), z = c.z + (int) Math.round(Math.cos(a) * c.rz * rr);
        int y = floorAt(g, x, z, c.floor + 1);
        if (y == Integer.MIN_VALUE) return;
        int type = random.nextInt(3);
        if (type == 0) {
            // Pillar, snapped off; rubble at its foot.
            int h = 3 + random.nextInt(Math.max(1, c.h - 4));
            for (int dy = 0; dy < h && g.air(x, y + dy, z); dy++) {
                level.setBlock(p.set(x, y + dy, z), w.ruin[dy == 0 ? 0 : random.nextInt(w.ruin.length)], FLAGS);
            }
            for (int i = 0; i < 3; i++) {
                int rx = x + random.nextInt(5) - 2, rz = z + random.nextInt(5) - 2;
                int ry = floorAt(g, rx, rz, y);
                if (ry != Integer.MIN_VALUE && (rx != x || rz != z)) level.setBlock(p.set(rx, ry, rz), w.ruin[random.nextInt(w.ruin.length)], FLAGS);
            }
        } else if (type == 1) {
            // Half an arch: two legs, a lintel that breaks off.
            boolean alongX = random.nextBoolean();
            int span = 4 + random.nextInt(3), h = 4 + random.nextInt(2);
            for (int s = 0; s <= span; s++) {
                int ax = alongX ? x + s : x, az = alongX ? z : z + s;
                boolean leg = s == 0 || s == span;
                for (int dy = 0; dy <= h; dy++) {
                    if (!leg && dy < h) continue;
                    if (s == span && dy > h - 2 - random.nextInt(2)) continue;   // broken end
                    if (g.air(ax, y + dy, az)) level.setBlock(p.set(ax, y + dy, az), w.ruin[random.nextInt(w.ruin.length)], FLAGS);
                }
            }
        } else {
            // A wall fragment with a gap.
            boolean alongX = random.nextBoolean();
            int len = 4 + random.nextInt(4), h = 2 + random.nextInt(3);
            int gap = 1 + random.nextInt(Math.max(1, len - 2));
            for (int s = 0; s < len; s++) {
                if (s == gap) continue;
                int ax = alongX ? x + s : x, az = alongX ? z : z + s;
                int top = h - (s > gap ? random.nextInt(2) : 0);
                for (int dy = 0; dy < top; dy++) {
                    if (g.air(ax, y + dy, az)) level.setBlock(p.set(ax, y + dy, az), w.ruin[random.nextInt(w.ruin.length)], FLAGS);
                }
            }
        }
    }

    private static void chestNearWall(ServerLevel level, Grid g, Cave c, GateRank rank, RandomSource random, int water) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double a = random.nextDouble() * Math.PI * 2;
            double rr = 0.55 + random.nextDouble() * 0.35;
            int x = c.x + (int) Math.round(Math.sin(a) * c.rx * rr), z = c.z + (int) Math.round(Math.cos(a) * c.rz * rr);
            int y = floorAt(g, x, z, c.floor + 1);
            if (y == Integer.MIN_VALUE || !g.air(x, y + 1, z)) continue;
            Direction facing = Direction.fromYRot(Math.toDegrees(Math.atan2(x - c.x, c.z - z)));   // toward the centre
            placeChest(level, new BlockPos(x, y, z), facing, rank, random, y <= water);
            g.set(x, y, z, (byte) 0);
            return;
        }
    }

    private static void placeChest(ServerLevel level, BlockPos pos, Direction facing, GateRank rank, RandomSource random,
                                   boolean wet) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing)
                .setValue(ChestBlock.WATERLOGGED, wet), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.withDefaultNamespace(rank.ordinal() >= GateRank.B.ordinal()
                            ? "chests/stronghold_corridor" : "chests/simple_dungeon"));
            chest.setLootTable(table, random.nextLong());
            Container box = chest;
            if (random.nextFloat() < 0.40F) {
                box.setItem(4, EquipmentRules.rollItem(random, EquipmentRules.randomDroppableSlot(random),
                        EquipmentRules.rollStandardRarity(random, false), rank));
            }
            if (random.nextFloat() < 0.08F + rank.ordinal() * 0.03F) {
                box.setItem(13, new ItemStack(ModItems.AWAKENING_SHARD.get()));
            }
            box.setItem(22, new ItemStack(ModItems.MANA_CRYSTAL_SHARD.get(), 1 + random.nextInt(3 + rank.ordinal())));
        }
    }

    private static void decorateEntrance(ServerLevel level, Grid g, Cave c, World w, GateRank rank, RandomSource random) {
        if (rank == GateRank.S) return;   // red gates: no way back until the boss falls
        // Return rift at the back of the entrance cavern, facing into it.
        BlockPos base = new BlockPos(c.x, c.floor + 2, c.z - c.rz + 4);
        BlockState portal = ModBlocks.GATE_PORTAL.get().defaultBlockState()
                .setValue(GateBlock.RANK, rank).setValue(GateBlock.EXIT, true);
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos p = base.offset(dx, dy, 0);
                level.setBlock(p, portal, Block.UPDATE_ALL);
                g.set(p.getX(), p.getY(), p.getZ(), (byte) 0);
                if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                    be.configure(rank, GateSize.SMALL, level.getGameTime(), -1, base);
                }
            }
        }
    }

    private static void decorateBoss(ServerLevel level, Grid g, Cave c, World w, GateRank rank, RandomSource random, int water) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // Rank-colored ring in the floor around the centre.
        BlockState glow = rankGlass(rank);
        for (int k = 0; k < 72; k++) {
            double a = k * Math.PI * 2 / 72;
            for (int r : new int[]{7, 8}) {
                level.setBlock(p.set(c.x + (int) Math.round(Math.cos(a) * r), c.floor, c.z + (int) Math.round(Math.sin(a) * r)), glow, FLAGS);
            }
        }
        // Ring of tall broken pillars.
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI * 2 / 10;
            int x = c.x + (int) Math.round(Math.cos(a) * (c.rx - 6)), z = c.z + (int) Math.round(Math.sin(a) * (c.rz - 6));
            int top = 6 + random.nextInt(c.h - 8);
            for (int dy = 1; dy <= top; dy++) {
                if (g.air(x, c.floor + dy, z)) level.setBlock(p.set(x, c.floor + dy, z), w.ruin[dy == 1 ? 0 : random.nextInt(w.ruin.length)], FLAGS);
            }
        }
        // Dais where the exit rift will open. In a sea dungeon it is an island rising out of the water.
        boolean sea = water != Integer.MIN_VALUE;
        int daisTop = sea ? water : c.floor + 1;
        BlockPos exit = new BlockPos(c.x, daisTop + 1, c.z + c.rz - 7);
        for (int x = exit.getX() - 3; x <= exit.getX() + 4; x++)
            for (int z = exit.getZ() - 3; z <= exit.getZ() + 3; z++)
                for (int y = c.floor + 1; y <= daisTop; y++)
                    if (g.air(x, y, z)) { level.setBlock(p.set(x, y, z), w.ruin[0], FLAGS); g.set(x, y, z, (byte) 2); }
        // Reward chest behind the rift.
        placeChest(level, new BlockPos(exit.getX(), daisTop + 1, exit.getZ() + 3), Direction.NORTH, rank, random, false);
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

    private static void populate(ServerLevel level, int slot, Layout layout, Grid g, GateRank rank,
                                 GateSize size, RandomSource random, boolean sea) {
        EntityType<?>[] mobs = sea ? new EntityType<?>[]{EntityType.DROWNED, EntityType.DROWNED, EntityType.GUARDIAN} : mobsFor(rank);
        List<Cave> fights = new ArrayList<>();
        for (Cave c : layout.caves) if (c.kind == Kind.CHAMBER || c.kind == Kind.SIDE) fights.add(c);
        int total = size.mobCount() + fights.size() * 3;
        for (int i = 0; i < total && !fights.isEmpty(); i++) {
            Cave c = fights.get(i % fights.size());
            EntityType<?> type = mobs[random.nextInt(mobs.length)];
            if (!(type.create(level) instanceof Mob mob)) continue;
            BlockPos spot = spawnSpot(g, c, random);
            if (spot == null) continue;
            mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
            mob.setPersistenceRequired();
            mob.getPersistentData().putBoolean(MobScaling.GATE_MONSTER_KEY, true);
            mob.getPersistentData().putInt(DUNGEON_SLOT_KEY, slot);
            MobScaling.apply(mob, rank, rank.randomLevel(random));
            level.addFreshEntity(mob);
        }

        Cave bossCave = layout.boss();
        EntityType<? extends Mob> bossType = sea
                ? (rank.ordinal() >= GateRank.A.ordinal() ? EntityType.ELDER_GUARDIAN : EntityType.DROWNED)
                : switch (rank) {
                    case E, D -> EntityType.HUSK;
                    case C, B -> EntityType.VINDICATOR;
                    case A, S -> EntityType.WITHER_SKELETON;
                };
        Mob boss = bossType.create(level);
        if (boss != null) {
            boss.moveTo(bossCave.x + 0.5, bossCave.floor + 1.0, bossCave.z + 0.5, 180.0F, 0.0F);
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

    private static BlockPos spawnSpot(Grid g, Cave c, RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double a = random.nextDouble() * Math.PI * 2, rr = random.nextDouble() * 0.7;
            int x = c.x + (int) Math.round(Math.sin(a) * c.rx * rr), z = c.z + (int) Math.round(Math.cos(a) * c.rz * rr);
            int y = floorAt(g, x, z, c.floor + 1);
            if (y != Integer.MIN_VALUE && g.air(x, y + 1, z)) return new BlockPos(x, y, z);
        }
        return null;
    }

    public static void openExitPortal(ServerLevel level, int slot, GateSize size, GateRank rank) {
        openExitPortal(level, slot, size, rank, false);
    }

    public static void openExitPortal(ServerLevel level, int slot, GateSize size, GateRank rank, boolean sea) {
        BlockPos exitBase = exitPortalCenter(slot, size, sea);
        BlockState portal = ModBlocks.GATE_PORTAL.get().defaultBlockState()
                .setValue(GateBlock.RANK, rank)
                .setValue(GateBlock.EXIT, true);
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos p = exitBase.offset(dx, dy, 0);
                level.setBlock(p, portal, Block.UPDATE_ALL);
                if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                    be.configure(rank, size, level.getGameTime(), slot, exitBase);
                }
            }
        }
    }
}
