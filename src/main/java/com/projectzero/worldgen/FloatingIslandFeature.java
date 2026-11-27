package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * A floating island 35 to 110 blocks above the ground (Skyfall Isles biome): a grassy top on an
 * upside-down rocky cone, roots and glowing berry vines trailing from its underside, a small tree
 * or two, and sometimes a spring at the edge that pours a waterfall down to the land below.
 */
public class FloatingIslandFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 14;

    public FloatingIslandFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        // Islands belong to the Skyfall Isles alone: skip any that would hang over a neighbouring biome.
        BlockPos o = ctx.origin();
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            BlockPos edge = o.offset((int) Math.round(Math.cos(a) * (REACH + 6)), 0, (int) Math.round(Math.sin(a) * (REACH + 6)));
            // Ask the biome source, not the level: these points lie outside the chunks a feature may read.
            if (!ctx.chunkGenerator().getBiomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(edge.getX()),
                    net.minecraft.core.QuartPos.fromBlock(edge.getY()), net.minecraft.core.QuartPos.fromBlock(edge.getZ()),
                    level.getLevel().getChunkSource().randomState().sampler()).is(ModBiomes.SKYFALL_ISLES)) return false;
        }
        int lift = 35 + r.nextInt(76);
        BlockPos c = ctx.origin().above(lift);
        if (c.getY() + 12 >= level.getMaxBuildHeight()) return false;
        double rad = 5 + r.nextDouble() * 8.5;
        double depth = rad * (1.4 + r.nextDouble());
        double p1 = r.nextDouble() * 10;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int span = (int) Math.ceil(rad) + 1;
        int[] top = new int[(REACH * 2 + 1) * (REACH * 2 + 1)];
        java.util.Arrays.fill(top, Integer.MIN_VALUE);
        int[] bottom = new int[top.length];
        for (int dx = -span; dx <= span; dx++) for (int dz = -span; dz <= span; dz++) {
            if (Math.abs(dx) > REACH || Math.abs(dz) > REACH) continue;
            double ang = Math.atan2(dz, dx);
            double edge = rad * (1 + Math.sin(ang * 3 + p1) * 0.12 + Math.sin(ang * 5 - p1) * 0.07);
            double d = Math.sqrt(dx * dx + dz * dz) / edge;
            if (d > 1) continue;
            int up = (int) Math.round(2 * (1 - d * d) + Math.sin(dx * 0.7 + p1) * 0.6);
            int down = (int) Math.round(depth * Math.pow(1 - d, 1.3) + 1 + Math.sin(dz * 0.9 - p1) * 1.2);
            for (int y = -down; y <= up; y++) {
                p.set(c.getX() + dx, c.getY() + y, c.getZ() + dz);
                if (!level.getBlockState(p).isAir()) continue;
                BlockState s;
                if (y == up) s = Blocks.GRASS_BLOCK.defaultBlockState();
                else if (y > up - 3) s = Blocks.DIRT.defaultBlockState();
                else if (r.nextFloat() < 0.08F) s = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                else if (y < -down + 2 && r.nextFloat() < 0.3F) s = Blocks.ROOTED_DIRT.defaultBlockState();
                else s = r.nextFloat() < 0.15F ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
                level.setBlock(p, s, 2);
            }
            int i = (dx + REACH) * (REACH * 2 + 1) + dz + REACH;
            top[i] = up;
            bottom[i] = -down;
            // Things hanging from the underside.
            p.set(c.getX() + dx, c.getY() - down - 1, c.getZ() + dz);
            float roll = r.nextFloat();
            if (roll < 0.18F) hang(level, p, Blocks.HANGING_ROOTS.defaultBlockState(), 1);
            else if (roll < 0.30F) vines(level, p, r);
            // Grass and flowers on top.
            p.set(c.getX() + dx, c.getY() + up + 1, c.getZ() + dz);
            if (level.getBlockState(p).isAir()) {
                float f = r.nextFloat();
                if (f < 0.35F) level.setBlock(p, Blocks.SHORT_GRASS.defaultBlockState(), 2);
                else if (f < 0.42F) level.setBlock(p, (r.nextBoolean() ? Blocks.AZURE_BLUET : Blocks.OXEYE_DAISY).defaultBlockState(), 2);
            }
        }
        // Trees.
        int trees = rad > 8 ? 1 + r.nextInt(2) : r.nextInt(2);
        for (int t = 0; t < trees; t++) {
            int dx = (int) ((r.nextDouble() - 0.5) * rad), dz = (int) ((r.nextDouble() - 0.5) * rad);
            int i = (dx + REACH) * (REACH * 2 + 1) + dz + REACH;
            if (top[i] == Integer.MIN_VALUE) continue;
            tree(level, c.offset(dx, top[i] + 1, dz), r);
        }
        // A spring at the rim feeding a waterfall.
        if (r.nextFloat() < 0.35F) {
            double a = r.nextDouble() * Math.PI * 2;
            int dx = (int) Math.round(Math.cos(a) * (rad - 2)), dz = (int) Math.round(Math.sin(a) * (rad - 2));
            int i = (dx + REACH) * (REACH * 2 + 1) + dz + REACH;
            if (top[i] != Integer.MIN_VALUE) {
                BlockPos w = c.offset(dx, top[i], dz);
                level.setBlock(w, Blocks.WATER.defaultBlockState(), 2);
                level.scheduleTick(w, Fluids.WATER, 0);
            }
        }
        return true;
    }

    private static void hang(WorldGenLevel level, BlockPos at, BlockState s, int len) {
        BlockPos.MutableBlockPos p = at.mutable();
        for (int k = 0; k < len && level.getBlockState(p).isAir(); k++) { level.setBlock(p, s, 2); p.move(Direction.DOWN); }
    }

    private static void vines(WorldGenLevel level, BlockPos at, RandomSource r) {
        int len = 3 + r.nextInt(12);
        BlockPos.MutableBlockPos p = at.mutable();
        for (int k = 0; k < len; k++) {
            if (!level.getBlockState(p).isAir()) break;
            boolean last = k == len - 1 || !level.getBlockState(p.below()).isAir();
            level.setBlock(p, (last ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                    .setValue(CaveVines.BERRIES, r.nextFloat() < 0.3F), 2);
            if (last) break;
            p.move(Direction.DOWN);
        }
    }

    /** Mostly skywood (pale bark, blue-white leaves trailing below the crown), now and then a flowering azalea. */
    private static void tree(WorldGenLevel level, BlockPos at, RandomSource r) {
        boolean azalea = r.nextFloat() < 0.2F;
        BlockState log = (azalea ? Blocks.OAK_LOG : com.projectzero.registry.ModNature.SKYWOOD_LOG.get()).defaultBlockState();
        BlockState leaves = (azalea ? Blocks.FLOWERING_AZALEA_LEAVES : com.projectzero.registry.ModNature.SKYWOOD_LEAVES.get())
                .defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int h = (azalea ? 4 : 6) + r.nextInt(3);
        for (int y = 0; y < h; y++) if (level.getBlockState(at.above(y)).canBeReplaced() || level.getBlockState(at.above(y)).isAir()) level.setBlock(at.above(y), log, 2);
        BlockPos crown = at.above(h - 1);
        int rad = azalea ? 2 : 3;
        for (int dx = -rad; dx <= rad; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -rad; dz <= rad; dz++) {
            if (dx * dx + dy * dy * 2 + dz * dz > rad * rad + 2) continue;
            BlockPos lp = crown.offset(dx, dy, dz);
            if (!level.getBlockState(lp).isAir()) continue;
            level.setBlock(lp, leaves, 2);
            // Skywood leaves trail down in strands from the underside of the crown.
            if (!azalea && dy == -1 && r.nextFloat() < 0.3F) {
                int len = 1 + r.nextInt(3);
                for (int k = 1; k <= len && level.getBlockState(lp.below(k)).isAir(); k++) level.setBlock(lp.below(k), leaves, 2);
            }
        }
    }
}
