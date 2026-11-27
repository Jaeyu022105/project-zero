package com.projectzero.worldgen;

import com.projectzero.registry.ModBlocks;
import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Crystal Tundra outcrop: a cluster of tilted glowing mana-crystal shards, 4 to 16 blocks long,
 * bursting from a calcite base, with small mana crystals growing on the shards.
 */
public class CrystalOutcropFeature extends Feature<NoneFeatureConfiguration> {
    public CrystalOutcropFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin().below();
        BlockState crystal = ModNature.MANA_CRYSTAL_BLOCK.get().defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // Calcite base.
        int br = 2 + r.nextInt(2);
        for (int dx = -br; dx <= br; dx++) for (int dz = -br; dz <= br; dz++) for (int dy = -2; dy <= 0; dy++) {
            if (dx * dx + dz * dz + dy * dy * 3 > br * br + 1) continue;
            p.set(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
            BlockState cur = level.getBlockState(p);
            if (cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.DIRT) || cur.is(BlockTags.SNOW) || cur.is(BlockTags.BASE_STONE_OVERWORLD))
                level.setBlock(p, Blocks.CALCITE.defaultBlockState(), 2);
        }
        int shards = 3 + r.nextInt(5);
        for (int s = 0; s < shards; s++) {
            double yaw = r.nextDouble() * Math.PI * 2, tilt = (s == 0 ? 0.1 : 0.25 + r.nextDouble() * 0.55);
            double dx = Math.sin(tilt) * Math.cos(yaw), dy = Math.cos(tilt), dz = Math.sin(tilt) * Math.sin(yaw);
            int len = (s == 0 ? 9 : 4) + r.nextInt(s == 0 ? 8 : 7);
            double rad = s == 0 ? 1.6 : 0.9 + r.nextDouble() * 0.5;
            for (int k = 0; k <= len; k++) {
                double t = k / (double) len;
                double rr = rad * (1 - t * 0.85);
                double cx = dx * k, cy = dy * k, cz = dz * k;
                int ri = (int) Math.ceil(rr);
                for (int ox = -ri; ox <= ri; ox++) for (int oy = -ri; oy <= ri; oy++) for (int oz = -ri; oz <= ri; oz++) {
                    if (ox * ox + oy * oy + oz * oz > rr * rr + 0.2) continue;
                    p.set(o.getX() + Mth.floor(cx + ox + 0.5), o.getY() + 1 + Mth.floor(cy + oy + 0.5), o.getZ() + Mth.floor(cz + oz + 0.5));
                    BlockState cur = level.getBlockState(p);
                    if (cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.SNOW)) level.setBlock(p, crystal, 2);
                }
            }
        }
        // Little crystals sprouting on the shards.
        for (int i = 0; i < 14; i++) {
            BlockPos at = o.offset(r.nextInt(7) - 3, 1 + r.nextInt(10), r.nextInt(7) - 3);
            if (!level.getBlockState(at).isAir()) continue;
            for (Direction d : Direction.values()) {
                if (!level.getBlockState(at.relative(d.getOpposite())).is(crystal.getBlock())) continue;
                level.setBlock(at, ModBlocks.MANA_CRYSTAL.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, d), 2);
                break;
            }
        }
        return true;
    }
}
