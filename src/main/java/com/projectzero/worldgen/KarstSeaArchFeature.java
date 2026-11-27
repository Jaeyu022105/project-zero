package com.projectzero.worldgen;

import com.projectzero.registry.ModNature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** An occasional limestone sea arch rising from the shallow Karst Tower Bay floor. */
public class KarstSeaArchFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 16;

    public KarstSeaArchFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();
        int sea = level.getSeaLevel();
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        if (sea - floor > 24) return false;
        int width = 12 + r.nextInt(7);
        int half = width / 2;
        int spring = sea + 2;
        int rise = 7 + r.nextInt(8);
        if (spring + rise + 4 >= level.getMaxBuildHeight()) return false;
        boolean alongX = r.nextBoolean();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        // Build two stout, tapering feet from the bay floor up to the spring of the arch.
        for (int side : new int[]{-1, 1}) {
            int x = side * half;
            double radius = 1.8 + r.nextDouble() * 0.5;
            for (int y = floor - 1; y <= spring; y++) {
                double taper = radius - Math.max(0, (y - floor) / (double) Math.max(1, spring - floor)) * 0.55;
                for (int a = -(int) Math.ceil(radius); a <= Math.ceil(radius); a++)
                    for (int b = -(int) Math.ceil(radius); b <= Math.ceil(radius); b++) {
                        if (a * a + b * b > taper * taper) continue;
                        p.set(origin.getX() + (alongX ? x + a : a), y,
                                origin.getZ() + (alongX ? b : x + b));
                        if (Math.abs(p.getX() - origin.getX()) > REACH || Math.abs(p.getZ() - origin.getZ()) > REACH) continue;
                        if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()
                                || level.getBlockState(p).getFluidState().isSource())
                            level.setBlock(p, stone(y + a + b), 2);
                    }
            }
        }

        int samples = width * 2;
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            int x = (int) Math.round(-half + width * t);
            int y = spring + (int) Math.round(Math.sin(Math.PI * t) * rise);
            for (int across = -2; across <= 2; across++) for (int dy = -1; dy <= 1; dy++) {
                if (Math.abs(across) == 2 && dy > 0 && r.nextFloat() < 0.3F) continue;
                p.set(origin.getX() + (alongX ? x : across), y + dy,
                        origin.getZ() + (alongX ? across : x));
                if (Math.abs(p.getX() - origin.getX()) > REACH || Math.abs(p.getZ() - origin.getZ()) > REACH) continue;
                if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()
                        || level.getBlockState(p).getFluidState().isSource())
                    level.setBlock(p, stone(y + dy), 2);
            }
            p.set(origin.getX() + (alongX ? x : 0), y + 2, origin.getZ() + (alongX ? 0 : x));
            if (level.getBlockState(p).isAir() && r.nextFloat() < 0.38F)
                level.setBlock(p, r.nextBoolean() ? Blocks.MOSS_CARPET.defaultBlockState() : Blocks.FERN.defaultBlockState(), 2);
        }

        // Short curtains cling to the two sheltered sides of the span.
        for (int i = 2; i < samples; i += 3) {
            if (r.nextFloat() > 0.45F) continue;
            double t = i / (double) samples;
            int x = (int) Math.round(-half + width * t);
            int y = spring + (int) Math.round(Math.sin(Math.PI * t) * rise) - 2;
            int side = r.nextBoolean() ? -1 : 1;
            Direction face = alongX ? (side < 0 ? Direction.NORTH : Direction.SOUTH)
                    : (side < 0 ? Direction.WEST : Direction.EAST);
            BlockPos.MutableBlockPos vine = new BlockPos.MutableBlockPos(
                    origin.getX() + (alongX ? x : side * 3), y,
                    origin.getZ() + (alongX ? side * 3 : x));
            BlockState vineState = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(face), true);
            for (int k = 0; k < 4 && level.getBlockState(vine).isAir(); k++) {
                if (level.getBlockState(vine.relative(face.getOpposite())).isAir()) break;
                level.setBlock(vine, vineState, 2);
                vine.move(Direction.DOWN);
            }
        }
        return true;
    }

    private static BlockState stone(int seed) {
        return switch (Math.floorMod(seed, 10)) {
            case 0, 1 -> Blocks.CALCITE.defaultBlockState();
            case 2 -> Blocks.DIORITE.defaultBlockState();
            default -> ModNature.LIMESTONE.get().defaultBlockState();
        };
    }
}
