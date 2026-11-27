package com.projectzero.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import com.projectzero.gates.GateBlock;
import com.projectzero.gates.GateBlockEntity;
import com.projectzero.gates.GateRank;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Gates are drawn as a free-floating rift: a thin bright ring around a dark swirling core,
 * in the gate's rank color (exit rifts are pale cyan). The portal blocks themselves are
 * invisible; only the lowest-corner block of each portal draws the whole rift.
 */
public class GateRiftRenderer implements BlockEntityRenderer<GateBlockEntity> {
    private static final ResourceLocation CORE = ProjectZero.id("textures/entity/gate/rift_core.png");
    private static final ResourceLocation SWIRL = ProjectZero.id("textures/entity/gate/rift_swirl.png");
    private static final ResourceLocation RING = ProjectZero.id("textures/entity/gate/rift_ring.png");
    private static final ResourceLocation HALO = ProjectZero.id("textures/entity/gate/rift_halo.png");
    private static final int EXIT_COLOR = 0xFF9FF4FF;

    public GateRiftRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(GateBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof GateBlock)) return;

        boolean alongX = planeAlongX(level, pos);
        Direction side = alongX ? Direction.WEST : Direction.NORTH;
        // Only the min corner draws.
        if (isGate(level, pos.relative(side)) || isGate(level, pos.below())) return;
        int w = 1, h = 1;
        while (w < 16 && isGate(level, pos.relative(side.getOpposite(), w))) w++;
        while (h < 16 && isGate(level, pos.above(h))) h++;

        boolean exit = state.getValue(GateBlock.EXIT);
        GateRank rank = state.getValue(GateBlock.RANK);
        int rgb = exit ? EXIT_COLOR : rank.argb();
        float r = ((rgb >> 16) & 0xFF) / 255F, g = ((rgb >> 8) & 0xFF) / 255F, b = (rgb & 0xFF) / 255F;

        float radius = Math.max(w, h) * 0.62F + 0.35F;
        float t = (level.getGameTime() + partialTick) / 20F;
        float breathe = 1F + 0.025F * (float) Math.sin(t * 2.1F);

        pose.pushPose();
        // Centre of the portal block area.
        if (alongX) pose.translate(w / 2F, h / 2F, 0.5F);
        else pose.translate(0.5F, h / 2F, w / 2F);
        if (!alongX) pose.mulPose(Axis.YP.rotationDegrees(90F));

        // Halo (soft, wide), dark core, two counter-rotating swirls, thin ring.
        disc(pose, buffers.getBuffer(RenderType.eyes(HALO)), radius * 1.45F * breathe, t * 8F, r * 0.55F, g * 0.55F, b * 0.55F, 0F);
        disc(pose, buffers.getBuffer(RenderType.entityTranslucent(CORE)), radius * 0.98F, -t * 12F, 1F, 1F, 1F, -0.01F);
        disc(pose, buffers.getBuffer(RenderType.eyes(SWIRL)), radius * 0.95F, -t * 70F, r, g, b, 0.01F);
        disc(pose, buffers.getBuffer(RenderType.eyes(SWIRL)), radius * 0.7F, t * 45F + 60F, r * 0.6F + 0.4F, g * 0.6F + 0.4F, b * 0.6F + 0.4F, 0.015F);
        disc(pose, buffers.getBuffer(RenderType.eyes(RING)), radius * breathe, t * 20F, r * 0.5F + 0.5F, g * 0.5F + 0.5F, b * 0.5F + 0.5F, 0.02F);
        pose.popPose();
    }

    /** A square textured quad of half-size {@code half}, spun {@code deg} degrees, drawn both ways round. */
    private static void disc(PoseStack pose, VertexConsumer vc, float half, float deg,
                             float r, float g, float b, float depth) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(deg % 360F));
        PoseStack.Pose p = pose.last();
        quad(vc, p, half, depth, r, g, b, 1F);
        quad(vc, p, half, -depth, r, g, b, -1F);
        pose.popPose();
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, float s, float z,
                             float r, float g, float b, float facing) {
        float[][] corners = facing > 0
                ? new float[][] {{-s, -s, 0, 1}, {s, -s, 1, 1}, {s, s, 1, 0}, {-s, s, 0, 0}}
                : new float[][] {{-s, s, 0, 0}, {s, s, 1, 0}, {s, -s, 1, 1}, {-s, -s, 0, 1}};
        for (float[] c : corners) {
            vc.addVertex(p, c[0], c[1], z)
                    .setColor(r, g, b, 1F)
                    .setUv(c[2], c[3])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(p, 0F, 0F, facing);
        }
    }

    private static boolean isGate(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof GateBlock;
    }

    /** True when the portal is a wall along the X axis (you walk through it along Z). */
    private static boolean planeAlongX(Level level, BlockPos pos) {
        if (isGate(level, pos.east()) || isGate(level, pos.west())) return true;
        if (isGate(level, pos.north()) || isGate(level, pos.south())) return false;
        return level.getBlockState(pos.north()).isAir() || level.getBlockState(pos.south()).isAir();
    }

    @Override
    public boolean shouldRenderOffScreen(GateBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRender(GateBlockEntity be, Vec3 cameraPos) {
        return Vec3.atCenterOf(be.getBlockPos()).closerThan(cameraPos, getViewDistance());
    }
}
