package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Bloodmoon Steppe: the half-buried skeleton of something huge. A spine of bone blocks runs along
 * the ground and pairs of ribs arch up out of the earth, curving in toward each other. Some ribs are
 * broken short. Stays within about 12 blocks of the origin.
 */
public class BoneRemainsFeature extends Feature<NoneFeatureConfiguration> {
    public BoneRemainsFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        if (!level.getBlockState(o.below()).isSolid()) return false;
        Direction along = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        Direction side = along.getClockWise();
        int ribs = 4 + r.nextInt(4);
        int gap = 2;
        int half = ribs * gap / 2;
        BlockState spine = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, along.getAxis());
        double span = 3.5 + r.nextDouble() * 2.5;   // half width of the rib cage
        double height = span * (1.1 + r.nextDouble() * 0.4);
        int sink = 1 + r.nextInt(2);                // how deep the skeleton lies
        // The spine follows the ground. Find the ground under every spine column first, and give up
        // on rough ground (a gully or a hillside) rather than leave bones floating or buried deep.
        int from = -half - 2, to = half + 4;
        int[] ground = new int[to - from + 1];
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int i = from; i <= to; i++) {
            int g = groundY(level, o.relative(along, i));
            if (g == Integer.MIN_VALUE) return false;
            ground[i - from] = g;
            lo = Math.min(lo, g); hi = Math.max(hi, g);
        }
        if (hi - lo > 4) return false;
        for (int i = from; i <= half + 3; i++) {
            BlockPos col = o.relative(along, i);
            BlockPos s = new BlockPos(col.getX(), ground[i - from] - sink, col.getZ());
            if (r.nextFloat() < 0.85F) level.setBlock(s, spine, 2);
        }
        for (int k = 0; k < ribs; k++) {
            int i = -half + k * gap;
            BlockPos col = o.relative(along, i);
            BlockPos root = new BlockPos(col.getX(), ground[i - from] - sink, col.getZ());
            double scale = 1 - Math.abs(k - (ribs - 1) / 2.0) / ribs * 0.6; // middle ribs are the biggest
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                boolean broken = r.nextFloat() < 0.3F;
                int steps = 18;
                int stop = broken ? 6 + r.nextInt(6) : steps;
                for (int t = 1; t <= stop; t++) {
                    double a = Math.PI * t / (steps * 1.15);        // a bit less than a half circle
                    double out = Math.sin(a) * span * scale;
                    double up = (1 - Math.cos(a)) * height * scale * 0.55 + Math.sin(a) * 0.6;
                    BlockPos p = root.relative(side, (int) Math.round(sgn * out)).above((int) Math.round(up));
                    BlockState cur = level.getBlockState(p);
                    if (cur.isAir() || cur.canBeReplaced() || cur.is(net.minecraft.tags.BlockTags.DIRT) || cur.is(net.minecraft.tags.BlockTags.SAND))
                        level.setBlock(p, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), 2);
                }
            }
        }
        // The skull lies at the front end of the spine.
        BlockPos headCol = o.relative(along, half + 4);
        BlockPos q = new BlockPos(headCol.getX(), ground[half + 4 - from] + 1, headCol.getZ());
        if (level.getBlockState(q).isAir())
            level.setBlock(q, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r.nextInt(16)), 2);
        return true;
    }

    /**
     * The Y of the top natural ground block in this column (dirt, sand, stone, terracotta), searching
     * from a little above the origin down; MIN_VALUE if there is none close by. Ignores logs, leaves and
     * plants that other features have already put here.
     */
    private static int groundY(WorldGenLevel level, BlockPos col) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = col.getY() + 5; y >= col.getY() - 8; y--) {
            p.set(col.getX(), y, col.getZ());
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD)
                    || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.GRAVEL) || s.is(Blocks.COARSE_DIRT)) return y;
        }
        return Integer.MIN_VALUE;
    }
}
