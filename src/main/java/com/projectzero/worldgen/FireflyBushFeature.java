package com.projectzero.worldgen;

import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Scatters a small cluster of {@link FireflyBushBlock} strictly inside its own 16x16 chunk.
 * Placed only via Forge biome modifier (data/projectzero/forge/biome_modifier/fireflies.json).
 */
public class FireflyBushFeature extends Feature<NoneFeatureConfiguration> {
    public FireflyBushFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();

        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        BlockState bush = ModNature.FIREFLY_BUSH.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();

        int attempts = 2 + r.nextInt(3);
        boolean placedAny = false;
        for (int i = 0; i < attempts; i++) {
            int x = origin.getX() + r.nextInt(9) - 4;
            int z = origin.getZ() + r.nextInt(9) - 4;
            if (x < minX || x > maxX || z < minZ || z > maxZ) continue;

            int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            int minY = Math.max(level.getMinBuildHeight() + 2, topY - 56);
            for (int y = topY; y >= minY; y--) {
                pos.set(x, y, z);
                BlockState cur = level.getBlockState(pos);
                if (!cur.isAir() && !(cur.canBeReplaced() && cur.getFluidState().isEmpty() && !cur.is(bush.getBlock()))) {
                    continue;
                }
                below.set(x, y - 1, z);
                if (FireflyBushBlock.canGrowOn(level.getBlockState(below))) {
                    level.setBlock(pos, bush, 2);
                    placedAny = true;
                    break;
                }
            }
        }
        return placedAny;
    }
}
