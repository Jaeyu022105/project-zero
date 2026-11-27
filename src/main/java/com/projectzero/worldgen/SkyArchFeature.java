package com.projectzero.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A self-supporting banded arch and small mossy boulder heaps in the Sky Pillars valley. */
public class SkyArchFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 16;

    public SkyArchFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();
        int width = 10 + r.nextInt(15);
        int height = 30 + r.nextInt(51);
        if (origin.getY() + height + 12 >= level.getMaxBuildHeight()) return false;
        int direction = r.nextBoolean() ? 0 : 1;
        int half = width / 2;
        int springY = height - 9;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        // Two tapered stubs carry the span; neither relies on existing terrain formations.
        for (int side : new int[]{-1, 1}) {
            int x = side * half;
            int stubHeight = springY;
            double radius = 1.7 + r.nextDouble() * 0.9;
            for (int y = 0; y <= stubHeight; y++) {
                double taper = radius - (y / (double) stubHeight) * 0.5;
                for (int a = -(int) Math.ceil(radius); a <= Math.ceil(radius); a++)
                    for (int b = -(int) Math.ceil(radius); b <= Math.ceil(radius); b++) {
                        double edge = taper + Math.sin((a * 1.7 + b) * 2 + y * 0.12) * 0.22;
                        if (a * a + b * b > edge * edge) continue;
                        p.set(origin.getX() + (direction == 0 ? x + a : a), origin.getY() + y,
                                origin.getZ() + (direction == 0 ? b : x + b));
                        if (Math.abs(p.getX() - origin.getX()) > REACH || Math.abs(p.getZ() - origin.getZ()) > REACH) continue;
                        if (replaceable(level.getBlockState(p))) level.setBlock(p, band(y), 2);
                    }
            }
            // A grassy cap makes each support read as a planted pillar stub.
            p.set(origin.getX() + (direction == 0 ? x : 0), origin.getY() + stubHeight + 1,
                    origin.getZ() + (direction == 0 ? 0 : x));
            if (level.getBlockState(p).isAir()) level.setBlock(p, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        }

        // A rough, two-block-thick curved span rises between the supports.
        int samples = width * 2;
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            double curve = Math.sin(Math.PI * t);
            int x = (int) Math.round(-half + width * t);
            int y = springY + (int) Math.round(curve * (height - springY));
            for (int across = -1; across <= 1; across++) for (int dy = -1; dy <= 1; dy++) {
                if (Math.abs(across) == 1 && dy == 1 && r.nextFloat() < 0.18F) continue;
                p.set(origin.getX() + (direction == 0 ? x : across), origin.getY() + y + dy,
                        origin.getZ() + (direction == 0 ? across : x));
                if (Math.abs(p.getX() - origin.getX()) > REACH || Math.abs(p.getZ() - origin.getZ()) > REACH) continue;
                if (replaceable(level.getBlockState(p))) level.setBlock(p, band(y + dy), 2);
            }
            // Moss and tufts sit on the top edge of the span.
            p.set(origin.getX() + (direction == 0 ? x : 0), origin.getY() + y + 2,
                    origin.getZ() + (direction == 0 ? 0 : x));
            if (level.getBlockState(p).isAir() && r.nextFloat() < 0.45F)
                level.setBlock(p, r.nextFloat() < 0.65F ? Blocks.MOSS_CARPET.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState(), 2);
        }

        // Hang short vine curtains from the underside; all stay close to the arch centreline.
        for (int i = 2; i < samples - 1; i += 2) {
            double t = i / (double) samples;
            int x = (int) Math.round(-half + width * t);
            int y = springY + (int) Math.round(Math.sin(Math.PI * t) * (height - springY)) - 2;
            if (r.nextFloat() > 0.48F) continue;
            int side = r.nextBoolean() ? -1 : 1;
            BlockPos.MutableBlockPos vine = new BlockPos.MutableBlockPos(
                    origin.getX() + (direction == 0 ? x : side * 2), origin.getY() + y,
                    origin.getZ() + (direction == 0 ? side * 2 : x));
            Direction face = direction == 0 ? (side < 0 ? Direction.EAST : Direction.WEST)
                    : (side < 0 ? Direction.SOUTH : Direction.NORTH);
            BlockState vineState = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(face), true);
            int len = 2 + r.nextInt(7);
            for (int k = 0; k < len && vine.getY() > origin.getY() && level.getBlockState(vine).isAir(); k++) {
                level.setBlock(vine, vineState, 2);
                vine.move(Direction.DOWN);
            }
        }

        // A small wind-bent pine grows from the crown of the span.
        int pineX = (int) Math.round((r.nextDouble() - 0.5) * width * 0.35);
        int pineY = springY + height - springY;
        pine(level, origin.offset(direction == 0 ? pineX : 0, pineY + 2, direction == 0 ? 0 : pineX), r);

        // Mossy boulder heaps and ferns mark the valley floor below each support.
        for (int side : new int[]{-1, 1}) {
            int x = side * Math.max(3, half - 2);
            BlockPos heap = origin.offset(direction == 0 ? x : 0, 0, direction == 0 ? 0 : x);
            boulder(level, heap, r);
        }
        return true;
    }

    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.DIRT)
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    private static BlockState band(int y) {
        return switch (Math.floorMod(y / 4, 8)) {
            case 1, 5 -> Blocks.ANDESITE.defaultBlockState();
            case 3 -> Blocks.TUFF.defaultBlockState();
            case 7 -> Blocks.CALCITE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    private static void pine(WorldGenLevel level, BlockPos at, RandomSource r) {
        int h = 5 + r.nextInt(4);
        Direction bend = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockPos.MutableBlockPos p = at.mutable();
        for (int y = 0; y < h; y++) {
            if (y == h / 2) p.move(bend);
            if (Math.abs(p.getX() - at.getX()) <= REACH && Math.abs(p.getZ() - at.getZ()) <= REACH
                    && (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()))
                level.setBlock(p, Blocks.SPRUCE_LOG.defaultBlockState(), 2);
            int radius = y < 2 ? 1 : Math.max(1, (h - y) / 2);
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > radius + 1) continue;
                BlockPos leaf = p.offset(dx, 0, dz);
                if (level.getBlockState(leaf).isAir()) level.setBlock(leaf, leaves, 2);
            }
            p.move(Direction.UP);
        }
    }

    private static void boulder(WorldGenLevel level, BlockPos at, RandomSource r) {
        int count = 3 + r.nextInt(3);
        for (int i = 0; i < count; i++) {
            int cx = r.nextInt(7) - 3, cz = r.nextInt(7) - 3;
            int rx = 1 + r.nextInt(2), ry = 1 + r.nextInt(2), rz = 1 + r.nextInt(2);
            for (int dx = -rx; dx <= rx; dx++) for (int dy = -ry; dy <= ry; dy++) for (int dz = -rz; dz <= rz; dz++) {
                if (dx * dx / (double) (rx * rx + 1) + dy * dy / (double) (ry * ry + 1)
                        + dz * dz / (double) (rz * rz + 1) > 1) continue;
                BlockPos pos = at.offset(cx + dx, dy, cz + dz);
                if (Math.abs(pos.getX() - at.getX()) > 5 || Math.abs(pos.getZ() - at.getZ()) > 5) continue;
                BlockState current = level.getBlockState(pos);
                if (!replaceable(current)) continue;
                level.setBlock(pos, r.nextFloat() < 0.28F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : band(pos.getY() - at.getY()), 2);
            }
        }
        for (int i = 0; i < 3; i++) {
            BlockPos fern = at.offset(r.nextInt(7) - 3, 2, r.nextInt(7) - 3);
            if (Math.abs(fern.getX() - at.getX()) > 5 || Math.abs(fern.getZ() - at.getZ()) > 5) continue;
            if (level.getBlockState(fern).isAir() && level.getBlockState(fern.below()).isSolid())
                level.setBlock(fern, Blocks.FERN.defaultBlockState(), 2);
        }
    }
}
