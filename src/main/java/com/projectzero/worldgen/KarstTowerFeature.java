package com.projectzero.worldgen;

import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A karst tower standing in green water: steep limestone sides, a rounded jungle-covered dome, a
 * wave-cut notch at the waterline, sometimes a sea cave straight through, and vines down the walls.
 */
public class KarstTowerFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 14;

    public KarstTowerFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int sea = level.getSeaLevel();
        int floor = o.getY() - 2;
        if (sea - floor > 30) return false;   // too deep: let the open water be
        int top = sea + 18 + r.nextInt(40);
        double R = 4.5 + r.nextDouble() * 6.5;
        double p1 = r.nextDouble() * 10, p2 = r.nextDouble() * 10;
        boolean cave = r.nextFloat() < 0.35F && R > 6;
        Direction caveDir = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        int span = (REACH * 2 + 1);
        int[] topY = new int[span * span];
        java.util.Arrays.fill(topY, Integer.MIN_VALUE);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = floor; y <= top; y++) {
            double t = (y - floor) / (double) (top - floor);
            double rad = t < 0.72 ? R * (1 - 0.12 * t) : R * 0.91 * Math.sqrt(Math.max(0, 1 - Math.pow((t - 0.72) / 0.28, 2)));
            if (y >= sea - 1 && y <= sea + 2) rad *= 0.84;   // notch cut by the waves
            int ri = (int) Math.ceil(rad) + 1;
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                if (Math.abs(dx) > REACH || Math.abs(dz) > REACH) continue;
                double ang = Math.atan2(dz, dx);
                double edge = rad + Math.sin(ang * 4 + p1 + y * 0.05) * 0.8 + Math.sin(ang * 9 + p2) * 0.35;
                if (dx * dx + dz * dz > edge * edge) continue;
                if (cave && y >= sea && y <= sea + 3 && Math.abs(caveDir.getAxis() == Direction.Axis.X ? dz : dx) <= 1) continue;
                p.set(o.getX() + dx, y, o.getZ() + dz);
                BlockState cur = level.getBlockState(p);
                if (!(cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.SAND) || cur.is(BlockTags.DIRT)
                        || cur.is(BlockTags.BASE_STONE_OVERWORLD) || cur.is(Blocks.GRAVEL) || cur.getFluidState().isSource())) continue;
                boolean outer = dx * dx + dz * dz > (edge - 1.3) * (edge - 1.3);
                float f = r.nextFloat();
                BlockState s = outer && y > sea + 3 && f < 0.12F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : f < 0.75F ? ModNature.LIMESTONE.get().defaultBlockState() : f < 0.9F ? Blocks.STONE.defaultBlockState() : Blocks.DIORITE.defaultBlockState();
                level.setBlock(p, s, 2);
                int i = (dx + REACH) * span + dz + REACH;
                topY[i] = Math.max(topY[i], y);
            }
        }
        // Green dome, jungle shrubs and vines over the rim.
        BlockState jungleLeaves = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -REACH; dx <= REACH; dx++) for (int dz = -REACH; dz <= REACH; dz++) {
            int ty = topY[(dx + REACH) * span + dz + REACH];
            if (ty < sea + 8) continue;
            p.set(o.getX() + dx, ty, o.getZ() + dz);
            if (!level.getBlockState(p.above()).isAir()) continue;
            level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            BlockPos up = p.above();
            float f = r.nextFloat();
            if (f < 0.18F && Math.abs(dx) < REACH - 2 && Math.abs(dz) < REACH - 2) {
                level.setBlock(up, Blocks.JUNGLE_LOG.defaultBlockState(), 2);
                for (int lx = -2; lx <= 2; lx++) for (int ly = 0; ly <= 2; ly++) for (int lz = -2; lz <= 2; lz++) {
                    if (lx * lx + lz * lz + ly * ly * 2 > 5) continue;
                    BlockPos lp = up.offset(lx, ly + 1, lz);
                    if (level.getBlockState(lp).isAir()) level.setBlock(lp, jungleLeaves, 2);
                }
            } else if (f < 0.5F && level.getBlockState(up).isAir()) {
                level.setBlock(up, (r.nextBoolean() ? Blocks.SHORT_GRASS : Blocks.FERN).defaultBlockState(), 2);
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos side = p.relative(d);
                if (!level.getBlockState(side).isAir() || r.nextFloat() > 0.4F) continue;
                BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(d.getOpposite()), true);
                BlockPos.MutableBlockPos v = side.mutable();
                int len = 8 + r.nextInt(30);
                for (int k = 0; k < len && v.getY() > sea && level.getBlockState(v).isAir() && !level.getBlockState(v.relative(d.getOpposite())).isAir(); k++) {
                    level.setBlock(v, vine, 2);
                    v.move(Direction.DOWN);
                }
            }
        }
        return true;
    }
}
