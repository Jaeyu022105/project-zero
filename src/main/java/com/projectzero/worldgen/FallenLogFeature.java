package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

/**
 * A fallen tree: often a short upright stump, a gap, then the trunk lying on the ground (3 to 6
 * long). The fallen trunk gets moss carpets and the odd mushroom on top. The log block comes from
 * the config, so one feature serves oak, birch, spruce, jungle, acacia, dark oak and driftwood.
 */
public class FallenLogFeature extends Feature<BlockStateConfiguration> {
    public FallenLogFeature() {
        super(BlockStateConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();
        BlockState log = ctx.config().state;
        if (!log.hasProperty(RotatedPillarBlock.AXIS)) return false;
        if (!solidGround(level, origin.below()) || !level.getBlockState(origin).canBeReplaced()) return false;

        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        boolean stump = r.nextFloat() < 0.6F;
        int start = stump ? 2 : 0;
        int length = 3 + r.nextInt(4);

        // Check the whole lying trunk has ground under it and room for it.
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = start; i < start + length; i++) {
            p.setWithOffset(origin, dir.getStepX() * i, 0, dir.getStepZ() * i);
            if (!level.getBlockState(p).canBeReplaced() || !solidGround(level, p.below())) return false;
        }
        if (stump) level.setBlock(origin, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), 2);
        BlockState lying = log.setValue(RotatedPillarBlock.AXIS, dir.getAxis());
        for (int i = start; i < start + length; i++) {
            p.setWithOffset(origin, dir.getStepX() * i, 0, dir.getStepZ() * i);
            level.setBlock(p, lying, 2);
            BlockPos above = p.above();
            if (!level.getBlockState(above).isAir()) continue;
            float roll = r.nextFloat();
            if (roll < 0.35F) level.setBlock(above, Blocks.MOSS_CARPET.defaultBlockState(), 2);
            else if (roll < 0.45F) level.setBlock(above, (r.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM).defaultBlockState(), 2);
        }
        return true;
    }

    private static boolean solidGround(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.MUD);
    }
}
