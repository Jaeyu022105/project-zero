package com.projectzero.gates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The shape of one portal: its lowest corner block, which way it faces, and its size in blocks.
 * Worked out from the gate blocks themselves, so it needs no synced data. Used by the rift
 * renderer and the inflow particles.
 */
public record RiftShape(BlockPos min, boolean alongX, int width, int height) {
    /** The portal shape if {@code pos} is its min corner, else null (only one block draws a rift). */
    public static @Nullable RiftShape ifCorner(BlockGetter level, BlockPos pos) {
        boolean alongX = planeAlongX(level, pos);
        Direction side = alongX ? Direction.WEST : Direction.NORTH;
        if (isGate(level, pos.relative(side)) || isGate(level, pos.below())) return null;
        int w = 1, h = 1;
        while (w < 16 && isGate(level, pos.relative(side.getOpposite(), w))) w++;
        while (h < 16 && isGate(level, pos.above(h))) h++;
        return new RiftShape(pos, alongX, w, h);
    }

    public Vec3 center() {
        return alongX
                ? new Vec3(min.getX() + width / 2.0, min.getY() + height / 2.0, min.getZ() + 0.5)
                : new Vec3(min.getX() + 0.5, min.getY() + height / 2.0, min.getZ() + width / 2.0);
    }

    /** Ring radius in blocks: a little bigger than the block area, so the rift reads as big. */
    public float radius() {
        return Math.max(width, height) * 0.62F + 0.35F;
    }

    /** Unit vector along the portal's width. */
    public Vec3 across() {
        return alongX ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
    }

    private static boolean isGate(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof GateBlock;
    }

    /** True when the portal is a wall along the X axis (you walk through it along Z). */
    private static boolean planeAlongX(BlockGetter level, BlockPos pos) {
        if (isGate(level, pos.east()) || isGate(level, pos.west())) return true;
        if (isGate(level, pos.north()) || isGate(level, pos.south())) return false;
        return level.getBlockState(pos.north()).isAir() || level.getBlockState(pos.south()).isAir();
    }
}
