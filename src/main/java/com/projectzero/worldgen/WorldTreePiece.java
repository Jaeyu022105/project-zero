package com.projectzero.worldgen;

import com.projectzero.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The World Tree's blocks. The whole tree is rebuilt from its seed as simple shapes (tapered
 * capsules for wood, spheres for leaf clouds) and each chunk only draws the part inside it, so the
 * tree can be any size. A trunk 130 to 170 tall flaring to about 26 across at the base, giant roots
 * arching out and diving into the ground, 14 to 20 branches spiralling up the trunk with leaf
 * clouds at their tips, a vast crown, and glowing berry vines hanging from the leaves.
 */
public class WorldTreePiece extends StructurePiece {
    private static final int REACH = 72, DOWN = 24, UP = 210;
    private static final BlockState WOOD = Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState();
    private static final BlockState ROOT = Blocks.BIRCH_WOOD.defaultBlockState();
    private static final BlockState LEAVES = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

    private final BlockPos base;
    private final long seed;

    public WorldTreePiece(BlockPos base, long seed) {
        super(ModStructures.WORLD_TREE_PIECE.get(), 0, new BoundingBox(base.getX() - REACH, base.getY() - DOWN, base.getZ() - REACH,
                base.getX() + REACH, base.getY() + UP, base.getZ() + REACH));
        this.base = base;
        this.seed = seed;
    }

