package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Spirit Bamboo Sea: a grove of giant bamboo stalks 16 to 40 blocks tall, ringed with pale nodes,
 * crowned with leaf tufts, with a spirit lantern hanging under a tuft here and there.
 */
public class GiantBambooFeature extends Feature<NoneFeatureConfiguration> {
    public GiantBambooFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int stalks = 6 + r.nextInt(10);
        BlockState stalk = Blocks.BAMBOO_BLOCK.defaultBlockState();
        BlockState node = Blocks.STRIPPED_BAMBOO_BLOCK.defaultBlockState();
        BlockState leaves = Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int s = 0; s < stalks; s++) {
            int x = o.getX() + r.nextInt(13) - 6, z = o.getZ() + r.nextInt(13) - 6;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos base = new BlockPos(x, y, z);
            if (!level.getBlockState(base.below()).is(BlockTags.DIRT) || !level.getBlockState(base).isAir()) continue;
            int h = 16 + r.nextInt(25);
            int phase = r.nextInt(5);
            for (int k = 0; k < h; k++) {
                BlockPos at = base.above(k);
                if (!level.getBlockState(at).isAir() && !level.getBlockState(at).canBeReplaced()) { h = k; break; }
                level.setBlock(at, (k + phase) % 5 == 0 ? node : stalk, 2);
            }
            // Tufts along the upper third, and the crown.
            for (int k = h * 2 / 3; k <= h; k += 3 + r.nextInt(3)) {
                BlockPos at = base.above(k);
                int rad = k >= h - 1 ? 2 : 1;
                for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) for (int dy = 0; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dz) + dy > rad + 1 || (dx == 0 && dz == 0 && dy == 0)) continue;
                    BlockPos lp = at.offset(dx, dy, dz);
                    if (level.getBlockState(lp).isAir()) level.setBlock(lp, leaves, 2);
                }
                if (r.nextFloat() < 0.18F) {
                    BlockPos lantern = at.offset(r.nextBoolean() ? 1 : -1, -1, 0);
                    if (level.getBlockState(lantern).isAir() && level.getBlockState(lantern.above()).is(BlockTags.LEAVES))
                        level.setBlock(lantern, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
                }
            }
        }
        return true;
    }
}
