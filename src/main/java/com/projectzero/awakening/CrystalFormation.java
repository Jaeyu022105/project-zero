package com.projectzero.awakening;

import com.projectzero.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds an Awakening Stone formation: a huge spiky crystal "iceberg" made of crystal blocks,
 * with glowing core veins in its lower part. Procedural, so every formation looks different.
 * Works on a live level (commands) and during world generation (any {@link LevelAccessor}).
 */
public final class CrystalFormation {
    private CrystalFormation() {}

    /** Formation sizes. Villages pick one; bigger villages get bigger stones (DESIGN-DOCUMENT.md §6). */
    public enum Size {
        //        main height    main radius   side spikes   spike length   spike radius  core chance
        SMALL(6, 8, 1.6, 3, 4, 3, 5, 1.1, 0.10),
        LARGE(14, 18, 3.0, 5, 7, 7, 11, 1.8, 0.07),
        COLOSSAL(26, 32, 5.0, 8, 11, 12, 20, 2.8, 0.05);

        final int minHeight, maxHeight;
        final double mainRadius;
        final int minSpikes, maxSpikes;
        final int minSpikeLength, maxSpikeLength;
        final double spikeRadius;
        final double coreChance;

        Size(int minHeight, int maxHeight, double mainRadius, int minSpikes, int maxSpikes,
             int minSpikeLength, int maxSpikeLength, double spikeRadius, double coreChance) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.mainRadius = mainRadius;
            this.minSpikes = minSpikes;
            this.maxSpikes = maxSpikes;
            this.minSpikeLength = minSpikeLength;
            this.maxSpikeLength = maxSpikeLength;
            this.spikeRadius = spikeRadius;
            this.coreChance = coreChance;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Horizontal distance from the center to the edge of the formation, roughly. */
        public int footprintRadius() {
            return (int) Math.ceil(mainRadius + maxSpikeLength * 0.6);
        }
    }

