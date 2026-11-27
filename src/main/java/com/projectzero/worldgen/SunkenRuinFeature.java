package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Sunken Ruins Marsh: what's left of an older civilisation, half-drowned. One of a broken round
 * tower (with a chest at the bottom), a toppled column, or a weathered statue leaning in the mud.
 */
public class SunkenRuinFeature extends Feature<NoneFeatureConfiguration> {
    public SunkenRuinFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin().below(2);
        float roll = r.nextFloat();
        if (roll < 0.4F) tower(level, o, r);
        else if (roll < 0.75F) column(level, o.above(2), r);
        else statue(level, o.above(1), r);
        return true;
    }

    private static BlockState brick(RandomSource r) {
        float f = r.nextFloat();
        return (f < 0.4F ? Blocks.MOSSY_STONE_BRICKS : f < 0.7F ? Blocks.STONE_BRICKS : f < 0.9F ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
    }

    private static boolean free(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.DIRT) || s.is(Blocks.MUD) || s.is(BlockTags.LEAVES) || !s.getFluidState().isEmpty();
    }

    private static void tower(WorldGenLevel level, BlockPos o, RandomSource r) {
        int R = 3 + r.nextInt(3), h = 6 + r.nextInt(12);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = -2; y < h; y++) for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R + 0.4 || d < R - 0.8) continue;
            // Jagged broken top: higher on one side.
            double top = h - 4 + 4 * Math.sin(Math.atan2(dz, dx) + h) + r.nextInt(2);
            if (y > top) continue;
            if (y == 2 && dx == R && Math.abs(dz) <= 0) continue;   // doorway
            p.set(o.getX() + dx, o.getY() + y, o.getZ() + dz);
            if (free(level, p)) level.setBlock(p, brick(r), 2);
        }
        BlockPos chest = o.offset(0, -1, 0);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(r))
                .setValue(ChestBlock.WATERLOGGED, !level.getFluidState(chest).isEmpty()), 2);
        if (level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity c)
            c.setLootTable(BuiltInLootTables.UNDERWATER_RUIN_BIG, r.nextLong());
    }

    private static void column(WorldGenLevel level, BlockPos o, RandomSource r) {
        Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        int len = 6 + r.nextInt(8);
        BlockPos.MutableBlockPos p = o.mutable();
        // Standing stump, then the fallen shaft lying in the mud.
        for (int y = 0; y < 2 + r.nextInt(3); y++) if (free(level, o.above(y))) level.setBlock(o.above(y), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        p.move(d, 2);
        for (int k = 0; k < len; k++) {
            if (r.nextFloat() < 0.15F) { p.move(d); continue; }   // broken into drums
            BlockState drum = r.nextFloat() < 0.5F
                    ? Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, d.getAxis())
                    : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            if (free(level, p)) level.setBlock(p, drum, 2);
            p.move(d);
        }
    }

    private static void statue(WorldGenLevel level, BlockPos o, RandomSource r) {
        BlockState stone = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState moss = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        int[][] shape = { // dx, dy: legs, body, shoulders, head (2 wide figure, 9 tall)
                {0, 0}, {1, 0}, {0, 1}, {1, 1}, {0, 2}, {1, 2}, {0, 3}, {1, 3}, {0, 4}, {1, 4}, {0, 5}, {1, 5},
                {-1, 5}, {2, 5}, {-1, 4}, {2, 6}, {2, 7}, {0, 6}, {1, 6}, {0, 7}, {1, 7}};
        for (int[] s : shape) {
            int shift = s[1] / 4;   // leans over as it rises
            BlockPos at = o.offset(s[0], s[1], 0).relative(lean, shift);
            if (free(level, at)) level.setBlock(at, r.nextFloat() < 0.25F ? moss : stone, 2);
        }
    }
}
