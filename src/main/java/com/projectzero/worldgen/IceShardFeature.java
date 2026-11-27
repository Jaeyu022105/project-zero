package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ice Cathedral: a cluster of ice shards bursting out of the ground like a crystal geode, 4 to 9
 * leaning blades of blue and packed ice, 4 to 14 long, pointing up and outward from a shared root.
 * Stays within about 10 blocks of the origin.
 */
public class IceShardFeature extends Feature<NoneFeatureConfiguration> {
    public IceShardFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos root = ctx.origin().below(2);
        if (!level.getBlockState(ctx.origin().below()).isSolid()) return false;
        int n = 4 + r.nextInt(6);
        for (int i = 0; i < n; i++) {
            double yaw = r.nextDouble() * Math.PI * 2;
            double tilt = 0.15 + r.nextDouble() * 0.55;           // 0 = straight up
            double len = 4 + r.nextDouble() * 10;
            double thick = 0.8 + len * 0.08;
            double dx = Math.cos(yaw) * Math.sin(tilt), dz = Math.sin(yaw) * Math.sin(tilt), dy = Math.cos(tilt);
            BlockState body = (r.nextFloat() < 0.6F ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (double s = 0; s <= len; s += 0.5) {
                double rad = thick * (1 - s / len) + 0.25;   // tapers to a point
                int ri = Mth.ceil(rad);
                double cx = root.getX() + 0.5 + dx * s, cy = root.getY() + 0.5 + dy * s, cz = root.getZ() + 0.5 + dz * s;
                for (int ox = -ri; ox <= ri; ox++) for (int oy = -ri; oy <= ri; oy++) for (int oz = -ri; oz <= ri; oz++) {
                    if (ox * ox + oy * oy + oz * oz > rad * rad) continue;
                    p.set(Mth.floor(cx) + ox, Mth.floor(cy) + oy, Mth.floor(cz) + oz);
                    BlockState cur = level.getBlockState(p);
                    if (cur.isAir() || cur.canBeReplaced() || cur.is(Blocks.SNOW_BLOCK) || cur.is(Blocks.PACKED_ICE))
                        level.setBlock(p, body, 2);
                }
            }
        }
        return true;
    }
}
