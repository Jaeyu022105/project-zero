package com.projectzero.gates;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.projectzero.fx.Fx;

import java.util.Locale;

/**
 * One carved piece of an ancient gate's stone ring (see {@link AncientGate}). Never a full block:
 * each piece is the slice of a circular band that falls in its cell ({@link AncientStonePiece}), so
 * a ring of them reads as a circle rather than a square frame. Runes on the faces glow in the gate's rank color while
 * the gate is open ({@link #LIT}) and go dark once it is cleared.
 */
public class AncientGateStoneBlock extends Block {
    public static final MapCodec<AncientGateStoneBlock> CODEC = simpleCodec(AncientGateStoneBlock::new);

    /** Stone the ring is carved from, matched to the biome (see {@link AncientGate.Palette}). */
    public enum Material implements StringRepresentable {
        STONE, MOSSY, SANDSTONE, RED_SANDSTONE, MUD, DEEPSLATE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<AncientStonePiece> PIECE = EnumProperty.create("piece", AncientStonePiece.class);
    public static final EnumProperty<Material> MATERIAL = EnumProperty.create("material", Material.class);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<GateRank> RANK = GateBlock.RANK;

    public AncientGateStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(PIECE, AncientStonePiece.RUBBLE)
                .setValue(MATERIAL, Material.STONE)
                .setValue(AXIS, Direction.Axis.X)
                .setValue(LIT, false)
                .setValue(RANK, GateRank.E));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PIECE, MATERIAL, AXIS, LIT, RANK);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PIECE).shape(state.getValue(AXIS) == Direction.Axis.X);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /** Live runes shed a faint rank-colored mote now and then; dark runes are just stone. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!glows(state) || random.nextInt(6) != 0) return;
        int rgb = state.getValue(RANK).argb() & 0xFFFFFF;
        level.addParticle(Fx.mote(rgb, 0.8F),
                pos.getX() + 0.2 + random.nextDouble() * 0.6,
                pos.getY() + 0.2 + random.nextDouble() * 0.6,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                0.0, 0.015, 0.0);
    }

    public static int lightLevel(BlockState state) {
        return glows(state) ? 6 : 0;
    }

    /** Paving uses LIT only to pick the clean or cracked texture; it has no runes to light. */
    private static boolean glows(BlockState state) {
        return state.getValue(LIT) && state.getValue(PIECE).kind() != AncientStonePiece.Kind.PLATE;
    }
}
