package com.projectzero.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.projectzero.ProjectZero;
import com.projectzero.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client half of {@code HitJuice}: the victim's white flash and shake-in-place (our hitstop, since
 * the server never pauses), the caster's small camera shake and the finisher FOV punch-in.
 * Limits follow the game-feel skill: shake at most 0.5 degrees, at most 6 ticks; both toggle in config.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HitFeelRenderer {
    private HitFeelRenderer() {}

    /** White-flash ticks at the start of a hit reaction. */
    private static final int FLASH_TICKS = 2;
    private static final int WHITE = OverlayTexture.pack(OverlayTexture.u(1F), 10);
    private static boolean rendering;

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (rendering || !ClientConfig.HIT_FLASH.get()) return;
        ClientSkillVisuals.Visual v = ClientSkillVisuals.find(event.getEntity().getId(), ClientSkillVisuals.Kind.HIT_FLASH);
        if (v == null) return;
        event.setCanceled(true);
        rerender(event, v);
    }

    private static <T extends LivingEntity, M extends EntityModel<T>> void rerender(RenderLivingEvent.Pre<T, M> event,
                                                                                  ClientSkillVisuals.Visual v) {
        T e = event.getEntity();
        float partial = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        float t = v.age + partial;
        float strength = 1F - v.progress(partial);
        // Shake in place: a fast, decaying jitter, bigger for heavier tiers (longer visuals).
        float amp = 0.035F * Math.min(3, v.total / 2F) * strength;
        pose.pushPose();
        pose.translate(Mth.sin(t * 9.1F + e.getId()) * amp, 0, Mth.cos(t * 7.3F + e.getId()) * amp);
        MultiBufferSource buffers = v.age < FLASH_TICKS ? white(event.getMultiBufferSource()) : event.getMultiBufferSource();
        rendering = true;
        try {
            float yaw = Mth.lerp(partial, e.yRotO, e.getYRot());
            event.getRenderer().render(e, yaw, partial, pose, buffers, event.getPackedLight());
        } finally {
            rendering = false;
            pose.popPose();
        }
    }

    /** Wraps buffers so every vertex uses the full white overlay (the creeper-flash overlay). */
    private static MultiBufferSource white(MultiBufferSource source) {
        return type -> new WhiteOverlay(source.getBuffer(type));
    }

    private record WhiteOverlay(VertexConsumer d) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { d.setColor(r, g, b, a); return this; }
        @Override public VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(WHITE & 0xFFFF, WHITE >> 16 & 0xFFFF); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { d.setNormal(x, y, z); return this; }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientConfig.SCREEN_SHAKE.get()) return;
        ClientSkillVisuals.Visual v = ClientSkillVisuals.find(mc.player.getId(), ClientSkillVisuals.Kind.SHAKE);
        if (v == null) return;
        float partial = (float) event.getPartialTick();
        float t = v.age + partial;
        float decay = 1F - v.progress(partial);
        float amp = 0.5F * decay * decay;
        event.setPitch(event.getPitch() + Mth.sin(t * 2.9F) * amp);
        event.setYaw(event.getYaw() + Mth.cos(t * 3.7F) * amp);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientConfig.SCREEN_SHAKE.get()) return;
        ClientSkillVisuals.Visual v = ClientSkillVisuals.find(mc.player.getId(), ClientSkillVisuals.Kind.PUNCH);
        if (v == null) return;
        float p = v.progress((float) event.getPartialTick());
        // Pinch in hard on contact, ease back out.
        float k = (1F - p) * (1F - p);
        event.setFOV(event.getFOV() * (1.0 - 0.07 * k));
    }
}
