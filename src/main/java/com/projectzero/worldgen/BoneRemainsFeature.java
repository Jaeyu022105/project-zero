package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        for (int i = -half - 2; i <= half + 3; i++) {
            BlockPos s = o.relative(along, i).below(sink);
            if (level.getBlockState(s.above()).isAir() || r.nextFloat() < 0.85F) level.setBlock(s, spine, 2);
        }
        for (int k = 0; k < ribs; k++) {
            BlockPos root = o.relative(along, -half + k * gap).below(sink);
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
        BlockPos head = o.relative(along, half + 4);
        for (int dy = 2; dy >= -2; dy--) {
            BlockPos q = head.above(dy);
            if (level.getBlockState(q).isAir() && level.getBlockState(q.below()).isSolid()) {
                level.setBlock(q, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r.nextInt(16)), 2);
                break;
            }
        }
        return true;
    }
}
