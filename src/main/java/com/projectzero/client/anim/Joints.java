package com.projectzero.client.anim;

import com.projectzero.ProjectZero;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Elbows and knees. Each arm and leg (and sleeve, trouser and armor piece) is split into an upper
 * box and a lower box. The lower box is a child named "lower" that hinges at the joint, so a bend
 * never detaches anything. The lower box reaches 1 px up into the upper one to fill the gap on the
 * outside of a bend. UVs are the same skin/armor layout cut in half, so any skin or armor works.
 */
public final class Joints {
    private Joints() {}

    public static final ModelLayerLocation PLAYER = new ModelLayerLocation(ProjectZero.id("player_jointed"), "main");
    public static final ModelLayerLocation PLAYER_SLIM = new ModelLayerLocation(ProjectZero.id("player_slim_jointed"), "main");
    public static final ModelLayerLocation ARMOR_INNER = new ModelLayerLocation(ProjectZero.id("player_jointed"), "inner_armor");
    public static final ModelLayerLocation ARMOR_OUTER = new ModelLayerLocation(ProjectZero.id("player_jointed"), "outer_armor");

    public static final CubeDeformation INNER = new CubeDeformation(0.5F), OUTER = new CubeDeformation(1.0F);

    public static LayerDefinition player(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE, sleeve = none.extend(0.25F);
        if (slim) {
            arm(root, "right_arm", 40, 16, -2F, 3, none, false, -5F, 2.5F);
            arm(root, "left_arm", 32, 48, -1F, 3, none, false, 5F, 2.5F);
            arm(root, "right_sleeve", 40, 32, -2F, 3, sleeve, false, -5F, 2.5F);
            arm(root, "left_sleeve", 48, 48, -1F, 3, sleeve, false, 5F, 2.5F);
        } else {
            arm(root, "right_arm", 40, 16, -3F, 4, none, false, -5F, 2F);
            arm(root, "left_arm", 32, 48, -1F, 4, none, false, 5F, 2F);
            arm(root, "right_sleeve", 40, 32, -3F, 4, sleeve, false, -5F, 2F);
            arm(root, "left_sleeve", 48, 48, -1F, 4, sleeve, false, 5F, 2F);
        }
        leg(root, "right_leg", 0, 16, none, false, -1.9F);
        leg(root, "left_leg", 16, 48, none, false, 1.9F);
        leg(root, "right_pants", 0, 32, sleeve, false, -1.9F);
        leg(root, "left_pants", 0, 48, sleeve, false, 1.9F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Armor on the old 64x32 layout; the left side mirrors the right, like vanilla. */
    public static LayerDefinition armor(CubeDeformation def, boolean inner) {
        MeshDefinition mesh = HumanoidModel.createMesh(def, 0F);
        PartDefinition root = mesh.getRoot();
        arm(root, "right_arm", 40, 16, -3F, 4, def, false, -5F, 2F);
        arm(root, "left_arm", 40, 16, -1F, 4, def, true, 5F, 2F);
        CubeDeformation legDef = inner ? def.extend(-0.1F) : def;
        leg(root, "right_leg", 0, 16, legDef, false, -1.9F);
        leg(root, "left_leg", 0, 16, legDef, true, 1.9F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void arm(PartDefinition root, String name, int u, int v, float x0, int w, CubeDeformation def,
                            boolean mirror, float px, float py) {
        PartDefinition upper = root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).mirror(mirror).addBox(x0, -2F, -2F, w, 6, 4, def),
                PartPose.offset(px, py, 0F));
        upper.addOrReplaceChild("lower",
                CubeListBuilder.create().texOffs(u, v + 5).mirror(mirror).addBox(-w / 2F, -1F, -2F, w, 7, 4, def),
                PartPose.offset(x0 + w / 2F, 4F, 0F));
    }

    private static void leg(PartDefinition root, String name, int u, int v, CubeDeformation def, boolean mirror, float px) {
        PartDefinition upper = root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).mirror(mirror).addBox(-2F, 0F, -2F, 4, 6, 4, def),
                PartPose.offset(px, 12F, 0F));
        upper.addOrReplaceChild("lower",
                CubeListBuilder.create().texOffs(u, v + 5).mirror(mirror).addBox(-2F, -1F, -2F, 4, 7, 4, def),
                PartPose.offset(0F, 6F, 0F));
    }

    /** A part's lower half, or an empty stand-in if the model was not built with joints. */
    static ModelPart lower(ModelPart limb) {
        try {
            return limb.getChild("lower");
        } catch (java.util.NoSuchElementException e) {
            return EMPTY;
        }
    }

    private static final ModelPart EMPTY = new ModelPart(java.util.List.of(), java.util.Map.of());

    /** After {@code limb.translateAndRotate(pose)}: also bend about the joint, so things worn on the lower half follow it. */
    public static void rotateLower(ModelPart limb, com.mojang.blaze3d.vertex.PoseStack pose) {
        ModelPart lower = lower(limb);
        if (lower.xRot == 0F) return;
        pose.translate(lower.x / 16F, lower.y / 16F, lower.z / 16F);
        pose.mulPose(com.mojang.math.Axis.XP.rotation(lower.xRot));
        pose.translate(-lower.x / 16F, -lower.y / 16F, -lower.z / 16F);
    }

    /** Joint angles in radians: elbows bend the forearm forward, knees bend the shin back. */
    public record Angles(float rightElbow, float leftElbow, float rightKnee, float leftKnee) {
        public static final Angles ZERO = new Angles(0, 0, 0, 0);
    }

    /** Applies joint angles to a humanoid model built with joints (player or armor). */
    public static void apply(HumanoidModel<?> m, Angles a) {
        lower(m.rightArm).xRot = -a.rightElbow();
        lower(m.leftArm).xRot = -a.leftElbow();
        lower(m.rightLeg).xRot = a.rightKnee();
        lower(m.leftLeg).xRot = a.leftKnee();
    }
}
