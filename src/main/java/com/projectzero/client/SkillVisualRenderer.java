package com.projectzero.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws {@link ClientSkillVisuals} in the world after particles, so they show in first person
 * too: the Aegis shield emblem popping up in front of the caster, and the hex aura bubble.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SkillVisualRenderer {
    private SkillVisualRenderer() {}

    private static final ResourceLocation EMBLEM = ProjectZero.id("textures/entity/fx/aegis_emblem.png");
    private static final ResourceLocation BUBBLE = ProjectZero.id("textures/entity/fx/aegis_bubble.png");

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().isPaused()) ClientSkillVisuals.tick();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (ClientSkillVisuals.active().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (ClientSkillVisuals.Visual v : ClientSkillVisuals.active()) {
            Entity e = mc.level.getEntity(v.entityId);
            if (e == null) continue;
            Vec3 at = e.getPosition(partial).subtract(cam);
            float life = v.progress(partial);
            float r = ((v.rgb >> 16) & 0xFF) / 255F, g = ((v.rgb >> 8) & 0xFF) / 255F, b = (v.rgb & 0xFF) / 255F;
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            switch (v.kind) {
                case SHIELD_POP -> shield(pose, buffers, e, partial, life, r, g, b);
                case BUBBLE -> bubble(pose, buffers, e, life, v.age + partial, r, g, b);
            }
            pose.popPose();
        }
        buffers.endBatch(RenderType.eyes(EMBLEM));
        buffers.endBatch(RenderType.eyes(BUBBLE));
    }

    /** Pops up (overshoot), holds, fades; faces where the caster's body faces, ~1.2 blocks ahead. */
    private static void shield(PoseStack pose, MultiBufferSource buffers, Entity e, float partial, float life,
                               float r, float g, float b) {
        float scale = life < 0.2F ? Mth.lerp(life / 0.2F, 0.4F, 1.15F)
                : life < 0.3F ? Mth.lerp((life - 0.2F) / 0.1F, 1.15F, 1F) : 1F;
        float fade = life < 0.55F ? 1F : 1F - (life - 0.55F) / 0.45F;
        float yaw = e.getViewYRot(partial);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.translate(0, e.getBbHeight() * 0.62, 1.2 + life * 0.35);
        float s = 0.85F * scale;
        quad(pose, buffers.getBuffer(RenderType.eyes(EMBLEM)), s, r * fade, g * fade, b * fade);
    }

    /** A hex-lattice sphere that fades in and out and slowly turns. */
    private static void bubble(PoseStack pose, MultiBufferSource buffers, Entity e, float life, float age,
                               float r, float g, float b) {
        float fadeIn = Math.min(1F, age / 6F);
        float fadeOut = life > 0.85F ? (1F - life) / 0.15F : 1F;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.25F);
        float k = 0.45F * fadeIn * fadeOut * pulse;
        float radius = Math.max(e.getBbWidth(), e.getBbHeight()) * 0.62F + 0.15F;
        pose.translate(0, e.getBbHeight() / 2F, 0);
        pose.mulPose(Axis.YP.rotationDegrees(age * 1.5F));
        sphere(pose, buffers.getBuffer(RenderType.eyes(BUBBLE)), radius * (0.96F + 0.04F * fadeIn), r * k, g * k, b * k);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float s, float r, float g, float b) {
        PoseStack.Pose p = pose.last();
        float[][] front = {{-s, -s, 0, 1}, {s, -s, 1, 1}, {s, s, 1, 0}, {-s, s, 0, 0}};
        for (float[] c : front) vertex(vc, p, c[0], c[1], 0, c[2], c[3], r, g, b, 0, 0, 1);
        for (int i = 3; i >= 0; i--) { float[] c = front[i]; vertex(vc, p, c[0], c[1], 0, c[2], c[3], r, g, b, 0, 0, -1); }
    }

    private static void sphere(PoseStack pose, VertexConsumer vc, float rad, float r, float g, float b) {
        PoseStack.Pose p = pose.last();
        int lon = 24, lat = 14;
        for (int i = 0; i < lat; i++) {
            float v0 = i / (float) lat, v1 = (i + 1) / (float) lat;
            float t0 = Mth.PI * v0, t1 = Mth.PI * v1;
            for (int j = 0; j < lon; j++) {
                float u0 = j / (float) lon, u1 = (j + 1) / (float) lon;
                float p0 = Mth.TWO_PI * u0, p1 = Mth.TWO_PI * u1;
                float[][] q = {pt(t0, p0), pt(t0, p1), pt(t1, p1), pt(t1, p0)};
                float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};   // textures clamp, so no tiling
                // Both sides, so it shows from inside (first person) and outside.
                for (int k = 0; k < 4; k++) vertex(vc, p, q[k][0] * rad, q[k][1] * rad, q[k][2] * rad, uv[k][0], uv[k][1], r, g, b, q[k][0], q[k][1], q[k][2]);
                for (int k = 3; k >= 0; k--) vertex(vc, p, q[k][0] * rad, q[k][1] * rad, q[k][2] * rad, uv[k][0], uv[k][1], r, g, b, -q[k][0], -q[k][1], -q[k][2]);
            }
        }
    }

    private static float[] pt(float theta, float phi) {
        return new float[] {Mth.sin(theta) * Mth.cos(phi), Mth.cos(theta), Mth.sin(theta) * Mth.sin(phi)};
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v,
                               float r, float g, float b, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(r, g, b, 1F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(p, nx, ny, nz);
    }
}
