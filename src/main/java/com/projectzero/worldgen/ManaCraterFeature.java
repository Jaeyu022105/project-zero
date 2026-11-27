package com.projectzero.worldgen;

import com.projectzero.registry.ModBlocks;
import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Mana Wastes: a blast crater where raw mana hit the ground. A shallow bowl glassed over with
 * tinted glass and amethyst, a raised lip of tuff thrown out around it, and a mana crystal heart in
 * the middle with crystals growing on it. Stays within about 10 blocks of the origin.
 */
public class ManaCraterFeature extends Feature<NoneFeatureConfiguration> {
    public ManaCraterFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos c = ctx.origin().below();
        if (!level.getBlockState(c).isSolid()) return false;
        double rad = 4 + r.nextDouble() * 4;
        int depth = 2 + r.nextInt(3);
        int ri = (int) Math.ceil(rad) + 2;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            int x = c.getX() + dx, z = c.getZ() + dz;
            if (d <= rad) {
                // Bowl: deepest in the middle, rising to the lip.
                int dig = (int) Math.round(depth * (1 - (d / rad) * (d / rad)));
                for (int y = 0; y < dig; y++) { p.set(x, c.getY() - y, z); level.setBlock(p, Blocks.AIR.defaultBlockState(), 2); }
                for (int y = 1; y <= 4; y++) {   // clear whatever stood on the old ground
                    p.set(x, c.getY() + y, z);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
                p.set(x, c.getY() - dig, z);
                float f = r.nextFloat();
                BlockState glass = f < 0.55F ? Blocks.TINTED_GLASS.defaultBlockState()
                        : f < 0.85F ? Blocks.AMETHYST_BLOCK.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
                level.setBlock(p, glass, 2);
                p.move(Direction.DOWN);
                if (level.getBlockState(p).isAir()) level.setBlock(p, Blocks.TUFF.defaultBlockState(), 2);
            } else if (d <= rad + 2 && r.nextFloat() < 0.75F) {
                // Thrown-out lip, one block high.
                for (int y = 2; y >= -2; y--) {
                    p.set(x, c.getY() + y, z);
                    if (level.getBlockState(p).isSolid() && level.getBlockState(p.above()).isAir()) {
                        level.setBlock(p.above(), (r.nextFloat() < 0.7F ? Blocks.TUFF : Blocks.COBBLED_DEEPSLATE).defaultBlockState(), 2);
                        break;
                    }
                }
            }
        }
        // The heart: a small mana crystal mound with crystals budding off it.
        BlockPos heart = c.below(depth - 1);
        for (int y = 0; y <= 1 + r.nextInt(2); y++) level.setBlock(heart.above(y), ModNature.MANA_CRYSTAL_BLOCK.get().defaultBlockState(), 2);
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) continue;
            BlockPos q = heart.above(1).relative(d);
            if (level.getBlockState(q).isAir() && r.nextFloat() < 0.7F)
                level.setBlock(q, ModBlocks.MANA_CRYSTAL.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, d), 2);
        }
        return true;
    }
}