    /**
     * Places a formation with its base centered on {@code ground} (the first air block above
     * the ground). The base sinks 2 blocks into the ground so it looks rooted.
     */
    public static void place(LevelAccessor level, BlockPos ground, Size size, RandomSource random) {
        PLACED.get().clear();
        BlockPos base = ground.below(2);
        int height = Mth.nextInt(random, size.minHeight, size.maxHeight) + 2;

        // Main spire: slight random lean.
        Vec3 mainDir = new Vec3((random.nextDouble() - 0.5) * 0.25, 1.0, (random.nextDouble() - 0.5) * 0.25).normalize();
        spike(level, base, mainDir, height, size.mainRadius, size, random);

        // Side spikes fanning out around the base.
        int spikes = Mth.nextInt(random, size.minSpikes, size.maxSpikes);
        double angleStep = Math.PI * 2 / spikes;
        for (int i = 0; i < spikes; i++) {
            double angle = i * angleStep + (random.nextDouble() - 0.5) * angleStep * 0.6;
            double tilt = Math.toRadians(20 + random.nextDouble() * 30);    // 20-50° from vertical
            Vec3 dir = new Vec3(Math.cos(angle) * Math.sin(tilt), Math.cos(tilt), Math.sin(angle) * Math.sin(tilt));
            double offset = size.mainRadius * (0.4 + random.nextDouble() * 0.5);
            BlockPos start = base.offset((int) Math.round(Math.cos(angle) * offset), random.nextInt(2),
                    (int) Math.round(Math.sin(angle) * offset));
            int length = Mth.nextInt(random, size.minSpikeLength, size.maxSpikeLength);
            double radius = size.spikeRadius * (0.75 + random.nextDouble() * 0.5);
            spike(level, start, dir, length, radius, size, random);
        }

        // Colossal formations get a ring of small shards around them, like broken-off pieces.
        if (size == Size.COLOSSAL) {
            for (int i = 0; i < 6; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = size.footprintRadius() * (0.8 + random.nextDouble() * 0.4);
                BlockPos start = base.offset((int) (Math.cos(angle) * dist), 1, (int) (Math.sin(angle) * dist));
                Vec3 dir = new Vec3(Math.cos(angle) * 0.4, 1.0, Math.sin(angle) * 0.4).normalize();
                fillSpike(level, start, dir, Mth.nextInt(random, 3, 6), 1.0, size, random);
            }
        }

        finish(level, random);

        // Fill under the base so it never floats over a dip.
        int r = (int) Math.ceil(size.mainRadius) + 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                BlockPos.MutableBlockPos p = base.offset(dx, -1, dz).mutable();
                for (int depth = 0; depth < 6 && canReplace(level, p) && level.getBlockState(p).isAir(); depth++) {
                    level.setBlock(p, crystal(size, random, 0), 2);
                    p.move(0, -1, 0);
                }
            }
        }
    }

    /** A spike plus its branch needles (owner idea: sharp straight spikes growing out of the big ones). */
    private static void spike(LevelAccessor level, BlockPos start, Vec3 dir, int length, double radius,
                              Size size, RandomSource random) {
        fillSpike(level, start, dir, length, radius, size, random);
        if (length < 5) return;     // tiny shards stay plain

        // 1 branch per ~5 blocks of length: thin, straight and sharp, angled out of the parent.
        int branches = Math.max(1, length / 5) + random.nextInt(2);
        Vec3 origin = Vec3.atCenterOf(start);
        for (int i = 0; i < branches; i++) {
            double along = length * (0.25 + random.nextDouble() * 0.5);
            Vec3 from = origin.add(dir.scale(along));
            Vec3 branchDir = branchDirection(dir, random);
            // Start on the parent's surface so the needle grows out of it.
            double parentRadius = radius * Math.pow(1.0 - along / length, 0.85);
            from = from.add(branchDir.scale(Math.max(0, parentRadius - 0.5)));
            int branchLength = Math.max(3, (int) Math.round(length * (0.25 + random.nextDouble() * 0.2)));
            double branchRadius = Math.max(0.6, radius * 0.3);
            fillSpike(level, BlockPos.containing(from), branchDir, branchLength, branchRadius, size, random);
        }
    }

    /** A direction 45-70° away from the parent, around it at a random angle, never pointing down. */
    private static Vec3 branchDirection(Vec3 parent, RandomSource random) {
        Vec3 helper = Math.abs(parent.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 side1 = parent.cross(helper).normalize();
        Vec3 side2 = parent.cross(side1).normalize();
        double around = random.nextDouble() * Math.PI * 2;
        Vec3 out = side1.scale(Math.cos(around)).add(side2.scale(Math.sin(around)));
        double tilt = Math.toRadians(45 + random.nextDouble() * 25);
        Vec3 d = parent.scale(Math.cos(tilt)).add(out.scale(Math.sin(tilt))).normalize();
        if (d.y < 0.1) d = new Vec3(d.x, 0.1, d.z).normalize();
        return d;
    }

    /** Fills a tapering cone along {@code dir} from {@code start}. */
    private static void fillSpike(LevelAccessor level, BlockPos start, Vec3 dir, int length, double radius,
                                  Size size, RandomSource random) {
        Vec3 origin = Vec3.atCenterOf(start);
        for (double t = 0; t <= length; t += 0.5) {
            double progress = t / length;
            double r = radius * Math.pow(1.0 - progress, 0.85);     // fat base, sharp tip
            if (r < 0.35) r = progress < 1.0 ? 0.35 : 0;
            Vec3 center = origin.add(dir.scale(t));
            int ir = (int) Math.ceil(r);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dy = -ir; dy <= ir; dy++) {
                    for (int dz = -ir; dz <= ir; dz++) {
                        if (dx * dx + dy * dy + dz * dz > r * r + 0.25) continue;
                        BlockPos p = BlockPos.containing(center.x + dx, center.y + dy, center.z + dz);
                        if (canReplace(level, p)) {
                            level.setBlock(p, crystal(size, random, progress), 2);
                            PLACED.get().add(p.immutable());
                        }
                    }
                }
            }
        }
    }

    /** Positions placed by the current {@link #place} call (per thread, so worldgen threads don't mix). */
    private static final ThreadLocal<Set<BlockPos>> PLACED = ThreadLocal.withInitial(HashSet::new);

    private static final double CLUSTER_CHANCE = 0.08;

    /**
     * Second pass over the finished shape:
     *   - crystal with no air next to it becomes solid deep crystal, so the formation is
     *     see-through only one block deep;
     *   - some exposed faces grow a spiky cluster.
     */
    private static void finish(LevelAccessor level, RandomSource random) {
        Set<BlockPos> placed = PLACED.get();
        BlockState deep = ModBlocks.AWAKENING_CRYSTAL_DEEP.get().defaultBlockState();
        List<BlockPos> interior = new ArrayList<>();
        List<BlockPos> clusterSpots = new ArrayList<>();
        List<Direction> clusterFaces = new ArrayList<>();

        for (BlockPos p : placed) {
            if (!level.getBlockState(p).is(ModBlocks.AWAKENING_CRYSTAL.get())) continue;
            boolean exposed = false;
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                BlockState ns = level.getBlockState(n);
                if (ns.isAir() || ns.canBeReplaced()) {
                    exposed = true;
                    if (dir != Direction.DOWN && random.nextDouble() < CLUSTER_CHANCE) {
                        clusterSpots.add(n);
                        clusterFaces.add(dir);
                    }
                }
            }
            if (!exposed) interior.add(p);
        }
        for (BlockPos p : interior) level.setBlock(p, deep, 2);

        BlockState cluster = ModBlocks.AWAKENING_CLUSTER.get().defaultBlockState();
        for (int i = 0; i < clusterSpots.size(); i++) {
            BlockPos p = clusterSpots.get(i);
            if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) {
                level.setBlock(p, cluster.setValue(AmethystClusterBlock.FACING, clusterFaces.get(i)), 2);
            }
        }
        placed.clear();
    }

    /** Core veins only in the lower half of a spike, where players can reach them. */
    private static BlockState crystal(Size size, RandomSource random, double progress) {
        boolean core = progress < 0.5 && random.nextDouble() < size.coreChance;
        return (core ? ModBlocks.AWAKENING_CORE : ModBlocks.AWAKENING_CRYSTAL).get().defaultBlockState();
    }

    private static boolean canReplace(LevelAccessor level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.AWAKENING_CORE.get())) return false;     // keep veins already placed
        return !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0;
    }
}
