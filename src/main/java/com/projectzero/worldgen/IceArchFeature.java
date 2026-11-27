package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ice Cathedral: a ribbed arch of blue and packed ice 10 to 26 blocks across, sometimes with a
 * frozen waterfall hanging from its crown, and now and then the bones of something that froze in
 * the ice at its foot.
 */
public class IceArchFeature extends Feature<NoneFeatureConfiguration> {
    public IceArchFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int half = 5 + r.nextInt(8);                  // half span, so 10 to 24 across
        int rise = (int) (half * (1.1 + r.nextDouble() * 0.6));
        double thick = 1.3 + r.nextDouble() * 1.2;
        boolean alongX = r.nextBoolean();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // Parametric half-ellipse; sample densely so the rib has no gaps.
        for (int i = 0; i <= 160; i++) {
            double a = Math.PI * i / 160.0;
            double u = Math.cos(a) * half, v = Math.sin(a) * rise;
            double t = thick * (1.3 - 0.5 * Math.sin(a));    // thicker at the feet
            int ti = (int) Math.ceil(t);
            for (int du = -ti; du <= ti; du++) for (int dv = -ti; dv <= ti; dv++) for (int dw = -ti; dw <= ti; dw++) {
                if (du * du + dv * dv + dw * dw > t * t) continue;
                int x = (int) Math.round(u) + du, y = (int) Math.round(v) + dv - 2, w = dw;
                p.set(o.getX() + (alongX ? x : w), o.getY() + y, o.getZ() + (alongX ? w : x));
                BlockState cur = level.getBlockState(p);
                if (!(cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.SNOW) || cur.is(BlockTags.DIRT) || cur.is(Blocks.ICE) || cur.is(Blocks.PACKED_ICE))) continue;
                level.setBlock(p, (r.nextFloat() < 0.6F ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState(), 2);
            }
        }
        // Frozen waterfall: a hanging sheet of ice from the crown, narrowing as it falls.
        if (r.nextFloat() < 0.6F) {
            int top = rise - 2 - (int) thick;
            for (int y = top; y > -1; y--) {
                int w = Math.max(0, (int) ((y + 2) * 0.25) + (r.nextFloat() < 0.3F ? 1 : 0));
                for (int k = -w; k <= w; k++) {
                    p.set(o.getX() + (alongX ? k : 0), o.getY() + y, o.getZ() + (alongX ? 0 : k));
                    if (level.getBlockState(p).isAir()) level.setBlock(p, Blocks.ICE.defaultBlockState(), 2);
                }
            }
        }
        // Something trapped in the ice at one foot: bones, and sometimes a skull.
        if (r.nextFloat() < 0.4F) {
            BlockPos foot = o.offset(alongX ? -half : 0, 0, alongX ? 0 : -half);
            for (int k = 0; k < 4; k++) {
                BlockPos b = foot.offset(r.nextInt(3) - 1, r.nextInt(3) - 1, r.nextInt(3) - 1);
                if (level.getBlockState(b).is(Blocks.BLUE_ICE) || level.getBlockState(b).is(Blocks.PACKED_ICE))
                    level.setBlock(b, Blocks.BONE_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                            Direction.Axis.values()[r.nextInt(3)]), 2);
            }
            if (r.nextBoolean() && (level.getBlockState(foot).is(Blocks.BLUE_ICE) || level.getBlockState(foot).is(Blocks.PACKED_ICE)))
                level.setBlock(foot, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r.nextInt(16)), 2);
        }
        return true;
    }
}
