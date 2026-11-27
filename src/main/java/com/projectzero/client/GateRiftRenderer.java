package com.projectzero.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import com.projectzero.fx.Fx;
import com.projectzero.gates.GateBlock;
import com.projectzero.gates.GateBlockEntity;
import com.projectzero.gates.GateRank;
import com.projectzero.gates.RiftShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Gates are drawn as a free-floating rift: a thin bright ring around a dark swirling core,
 * in the gate's rank color (exit rifts pale cyan, sea gates swirl teal). Motes are pulled into
 * the core from all around. A broken gate (dungeon break) jitters, flickers and bleeds red.
 * The portal blocks are invisible; only the lowest-corner block draws the whole rift.
 */
public class GateRiftRenderer implements BlockEntityRenderer<GateBlockEntity> {
    private static final ResourceLocation CORE = ProjectZero.id("textures/entity/gate/rift_core.png");
    private static final ResourceLocation SWIRL = ProjectZero.id("textures/entity/gate/rift_swirl.png");
    private static final ResourceLocation RING = ProjectZero.id("textures/entity/gate/rift_ring.png");
    private static final ResourceLocation HALO = ProjectZero.id("textures/entity/gate/rift_halo.png");
    private static final int EXIT_COLOR = 0x9FF4FF;
    private static final int SEA_COLOR = 0x2FD8C8;
    private static final int BROKEN_COLOR = 0xFF2020;

    /** Last game tick each rift spawned its inflow motes (one batch per tick, not per frame). */
    private static final Map<GateBlockEntity, Long> LAST_SPAWN = new WeakHashMap<>();

    public GateRiftRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(GateBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof GateBlock)) return;
        RiftShape shape = RiftShape.ifCorner(level, be.getBlockPos());
        if (shape == null) return;

        boolean exit = state.getValue(GateBlock.EXIT);
        boolean sea = state.getValue(GateBlock.SEA);
        boolean broken = state.getValue(GateBlock.BROKEN);
        GateRank rank = state.getValue(GateBlock.RANK);
        int rgb = exit ? EXIT_COLOR : rank.argb() & 0xFFFFFF;
        float r = ((rgb >> 16) & 0xFF) / 255F, g = ((rgb >> 8) & 0xFF) / 255F, b = (rgb & 0xFF) / 255F;
        int swirlRgb = sea ? SEA_COLOR : broken ? BROKEN_COLOR : rgb;
        float sr = ((swirlRgb >> 16) & 0xFF) / 255F, sg = ((swirlRgb >> 8) & 0xFF) / 255F, sb = (swirlRgb & 0xFF) / 255F;

        float radius = shape.radius();
        long gameTime = level.getGameTime();
        float t = (gameTime + partialTick) / 20F;
        float breathe = 1F + 0.025F * (float) Math.sin(t * 2.1F);
        float spinMul = broken ? 2.2F : 1F;
        if (broken) {
            // Unstable: random flicker and a shaking ring.
            RandomSource rs = RandomSource.create(gameTime * 31L + be.getBlockPos().asLong());
            breathe *= 0.94F + rs.nextFloat() * 0.12F;
            if (rs.nextInt(6) == 0) { r = g = b = 1F; }
        }

        spawnInflow(level, be, shape, gameTime, sea ? SEA_COLOR : broken ? BROKEN_COLOR : rgb, broken);

        pose.pushPose();
        Vec3 c = shape.center();
        pose.translate(c.x - be.getBlockPos().getX(), c.y - be.getBlockPos().getY(), c.z - be.getBlockPos().getZ());
        if (!shape.alongX()) pose.mulPose(Axis.YP.rotationDegrees(90F));

        disc(pose, buffers.getBuffer(RenderType.eyes(HALO)), radius * 1.45F * breathe, t * 8F, r * 0.55F, g * 0.55F, b * 0.55F, 0F);
        disc(pose, buffers.getBuffer(RenderType.entityTranslucent(CORE)), radius * 0.98F, -t * 12F * spinMul, 1F, 1F, 1F, -0.01F);
        disc(pose, buffers.getBuffer(RenderType.eyes(SWIRL)), radius * 0.95F, -t * 70F * spinMul, sr, sg, sb, 0.01F);
        disc(pose, buffers.getBuffer(RenderType.eyes(SWIRL)), radius * 0.7F, t * 45F * spinMul + 60F,
                sr * 0.6F + 0.4F, sg * 0.6F + 0.4F, sb * 0.6F + 0.4F, 0.015F);
        disc(pose, buffers.getBuffer(RenderType.eyes(RING)), radius * breathe, t * 20F, r * 0.5F + 0.5F, g * 0.5F + 0.5F, b * 0.5F + 0.5F, 0.02F);
        if (broken) {
            // A second, offset ring: the rift is tearing.
            disc(pose, buffers.getBuffer(RenderType.eyes(RING)), radius * 1.12F, -t * 35F, 1F, 0.15F, 0.1F, 0.025F);
        }
        pose.popPose();
    }

    /** Motes appear around the rift and get pulled into its core. */
    private static void spawnInflow(Level level, GateBlockEntity be, RiftShape shape, long gameTime, int rgb, boolean broken) {
        Long last = LAST_SPAWN.get(be);
        if (last != null && last == gameTime) return;
        LAST_SPAWN.put(be, gameTime);
        if (Minecraft.getInstance().isPaused()) return;
        Vec3 c = shape.center();
        float radius = shape.radius();
        RandomSource rs = level.random;
        int count = broken ? 1 : 2;
        for (int i = 0; i < count; i++) {
            // A random point on a sphere shell 1.4x-2.4x the ring radius, flattened toward the portal plane.
            double u = rs.nextDouble() * 2 - 1, a = rs.nextDouble() * Math.PI * 2;
            double s = Math.sqrt(1 - u * u);
            double dist = radius * (1.4 + rs.nextDouble());
            Vec3 across = shape.across();
            Vec3 normal = new Vec3(across.z, 0, across.x);
            Vec3 start = c.add(across.scale(Math.cos(a) * s * dist)).add(0, Math.sin(a) * s * dist, 0)
                    .add(normal.scale(u * dist * 0.6));
            Vec3 v = c.subtract(start).scale(1 / Fx.INFLOW_DIVISOR);
            level.addParticle(Fx.inflow(rgb, 1.2F + rs.nextFloat() * 0.6F), start.x, start.y, start.z, v.x, v.y, v.z);
        }
        if (broken && rs.nextInt(3) == 0) {
            // Bleeding outward too.
            level.addParticle(Fx.spark(BROKEN_COLOR, 1.4F), c.x, c.y, c.z,
                    (rs.nextDouble() - 0.5) * 0.3, (rs.nextDouble() - 0.5) * 0.3, (rs.nextDouble() - 0.5) * 0.3);
        }
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
