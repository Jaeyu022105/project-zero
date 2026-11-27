package com.projectzero.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.projectzero.ProjectZero;
import com.projectzero.jobs.JobRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws worn gauntlets (DESIGN-DOCUMENT.md §12) on both forearms, over the skin's sleeve
 * and under a chestplate's sleeve. Uncommon and better gauntlets get glowing rune seams
 * in the rarity color. Also used for the first-person arm (see {@link #renderFirstPerson}).
 *
 * Texture per item: textures/entity/gauntlets/&lt;item path&gt;.png (+ _glow.png), falling
 * back to iron_gauntlets when an item has no art yet.
 */
public final class GauntletLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation WIDE = new ModelLayerLocation(ProjectZero.id("gauntlets"), "wide");
    public static final ModelLayerLocation SLIM = new ModelLayerLocation(ProjectZero.id("gauntlets"), "slim");

    private static final ResourceLocation FALLBACK = ProjectZero.id("textures/entity/gauntlets/iron_gauntlets.png");
    private static final ResourceLocation FALLBACK_GLOW = ProjectZero.id("textures/entity/gauntlets/iron_gauntlets_glow.png");
    private static final Map<ResourceLocation, ResourceLocation[]> TEXTURES = new HashMap<>();

    private static ModelPart wideModel;
    private static ModelPart slimModel;

    private final ModelPart right;
    private final ModelPart left;

    public GauntletLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                         EntityModelSet models, boolean slim) {
        super(parent);
        ModelPart root = models.bakeLayer(slim ? SLIM : WIDE);
        if (slim) slimModel = root; else wideModel = root;
        this.right = root.getChild("right");
        this.left = root.getChild("left");
    }

    /** Arm boxes match PlayerModel: wide arms are 4 px across, slim 3. */
    public static LayerDefinition create(boolean slim) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int w = slim ? 3 : 4;
        int u = slim ? 32 : 0;
        float rx = slim ? -2F : -3F;
        CubeDeformation shell = new CubeDeformation(0.4F);
        CubeDeformation cuff = new CubeDeformation(0.75F);
        root.addOrReplaceChild("right", CubeListBuilder.create()
                .texOffs(u, 0).addBox(rx, 4F, -2F, w, 6, 4, shell)
                .texOffs(u, 12).addBox(rx, 4F, -2F, w, 1, 4, cuff), PartPose.ZERO);
        root.addOrReplaceChild("left", CubeListBuilder.create()
                .texOffs(u + 16, 0).addBox(-1F, 4F, -2F, w, 6, 4, shell)
                .texOffs(u + 16, 12).addBox(-1F, 4F, -2F, w, 1, 4, cuff), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        ClientWornGear.Worn worn = ClientWornGear.gauntlets(player.getId());
        if (worn == null) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0F);
        PlayerModel<AbstractClientPlayer> model = getParentModel();

        pose.pushPose();
        model.rightArm.translateAndRotate(pose);
        com.projectzero.client.anim.Joints.rotateLower(model.rightArm, pose);   // gauntlets sit on the forearm
        draw(right, pose, buffers, light, overlay, worn);
        pose.popPose();

        pose.pushPose();
        model.leftArm.translateAndRotate(pose);
        com.projectzero.client.anim.Joints.rotateLower(model.leftArm, pose);
        draw(left, pose, buffers, light, overlay, worn);
        pose.popPose();
    }

    /**
     * First-person arm. Called from RenderArmEvent, before vanilla draws the arm with the
     * same pose stack; vanilla uses the arm pivot with no rotation there, so we do too.
     */
    public static void renderFirstPerson(PoseStack pose, MultiBufferSource buffers, int light,
                                         AbstractClientPlayer player, HumanoidArm arm) {
        if (player.isInvisible()) return;
        ClientWornGear.Worn worn = ClientWornGear.gauntlets(player.getId());
        if (worn == null) return;
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        ModelPart root = slim ? slimModel : wideModel;
        if (root == null) return;
        boolean isRight = arm == HumanoidArm.RIGHT;
        pose.pushPose();
        pose.translate((isRight ? -5F : 5F) / 16F, (slim ? 2.5F : 2F) / 16F, 0F);
        draw(root.getChild(isRight ? "right" : "left"), pose, buffers, light, OverlayTexture.NO_OVERLAY, worn);
        pose.popPose();
    }

    private static void draw(ModelPart part, PoseStack pose, MultiBufferSource buffers, int light,
                             int overlay, ClientWornGear.Worn worn) {
        ResourceLocation[] tex = textures(worn.itemId());
        part.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex[0])), light, overlay);
        JobRarity rarity = worn.rarity();
        if (rarity != null && rarity != JobRarity.COMMON) {
            // Additive, full-bright seams. Gentle pulse so legendary gear breathes.
            float t = (System.currentTimeMillis() % 2400L) / 2400F;
            float pulse = 0.75F + 0.25F * (float) Math.sin(t * Math.PI * 2);
            int rgb = rarity.argb();
            int r = (int) (((rgb >> 16) & 0xFF) * pulse);
            int g = (int) (((rgb >> 8) & 0xFF) * pulse);
            int b = (int) ((rgb & 0xFF) * pulse);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            part.render(pose, buffers.getBuffer(RenderType.eyes(tex[1])), 0xF000F0, OverlayTexture.NO_OVERLAY, color);
        }
    }

    private static ResourceLocation[] textures(ResourceLocation itemId) {
        return TEXTURES.computeIfAbsent(itemId, id -> {
            ResourceLocation base = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                    "textures/entity/gauntlets/" + id.getPath() + ".png");
            ResourceLocation glow = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                    "textures/entity/gauntlets/" + id.getPath() + "_glow.png");
            var rm = Minecraft.getInstance().getResourceManager();
            return new ResourceLocation[] {
                    rm.getResource(base).isPresent() ? base : FALLBACK,
                    rm.getResource(glow).isPresent() ? glow : FALLBACK_GLOW };
        });
    }

    /** Drop cached texture lookups (resource reload, logout). */
    public static void clearCache() {
        TEXTURES.clear();
    }
}
