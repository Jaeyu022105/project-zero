package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A hot spring of the Geyser Basin: a round pool with rainbow rings (pale blue centre, then yellow,
 * orange and rust terracotta) on a white sinter mound. Some pools hide a vent: a narrow bubble
 * column over soul sand that throws you upward. Steam comes from {@code client/GeyserSteam}.
 */
public class GeyserFeature extends Feature<NoneFeatureConfiguration> {
    public GeyserFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos c = ctx.origin().below();
        if (!level.getBlockState(c).isSolid()) return false;
        int R = 3 + r.nextInt(5);
        int depth = 2 + r.nextInt(2);
        BlockState[] rings = {Blocks.YELLOW_TERRACOTTA.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
                Blocks.TERRACOTTA.defaultBlockState(), Blocks.CALCITE.defaultBlockState()};
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -R - 4; dx <= R + 4; dx++) for (int dz = -R - 4; dz <= R + 4; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R + 4) continue;
            if (d <= R) {
                // Bowl: deeper in the middle, calcite floor reads pale blue through the water.
                int dd = d < R * 0.5 ? depth : Math.max(1, depth - 1);
                for (int y = 0; y >= -dd; y--) {
                    p.set(c.getX() + dx, c.getY() + y, c.getZ() + dz);
                    level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
                }
                p.set(c.getX() + dx, c.getY() - dd - 1, c.getZ() + dz);
                level.setBlock(p, Blocks.CALCITE.defaultBlockState(), 2);
                for (int y = 1; y <= 2; y++) {
                    p.set(c.getX() + dx, c.getY() + y, c.getZ() + dz);
                    if (!level.getBlockState(p).isAir() && !level.getBlockState(p).is(BlockTags.LOGS)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            } else {
                int ring = Math.min(3, (int) (d - R));
                if (r.nextFloat() < 0.15F) ring = Math.min(3, ring + 1);
                p.set(c.getX() + dx, c.getY(), c.getZ() + dz);
                BlockState cur = level.getBlockState(p);
                if (cur.is(BlockTags.DIRT) || cur.is(BlockTags.BASE_STONE_OVERWORLD) || cur.is(Blocks.CALCITE) || cur.is(Blocks.GRAVEL))
                    level.setBlock(p, rings[ring], 2);
                BlockPos up = p.above();
                if (level.getBlockState(up).canBeReplaced() && !level.getBlockState(up).isAir() && level.getBlockState(up).getFluidState().isEmpty())
                    level.setBlock(up, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        // Vent: soul sand under a bubble column from the bottom of a shaft up to the surface.
        if (r.nextFloat() < 0.55F) {
            int shaft = 4 + r.nextInt(4);
            for (int y = 0; y >= -depth - shaft; y--) {
                p.set(c.getX(), c.getY() + y, c.getZ());
                level.setBlock(p, Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false), 2);
            }
            level.setBlock(p.move(0, -1, 0), Blocks.SOUL_SAND.defaultBlockState(), 2);
        }
        return true;
    }
}
