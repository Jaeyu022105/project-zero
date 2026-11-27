package com.projectzero.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.projectzero.registry.ModFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

import java.util.List;

/**
 * Buttress roots for any tree: the trunk flares out at the base, then roots run outward and dip into
 * the ground, lying in the grass. Added to vanilla's own tree configs, so every oak, birch, spruce,
 * jungle, acacia, dark oak and cherry (natural or grown from a sapling) gets them.
 */
public class RootsDecorator extends TreeDecorator {
    public static final MapCodec<RootsDecorator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("provider").forGetter(d -> d.provider),
            Codec.floatRange(0F, 1F).fieldOf("chance").forGetter(d -> d.chance),
            Codec.intRange(1, 6).fieldOf("max_length").forGetter(d -> d.maxLength)
    ).apply(i, RootsDecorator::new));

    private final BlockStateProvider provider;
    private final float chance;
    private final int maxLength;

    public RootsDecorator(BlockStateProvider provider, float chance, int maxLength) {
        this.provider = provider;
        this.chance = chance;
        this.maxLength = maxLength;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModFeatures.ROOTS.get();
    }

    @Override
    public void place(Context ctx) {
        List<BlockPos> logs = ctx.logs();
        if (logs.isEmpty()) return;
        int baseY = Integer.MAX_VALUE;
        for (BlockPos p : logs) baseY = Math.min(baseY, p.getY());
        RandomSource r = ctx.random();
        int bases = 0;
        for (BlockPos p : logs) if (p.getY() == baseY) bases++;
        boolean thick = bases >= 4;
        for (BlockPos b : logs) {
            if (b.getY() != baseY) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (r.nextFloat() > chance) continue;
                if (ctx.level().isStateAtPosition(b.relative(d), s -> s.is(BlockTags.LOGS))) continue;   // inside a 2x2 trunk
                int len = 1 + r.nextInt(maxLength + (thick ? 2 : 0));
                root(ctx, b, d, len, thick, r);
            }
        }
    }

    private void root(Context ctx, BlockPos base, Direction d, int len, boolean thick, RandomSource r) {
        // Flare: an upright log against the trunk (two high on big trees).
        BlockPos flare = base.relative(d);
        if (!soft(ctx, flare)) return;
        ctx.setBlock(flare, log(ctx, r, flare, Direction.Axis.Y));
        if (thick && r.nextBoolean() && soft(ctx, flare.above())) ctx.setBlock(flare.above(), log(ctx, r, flare.above(), Direction.Axis.Y));
        // Then the root itself runs outward, sunk one block into the ground and sometimes drifting sideways.
        BlockPos.MutableBlockPos p = flare.mutable();
        Direction side = r.nextBoolean() ? d.getClockWise() : d.getCounterClockWise();
        for (int k = 1; k < len; k++) {
            p.move(d);
            if (r.nextFloat() < 0.25F) p.move(side);
            p.setY(base.getY() - 1);
            // Follow the slope down a little.
            for (int drop = 0; drop < 3 && air(ctx, p.below()); drop++) p.move(Direction.DOWN);
            if (!soft(ctx, p)) break;
            ctx.setBlock(p.immutable(), log(ctx, r, p, d.getAxis()));
        }
    }

    private BlockState log(Context ctx, RandomSource r, BlockPos at, Direction.Axis axis) {
        BlockState s = provider.getState(r, at);
        return s.hasProperty(RotatedPillarBlock.AXIS) ? s.setValue(RotatedPillarBlock.AXIS, axis) : s;
    }

    private static boolean air(Context ctx, BlockPos p) {
        return ctx.level().isStateAtPosition(p, BlockState::isAir);
    }

    /** Ground or plants a root may take over: never stone, water, other logs or buildings. */
    private static boolean soft(Context ctx, BlockPos p) {
        return ctx.level().isStateAtPosition(p, s -> s.isAir() || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND)
                || s.is(Blocks.SNOW) || s.is(BlockTags.REPLACEABLE_BY_TREES) && s.getFluidState().isEmpty());
    }
}
