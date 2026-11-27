package com.projectzero.worldgen;

import com.projectzero.registry.ModBlocks;
import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Mana Wastes debris, one of three kinds: a rock torn loose and hanging in the air with crystal
 * spikes under it; a broken ring of a long-dead gate standing upright; or a crystal growth bursting
 * out of the ground.
 */
public class ManaWasteFeature extends Feature<NoneFeatureConfiguration> {
    public ManaWasteFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        float roll = r.nextFloat();
        if (roll < 0.55F) floatingRock(level, o.above(6 + r.nextInt(26)), r);
        else if (roll < 0.75F) deadGate(level, o, r);
        else crystalBurst(level, o, r);
        return true;
    }

    private static void floatingRock(WorldGenLevel level, BlockPos c, RandomSource r) {
        double rad = 2.5 + r.nextDouble() * 3.5;
        int ri = (int) Math.ceil(rad);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz) / rad;
            if (d > 1) continue;
            int down = (int) Math.round((1 - d) * rad * 1.4) + 1;
            for (int dy = -down; dy <= 1; dy++) {
                p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                if (!level.getBlockState(p).isAir()) continue;
                float f = r.nextFloat();
                level.setBlock(p, (dy == 1 ? (f < 0.6F ? Blocks.CALCITE : Blocks.TUFF)
                        : f < 0.6F ? Blocks.STONE : f < 0.85F ? Blocks.TUFF : Blocks.AMETHYST_BLOCK).defaultBlockState(), 2);
            }
            // Crystal points hanging from the underside.
            if (d < 0.5 && r.nextFloat() < 0.25F) {
                BlockPos under = new BlockPos(c.getX() + dx, c.getY() - down - 1, c.getZ() + dz);
                if (level.getBlockState(under).isAir())
                    level.setBlock(under, ModBlocks.MANA_CRYSTAL.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 2);
            }
        }
    }

    private static void deadGate(WorldGenLevel level, BlockPos o, RandomSource r) {
        int R = 5 + r.nextInt(4);
        boolean alongX = r.nextBoolean();
        int gap = r.nextInt(360);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int a = 0; a < 360; a += 3) {
            if (Math.abs(((a - gap + 540) % 360) - 180) < 30 + r.nextInt(10)) continue;   // the broken stretch
            double rad = Math.toRadians(a);
            int u = (int) Math.round(Math.cos(rad) * R), v = (int) Math.round(Math.sin(rad) * R) + R - 1;
            p.set(o.getX() + (alongX ? u : 0), o.getY() + v, o.getZ() + (alongX ? 0 : u));
            BlockState cur = level.getBlockState(p);
            if (!(cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.DIRT))) continue;
            float f = r.nextFloat();
            level.setBlock(p, (f < 0.45F ? Blocks.CRACKED_STONE_BRICKS : f < 0.8F ? Blocks.STONE_BRICKS : f < 0.92F ? Blocks.CHISELED_STONE_BRICKS
                    : ModNature.MANA_CRYSTAL_BLOCK.get()).defaultBlockState(), 2);
        }
        // Rubble at its foot.
        for (int i = 0; i < 8; i++) {
            BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                    o.offset(r.nextInt(R * 2 + 1) - R, 0, r.nextInt(R * 2 + 1) - R));
            if (level.getBlockState(at).isAir()) level.setBlock(at, Blocks.COBBLESTONE.defaultBlockState(), 2);
        }
    }

    private static void crystalBurst(WorldGenLevel level, BlockPos o, RandomSource r) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int spikes = 3 + r.nextInt(4);
        for (int s = 0; s < spikes; s++) {
            double yaw = r.nextDouble() * Math.PI * 2, tilt = 0.2 + r.nextDouble() * 0.6;
            int len = 3 + r.nextInt(7);
            for (int k = 0; k < len; k++) {
                p.set(o.getX() + (int) Math.round(Math.sin(tilt) * Math.cos(yaw) * k), o.getY() + (int) Math.round(Math.cos(tilt) * k),
                        o.getZ() + (int) Math.round(Math.sin(tilt) * Math.sin(yaw) * k));
                BlockState cur = level.getBlockState(p);
                if (cur.isAir() || cur.canBeReplaced())
                    level.setBlock(p, (k == len - 1 ? Blocks.AMETHYST_BLOCK : ModNature.MANA_CRYSTAL_BLOCK.get()).defaultBlockState(), 2);
            }
        }
    }
}
