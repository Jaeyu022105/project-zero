package com.projectzero.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import com.projectzero.domain.JobCardEntity;
import com.projectzero.jobs.Job;
import com.projectzero.jobs.JobRarity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Draws a job card: a flat card, back side facing the player while face down. On reveal it
 * flips over 12 ticks to show the front, tinted with the rarity color, with the job name.
 * Cards that were not chosen shrink and fade out.
 */
public class JobCardRenderer extends EntityRenderer<JobCardEntity> {
    private static final ResourceLocation BACK = ProjectZero.id("textures/entity/job_card_back.png");
    private static final ResourceLocation FRONT = ProjectZero.id("textures/entity/job_card_front.png");
    private static final float HALF_W = 0.4F;
    private static final float H = 1.2F;
    private static final int FLIP_TICKS = 12;
    private static final int FADE_TICKS = 25;

    private final Font font;

    public JobCardRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.font = context.getFont();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(JobCardEntity card, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        drawCard(card, partialTick, pose, buffers, 1F, 1F);

        // Reflection in the Reawakening sea: the same card, upside down under the water line,
        // dimmer and darker. The floor is translucent and drawn after entities, so it tints this.
        if (card.hasMirror()) {
            double y = Mth.lerp(partialTick, card.yo, card.getY());
            pose.pushPose();
            pose.translate(0, 2.0 * (card.mirrorY() - y), 0);
            pose.scale(1F, -1F, 1F);
            drawCard(card, partialTick, pose, buffers, 0.45F, 0.65F);
            pose.popPose();
        }
        super.render(card, entityYaw, partialTick, pose, buffers, packedLight);
    }

    private void drawCard(JobCardEntity card, float partialTick, PoseStack pose, MultiBufferSource buffers,
                          float alphaMul, float brightness) {
        float age = card.tickCount + partialTick;
        float flip = card.revealStartTick < 0 ? 0F : Mth.clamp((age - card.revealStartTick) / FLIP_TICKS, 0F, 1F);
        float fade = card.fadeStartTick < 0 ? 0F : Mth.clamp((age - card.fadeStartTick) / FADE_TICKS, 0F, 1F);
        if (fade >= 1F) return;

        float yaw = Mth.rotLerp(partialTick, card.yRotO, card.getYRot());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        // Gentle sway, plus the flip itself (half a turn around the vertical axis).
        pose.mulPose(Axis.YP.rotationDegrees(Mth.sin(age * 0.05F) * 6F + flip * 180F));
        float scale = 1F - fade * 0.8F;
        pose.translate(0, H / 2, 0);
        pose.scale(scale, scale, scale);
        pose.translate(0, -H / 2, 0);

        int alpha = (int) (255 * (1F - fade) * alphaMul);
        int light = LightTexture.FULL_BRIGHT;
        int grey = (int) (255 * brightness);

        // Back: faces +Z (toward the center/player while face down).
        quad(pose, buffers.getBuffer(RenderType.entityTranslucent(BACK)), 0.01F, grey, grey, grey, alpha, light, false);

        // Front: faces -Z, tinted by rarity once known.
        int color = 0xFFFFFF;
        JobRarity rarity = JobRarity.byOrdinal(card.rarityOrdinal());
        if (rarity != null) color = rarity.argb() & 0xFFFFFF;
        quad(pose, buffers.getBuffer(RenderType.entityTranslucent(FRONT)), -0.01F,
                (int) (((color >> 16) & 0xFF) * brightness), (int) (((color >> 8) & 0xFF) * brightness),
                (int) ((color & 0xFF) * brightness), alpha, light, true);

        // Job name on the front (skipped in reflections: text has no alpha fade).
        ResourceLocation jobId = card.isRevealed() ? ResourceLocation.tryParse(card.jobId()) : null;
        if (jobId != null && flip > 0.5F && alphaMul >= 1F) {
            pose.pushPose();
            pose.translate(0, H * 0.5F, -0.02F);
            pose.mulPose(Axis.YP.rotationDegrees(180F));
            pose.scale(0.012F, -0.012F, 0.012F);
            Matrix4f m = pose.last().pose();
            Component name = Job.displayName(jobId);
            font.drawInBatch(name, -font.width(name) / 2F, -4F, 0xFF000000 | color, true, m, buffers,
                    Font.DisplayMode.NORMAL, 0, light);
            if (rarity != null) {
                Component r = rarity.displayName();
                pose.scale(0.7F, 0.7F, 0.7F);
                font.drawInBatch(r, -font.width(r) / 2F, 12F, 0xFFD0D0D0, false, pose.last().pose(), buffers,
                        Font.DisplayMode.NORMAL, 0, light);
            }
            pose.popPose();
        }

        pose.popPose();
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float z, int r, int g, int b, int a, int light,
                             boolean mirrorU) {
        PoseStack.Pose p = pose.last();
        float u0 = mirrorU ? 1F : 0F;
        float u1 = mirrorU ? 0F : 1F;
        float nz = z >= 0 ? 1F : -1F;
        vertex(vc, p, -HALF_W, 0, z, u1, 1, r, g, b, a, light, nz);
        vertex(vc, p, HALF_W, 0, z, u0, 1, r, g, b, a, light, nz);
        vertex(vc, p, HALF_W, H, z, u0, 0, r, g, b, a, light, nz);
        vertex(vc, p, -HALF_W, H, z, u1, 0, r, g, b, a, light, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v,
                               int r, int g, int b, int a, int light, float nz) {
        vc.addVertex(p, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(p, 0, 0, nz);
    }

    /** Keep drawing the reflection when only it (not the card above) is on screen. */
    @Override
    public boolean shouldRender(JobCardEntity card, Frustum frustum, double camX, double camY, double camZ) {
        if (!card.hasMirror()) return super.shouldRender(card, frustum, camX, camY, camZ);
        if (!card.shouldRender(camX, camY, camZ)) return false;
        AABB box = card.getBoundingBox();
        double low = 2.0 * card.mirrorY() - box.maxY;
        return frustum.isVisible(box.setMinY(Math.min(box.minY, low)));
    }

    @Override
    public ResourceLocation getTextureLocation(JobCardEntity card) {
        return BACK;
    }
}
