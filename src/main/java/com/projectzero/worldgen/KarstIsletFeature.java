package com.projectzero.worldgen;

import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Low limestone islets with a small sand cove and a compact jungle crown. */
public class KarstIsletFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 16;

    public KarstIsletFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();
        int sea = level.getSeaLevel();
        int radius = 4 + r.nextInt(4);
        int top = sea + 2 + r.nextInt(7);
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        if (sea - floor > 24 || top >= level.getMaxBuildHeight() - 6) return false;
        int cx = origin.getX() + r.nextInt(7) - 3, cz = origin.getZ() + r.nextInt(7) - 3;
        double coveAngle = r.nextDouble() * Math.PI * 2;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int minX = Math.max(-REACH, cx - origin.getX() - radius - 1);
        int maxX = Math.min(REACH, cx - origin.getX() + radius + 1);
        int minZ = Math.max(-REACH, cz - origin.getZ() - radius - 1);
        int maxZ = Math.min(REACH, cz - origin.getZ() + radius + 1);

        for (int dx = minX; dx <= maxX; dx++) for (int dz = minZ; dz <= maxZ; dz++) {
            int x = origin.getX() + dx, z = origin.getZ() + dz;
            int localFloor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            if (sea - localFloor > 24) continue;
            double dist = Math.sqrt((x - cx) * (double) (x - cx) + (z - cz) * (double) (z - cz));
            double edge = radius + Math.sin(Math.atan2(z - cz, x - cx) * 4 + coveAngle) * 0.55;
            if (dist > edge) continue;
            int topY = top - (int) Math.floor(Math.pow(dist / edge, 2) * 2.2);
            double taper = Math.max(0.7, 1.0 - Math.max(0, (topY - sea) / (double) Math.max(1, top - sea)) * 0.28);
            double layerRadius = edge * taper;
            for (int y = localFloor - 1; y <= topY; y++) {
                double t = (y - (localFloor - 1)) / (double) Math.max(1, topY - localFloor + 1);
                double layerEdge = layerRadius + Math.sin((x + z) * 0.5 + y * 0.2) * 0.35;
                if (dist > layerEdge) continue;
                p.set(x, y, z);
                BlockState current = level.getBlockState(p);
                if (!(current.isAir() || current.canBeReplaced() || current.getFluidState().isSource()
                        || current.is(BlockTags.SAND) || current.is(BlockTags.DIRT)
                        || current.is(BlockTags.BASE_STONE_OVERWORLD))) continue;
                BlockState rock = t > 0.94 && y == topY ? Blocks.GRASS_BLOCK.defaultBlockState()
                        : r.nextFloat() < 0.08F ? Blocks.CALCITE.defaultBlockState()
                        : ModNature.LIMESTONE.get().defaultBlockState();
                level.setBlock(p, rock, 2);
            }
            // A sandy pocket curls around one side of the island just above the wave line.
            double angle = Math.atan2(z - cz, x - cx);
            double angleDelta = Math.atan2(Math.sin(angle - coveAngle), Math.cos(angle - coveAngle));
            if (Math.abs(angleDelta) < 0.75 && dist > radius * 0.38 && dist < radius * 0.9 && topY >= sea + 1) {
                BlockPos sand = new BlockPos(x, topY, z);
                if (level.getBlockState(sand).is(Blocks.GRASS_BLOCK) || level.getBlockState(sand).is(ModNature.LIMESTONE.get()))
                    level.setBlock(sand, Blocks.SAND.defaultBlockState(), 2);
            }
        }

        BlockPos crown = new BlockPos(cx, top, cz);
        if (!level.getBlockState(crown.below()).isSolid()) return true;
        if (r.nextFloat() < 0.7F) jungleTree(level, crown, r);
        else shrub(level, crown, r);
        return true;
    }

    private static void jungleTree(WorldGenLevel level, BlockPos base, RandomSource r) {
        int height = 4 + r.nextInt(3);
        BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int y = 0; y < height; y++) {
            BlockPos log = base.above(y);
            if (level.getBlockState(log).isAir() || level.getBlockState(log).canBeReplaced())
                level.setBlock(log, Blocks.JUNGLE_LOG.defaultBlockState(), 2);
        }
        BlockPos top = base.above(height - 1);
        for (int dx = -2; dx <= 2; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -2; dz <= 2; dz++) {
            if (dx * dx + dz * dz + dy * dy * 2 > 6) continue;
            BlockPos leaf = top.offset(dx, dy, dz);
            if (level.getBlockState(leaf).isAir()) level.setBlock(leaf, leaves, 2);
        }
    }

    private static void shrub(WorldGenLevel level, BlockPos base, RandomSource r) {
        BlockPos log = base;
        if (level.getBlockState(log).isAir()) level.setBlock(log, Blocks.JUNGLE_LOG.defaultBlockState(), 2);
        BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            if (r.nextFloat() < 0.15F) continue;
            BlockPos leaf = base.offset(dx, dy, dz);
            if (level.getBlockState(leaf).isAir()) level.setBlock(leaf, leaves, 2);
        }
    }
}