    public WorldTreePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.WORLD_TREE_PIECE.get(), tag);
        this.base = new BlockPos(tag.getInt("bx"), tag.getInt("by"), tag.getInt("bz"));
        this.seed = tag.getLong("seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("bx", base.getX());
        tag.putInt("by", base.getY());
        tag.putInt("bz", base.getZ());
        tag.putLong("seed", seed);
    }

    /** A tapered capsule from a to b. */
    private record Seg(double ax, double ay, double az, double bx, double by, double bz, double r0, double r1, BlockState state) {}
    private record Ball(double x, double y, double z, double r) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        List<Seg> wood = new ArrayList<>();
        List<Ball> leaves = new ArrayList<>();
        build(wood, leaves);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Seg s : wood) drawSeg(level, box, s, p);
        for (Ball b : leaves) drawBall(level, box, b, p);
        for (Ball b : leaves) vines(level, box, b, p);
    }

    private void build(List<Seg> wood, List<Ball> leaves) {
        Random r = new Random(seed);
        double bx = base.getX() + 0.5, by = base.getY(), bz = base.getZ() + 0.5;
        int height = 130 + r.nextInt(41);
        double twist = r.nextDouble() * Math.PI * 2;
        // Trunk: stacked tapered sections, flared at the base, drifting slightly as it climbs.
        double px = bx, pz = bz;
        for (int y = -6; y < height; y += 4) {
            double t = Math.max(0, y) / (double) height;
            double r0 = radius(t, y), r1 = radius(Math.min(1, (y + 4.0) / height), y + 4);
            double nx = bx + Math.sin(t * 3 + twist) * 4 * t, nz = bz + Math.cos(t * 2.5 + twist) * 4 * t;
            wood.add(new Seg(px, by + y, pz, nx, by + y + 4, nz, r0, r1, WOOD));
            px = nx; pz = nz;
        }
        double topX = px, topZ = pz;
        // Roots: arch out from the flare and dive into the ground.
        int roots = 8 + r.nextInt(4);
        for (int i = 0; i < roots; i++) {
            double a = i * Math.PI * 2 / roots + r.nextDouble() * 0.4;
            double len = 18 + r.nextDouble() * 14;
            double cx = bx + Math.cos(a) * 9, cz = bz + Math.sin(a) * 9, cy = by + 6 + r.nextDouble() * 4;
            double rad = 3.2;
            for (int k = 1; k <= 6; k++) {
                double t = k / 6.0;
                double nx = bx + Math.cos(a) * (9 + len * t), nz = bz + Math.sin(a) * (9 + len * t);
                double ny = by + 6 - 14 * t * t + Math.sin(t * Math.PI) * 4;
                double nr = 3.2 * (1 - t * 0.7);
                wood.add(new Seg(cx, cy, cz, nx, ny, nz, rad, nr, ROOT));
                cx = nx; cy = ny; cz = nz; rad = nr;
            }
        }
        // Branches spiralling up the trunk.
        int branches = 14 + r.nextInt(7);
        double ang = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < branches; i++) {
            double t = 0.3 + 0.62 * i / (branches - 1.0);
            ang += 2.4 + r.nextDouble() * 0.3;   // close to the golden angle: a spiral staircase of limbs
            int y = (int) (height * t);
            double sx = bx + Math.sin(t * 3 + twist) * 4 * t, sz = bz + Math.cos(t * 2.5 + twist) * 4 * t;
            double len = (38 - 18 * t) * (0.8 + r.nextDouble() * 0.4);
            double rise = 0.25 + r.nextDouble() * 0.3;
            double cx = sx, cy = by + y, cz = sz, rad = 3.4 - 1.6 * t;
            double a = ang;
            for (int k = 1; k <= 6; k++) {
                double f = k / 6.0;
                a += 0.12;   // each limb curls along the spiral
                double nx = sx + Math.cos(a) * len * f, nz = sz + Math.sin(a) * len * f;
                double ny = by + y + len * f * rise + f * f * 6;
                double nr = Math.max(1.0, rad * (1 - 0.13 * k));
                wood.add(new Seg(cx, cy, cz, nx, ny, nz, rad, nr, WOOD));
                if (k >= 4) leaves.add(new Ball(nx, ny + 1, nz, 4 + r.nextDouble() * 2));
                cx = nx; cy = ny; cz = nz; rad = nr;
            }
            leaves.add(new Ball(cx, cy + 2, cz, 6.5 + r.nextDouble() * 3));
        }
        // Crown.
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2 / 8;
            double d = i == 8 ? 0 : 9 + r.nextDouble() * 5;
            leaves.add(new Ball(topX + Math.cos(a) * d, by + height + 2 - (i == 8 ? -4 : r.nextInt(5)), topZ + Math.sin(a) * d, 9 + r.nextDouble() * 4));
        }
    }

    private static double radius(double t, int y) {
        double r = 8.5 - 6 * t;
        if (y < 8) r += (8 - y) * 0.55;   // flare
        return Math.max(2.5, r);
    }

    private static void drawSeg(WorldGenLevel level, BoundingBox box, Seg s, BlockPos.MutableBlockPos p) {
        double rmax = Math.max(s.r0, s.r1);
        int minX = Mth.floor(Math.min(s.ax, s.bx) - rmax), maxX = Mth.ceil(Math.max(s.ax, s.bx) + rmax);
        int minY = Mth.floor(Math.min(s.ay, s.by) - rmax), maxY = Mth.ceil(Math.max(s.ay, s.by) + rmax);
        int minZ = Mth.floor(Math.min(s.az, s.bz) - rmax), maxZ = Mth.ceil(Math.max(s.az, s.bz) + rmax);
        minX = Math.max(minX, box.minX()); maxX = Math.min(maxX, box.maxX());
        minY = Math.max(minY, box.minY()); maxY = Math.min(maxY, box.maxY());
        minZ = Math.max(minZ, box.minZ()); maxZ = Math.min(maxZ, box.maxZ());
        if (minX > maxX || minY > maxY || minZ > maxZ) return;
        double dx = s.bx - s.ax, dy = s.by - s.ay, dz = s.bz - s.az;
        double len2 = dx * dx + dy * dy + dz * dz;
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            double qx = x + 0.5 - s.ax, qy = y + 0.5 - s.ay, qz = z + 0.5 - s.az;
            double t = len2 == 0 ? 0 : Mth.clamp((qx * dx + qy * dy + qz * dz) / len2, 0, 1);
            double ex = qx - dx * t, ey = qy - dy * t, ez = qz - dz * t;
            double rr = s.r0 + (s.r1 - s.r0) * t;
            if (ex * ex + ey * ey + ez * ez > rr * rr) continue;
            p.set(x, y, z);
            BlockState cur = level.getBlockState(p);
            if (cur.is(Blocks.BEDROCK) || cur.is(s.state.getBlock())) continue;
            level.setBlock(p, s.state, 2);
        }
    }

    private static void drawBall(WorldGenLevel level, BoundingBox box, Ball b, BlockPos.MutableBlockPos p) {
        int minX = Math.max(Mth.floor(b.x - b.r), box.minX()), maxX = Math.min(Mth.ceil(b.x + b.r), box.maxX());
        int minY = Math.max(Mth.floor(b.y - b.r * 0.7), box.minY()), maxY = Math.min(Mth.ceil(b.y + b.r * 0.7), box.maxY());
        int minZ = Math.max(Mth.floor(b.z - b.r), box.minZ()), maxZ = Math.min(Mth.ceil(b.z + b.r), box.maxZ());
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            double fx = x + 0.5 - b.x, fy = (y + 0.5 - b.y) / 0.7, fz = z + 0.5 - b.z;   // flattened cloud
            double d2 = fx * fx + fy * fy + fz * fz;
            if (d2 > b.r * b.r) continue;
            // Ragged edge: a stable per-block hash thins the outer shell.
            if (d2 > (b.r - 1.5) * (b.r - 1.5) && hash(x, y, z) % 100 < 35) continue;
            p.set(x, y, z);
            BlockState cur = level.getBlockState(p);
            if (cur.isAir() || cur.canBeReplaced() && cur.getFluidState().isEmpty()) level.setBlock(p, LEAVES, 2);
        }
    }

    private static void vines(WorldGenLevel level, BoundingBox box, Ball b, BlockPos.MutableBlockPos p) {
        int minX = Math.max(Mth.floor(b.x - b.r), box.minX()), maxX = Math.min(Mth.ceil(b.x + b.r), box.maxX());
        int minZ = Math.max(Mth.floor(b.z - b.r), box.minZ()), maxZ = Math.min(Mth.ceil(b.z + b.r), box.maxZ());
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            double fx = x + 0.5 - b.x, fz = z + 0.5 - b.z;
            double h = b.r * b.r - fx * fx - fz * fz;
            if (h < 0) continue;
            int bottom = Mth.floor(b.y - Math.sqrt(h) * 0.7);
            int roll = hash(x, 7, z) % 100;
            if (roll >= 9) continue;
            int len = 3 + hash(z, 3, x) % 10;
            for (int k = 1; k <= len; k++) {
                int y = bottom - k;
                if (y < box.minY() || y > box.maxY()) break;
                p.set(x, y, z);
                if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).is(BlockTags.LEAVES) && k == 1) break;
                boolean last = k == len || !level.getBlockState(p.below()).isAir();
                level.setBlock(p, (last ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                        .setValue(CaveVines.BERRIES, roll < 4), 2);
                if (last) break;
            }
        }
    }

    private static int hash(int x, int y, int z) {
        int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
        h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
        return h & 0x7fffffff;
    }
}
