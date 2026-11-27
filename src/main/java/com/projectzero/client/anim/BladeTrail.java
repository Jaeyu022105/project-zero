package com.projectzero.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A ribbon that traces the real blade: each frame we take the sword's hilt and tip through the same
 * transforms the game uses to draw it, and draw quads between recent samples ("trace the tip", alpha
 * 0 at the tail). Third person records in world space from the player model; first person records
 * in hand space from the held item, and is drawn right there with the hand.
 */
public final class BladeTrail {
    private BladeTrail() {}

    private static final ResourceLocation TEX = ProjectZero.id("textures/entity/fx/slash_arc.png");
    private static final long LIFE_NS = 130_000_000L;
    private static final int RGB = 0xC8DCFF;
    /** Hilt end and tip of a sword-shaped item, in item model space (0..1, y up). */
    private static final Vector3f HILT = new Vector3f(0.40F, 0.40F, 0.5F), TIP = new Vector3f(0.95F, 0.95F, 0.5F);

    private record Sample(Vec3 base, Vec3 tip, long t) {}

    private static final Map<AbstractClientPlayer, ArrayDeque<Sample>> WORLD = new WeakHashMap<>();
    private static final ArrayDeque<Sample> HAND = new ArrayDeque<>();

    // ---- recording ----

    /** Third person: called from the model right after posing it, once per rendered frame. */
    static void recordWorld(AnimatedPlayerModel model, AbstractClientPlayer p, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (p == mc.player && mc.screen != null) return;          // inventory preview, not the world
        boolean left = p.getMainArm() == HumanoidArm.LEFT;
        ItemStack stack = p.getMainHandItem();
        PoseStack pose = new PoseStack();
        // LivingEntityRenderer: body yaw, flip, player scale, model offset (plus the crouch offset).
        if (p.isCrouching()) pose.translate(0, -0.125, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot)));
        pose.scale(-1F, -1F, 1F);
        pose.scale(0.9375F, 0.9375F, 0.9375F);
        pose.translate(0, -1.501, 0);
        // ItemInHandLayer: hand, then the held-item turn and offset.
        model.translateToHand(left ? HumanoidArm.LEFT : HumanoidArm.RIGHT, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90F));
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        pose.translate((left ? -1F : 1F) / 16F, 0.125F, -0.625F);
        itemTransform(pose, stack, p, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left);
        Vec3 at = p.getPosition(partial);
        Matrix4f m = pose.last().pose();
        add(WORLD.computeIfAbsent(p, k -> new ArrayDeque<>()), at.add(point(m, HILT)), at.add(point(m, TIP)));
    }

    /** First person: called with the pose already set up to draw the held item. */
    static void recordHand(PoseStack pose, AbstractClientPlayer p, ItemStack stack, boolean leftHand) {
        pose.pushPose();
        itemTransform(pose, stack, p, leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, leftHand);
        Matrix4f m = pose.last().pose();
        add(HAND, point(m, HILT), point(m, TIP));
        pose.popPose();
    }

    private static void itemTransform(PoseStack pose, ItemStack stack, AbstractClientPlayer p, ItemDisplayContext ctx, boolean left) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel baked = mc.getItemRenderer().getModel(stack, p.level(), p, p.getId());
        baked.getTransforms().getTransform(ctx).apply(left, pose);
        pose.translate(-0.5F, -0.5F, -0.5F);
    }

    private static Vec3 point(Matrix4f m, Vector3f local) {
        Vector3f v = m.transformPosition(new Vector3f(local));
        return new Vec3(v.x, v.y, v.z);
    }

    private static void add(ArrayDeque<Sample> trail, Vec3 base, Vec3 tip) {
        long now = System.nanoTime();
        trail.addLast(new Sample(base, tip, now));
        prune(trail, now);
    }

    private static void prune(ArrayDeque<Sample> trail, long now) {
        while (!trail.isEmpty() && now - trail.peekFirst().t() > LIFE_NS) trail.removeFirst();
    }

    // ---- drawing ----

    /** World trails, relative to the camera; pose is the level view matrix. */
    public static void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 cam) {
        long now = System.nanoTime();
        VertexConsumer vc = null;
        for (Iterator<Map.Entry<AbstractClientPlayer, ArrayDeque<Sample>>> it = WORLD.entrySet().iterator(); it.hasNext(); ) {
            ArrayDeque<Sample> trail = it.next().getValue();
            prune(trail, now);
            if (trail.size() < 2) continue;
            if (vc == null) vc = buffers.getBuffer(RenderType.eyes(TEX));
            ribbon(vc, pose.last(), trail, cam, now);
        }
    }

    /** The first-person trail, in hand space (identity pose), drawn with the hand. */
    static void renderHand(MultiBufferSource buffers) {
        long now = System.nanoTime();
        prune(HAND, now);
        if (HAND.size() < 2) return;
        ribbon(buffers.getBuffer(RenderType.eyes(TEX)), new PoseStack().last(), HAND, Vec3.ZERO, now);
    }

    private static void ribbon(VertexConsumer vc, PoseStack.Pose p, ArrayDeque<Sample> trail, Vec3 off, long now) {
        float r = ((RGB >> 16) & 0xFF) / 255F, g = ((RGB >> 8) & 0xFF) / 255F, b = (RGB & 0xFF) / 255F;
        Sample prev = null;
        for (Sample s : trail) {
            if (prev != null) {
                float k0 = fade(prev, now), k1 = fade(s, now);
                Vec3 b0 = prev.base().subtract(off), t0 = prev.tip().subtract(off);
                Vec3 b1 = s.base().subtract(off), t1 = s.tip().subtract(off);
                Vec3[] q = {b0, t0, t1, b1};
                float[] k = {k0, k0, k1, k1};
                float[] v = {0F, 1F, 1F, 0F};
                for (int i = 0; i < 4; i++) vertex(vc, p, q[i], k[i], v[i], r, g, b);
                for (int i = 3; i >= 0; i--) vertex(vc, p, q[i], k[i], v[i], r, g, b);
            }
            prev = s;
        }
    }

    /** Newest samples full bright, the tail fades to nothing. */
    private static float fade(Sample s, long now) {
        float f = 1F - (now - s.t()) / (float) LIFE_NS;
        return Math.max(0F, f * f);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, Vec3 at, float k, float v, float r, float g, float b) {
        vc.addVertex(p, (float) at.x, (float) at.y, (float) at.z).setColor(r * k, g * k, b * k, 1F).setUv(k, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }
}
