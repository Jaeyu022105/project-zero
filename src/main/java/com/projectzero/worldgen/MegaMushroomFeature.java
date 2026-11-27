package com.projectzero.worldgen;

import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Giant mushrooms 28 to 48 blocks tall for the Capwood: a thick stem that leans and bends, and a
 * huge cap with a glowing gill layer underneath. Three kinds: a red dome, a broad flat brown
 * shelf, and a violet dome. Everything stays within 15 blocks of the stem so it fits the area a
 * feature may write to.
 */
public class MegaMushroomFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 15;

    public MegaMushroomFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos base = ctx.origin();
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT) && !level.getBlockState(base.below()).is(Blocks.MYCELIUM)) return false;
        int kind = r.nextInt(3);                     // 0 red dome, 1 brown shelf, 2 violet dome
        int height = 28 + r.nextInt(21);
        if (base.getY() + height + 10 >= level.getMaxBuildHeight()) return false;
        double stemR = 1.6 + r.nextDouble() * 1.0;
        double leanA = r.nextDouble() * Math.PI * 2, lean = 1.0 + r.nextDouble() * 1.5;

        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
        double tx = 0, tz = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = -2; y <= height; y++) {
            double t = Math.max(0, y) / (double) height;
            // Lean grows with height and curls back a little near the top.
            tx = Math.cos(leanA) * lean * Math.sin(t * Math.PI * 0.8);
            tz = Math.sin(leanA) * lean * Math.sin(t * Math.PI * 0.8);
            double rad = stemR * (1.25 - 0.35 * t) + (y < 3 ? (3 - y) * 0.35 : 0);   // flared foot
            fillDisc(level, base, tx, y, tz, rad, stem, p);
        }

        BlockState cap = switch (kind) {
            case 1 -> Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
            case 2 -> ModNature.VIOLET_CAP.get().defaultBlockState();
            default -> Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
        };
        if (cap.hasProperty(HugeMushroomBlock.DOWN)) cap = cap.setValue(HugeMushroomBlock.DOWN, false);
        BlockState gills = ModNature.GLOWCAP_GILLS.get().defaultBlockState();
        double capR = kind == 1 ? 11 + r.nextDouble() * 2.5 : 8 + r.nextDouble() * 3;
        double capH = kind == 1 ? 2.5 : 5 + r.nextDouble() * 3;
        int top = height;
        for (int dx = -REACH; dx <= REACH; dx++) for (int dz = -REACH; dz <= REACH; dz++) {
            double fx = dx - tx, fz = dz - tz;
            double d = Math.sqrt(fx * fx + fz * fz) / capR;
            if (d > 1.0) continue;
            // Dome: height falls off with distance; shelf: nearly flat with a drooping rim.
            double h = kind == 1 ? capH * (1 - d * d * 0.6) - Math.max(0, d - 0.8) * 6 : capH * Math.sqrt(1 - d * d);
            double rimDrop = kind == 1 ? 0 : Math.max(0, d - 0.75) * 7;   // domes curl down at the edge
            int y1 = top + (int) Math.round(h - rimDrop);
            int y0 = top - (int) Math.round(rimDrop) - (kind == 1 ? 0 : 1);
            for (int y = y0; y <= y1; y++) {
                boolean shell = y >= y1 - 1 || d > 0.9;
                p.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
                if (!level.getBlockState(p).isAir() && !level.getBlockState(p).canBeReplaced()) continue;
                level.setBlock(p, y == y0 && d < 0.93 && !shell ? gills : cap, 2);
            }
            // Glowing gills hang a block lower in rings.
            if (d > 0.25 && d < 0.9 && ((int) (d * 12)) % 2 == 0) {
                p.set(base.getX() + dx, base.getY() + y0 - 1, base.getZ() + dz);
                if (level.getBlockState(p).isAir()) level.setBlock(p, gills, 2);
            }
        }
        return true;
    }

    private static void fillDisc(WorldGenLevel level, BlockPos base, double ox, int y, double oz, double rad,
                                 BlockState state, BlockPos.MutableBlockPos p) {
        int ri = (int) Math.ceil(rad) + 1;
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            double fx = dx - ox, fz = dz - oz;
            if (fx * fx + fz * fz > rad * rad || Math.abs(dx) > REACH || Math.abs(dz) > REACH) continue;
            p.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
            BlockState cur = level.getBlockState(p);
            if (cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.LEAVES) || (y < 0 && cur.is(BlockTags.DIRT)))
                level.setBlock(p, state, 2);
        }
    }
}
