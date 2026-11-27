package com.projectzero.client.anim;

import com.projectzero.anim.PlayerAnim;
import com.projectzero.client.ClientSkillVisuals;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * The player model with our animations layered on top of vanilla: skill poses ({@link PlayerAnim})
 * and a diagonal slice for sword and axe swings. Installed into the player renderers by
 * {@link PlayerAnimEvents#install}. Blocky part rotations only, so it still reads as Minecraft.
 */
public class AnimatedPlayerModel extends PlayerModel<AbstractClientPlayer> {
    public AnimatedPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float partial = ageInTicks - player.tickCount;
        PlayerPoses.Key[] keys = null;
        float t = 0F;
        ClientSkillVisuals.Visual v = ClientSkillVisuals.find(player.getId(), ClientSkillVisuals.Kind.ANIM);
        if (v != null) {
            keys = PlayerPoses.keys(PlayerAnim.byId(v.rgb));
            t = v.progress(partial);
        } else if (attackTime > 0F && isBlade(player.getMainHandItem()) && !player.isUsingItem()) {
            keys = PlayerPoses.SWING;
            t = attackTime;
        }
        if (keys == null) return;
        float w = PlayerPoses.weight(t, keys);
        if (w <= 0F) return;
        PlayerPoses.Key p = PlayerPoses.sample(keys, t);
        // Mirror for left-handed players so the main hand leads.
        boolean left = player.getMainArm() == HumanoidArm.LEFT;
        float m = left ? -1F : 1F;
        ModelPart main = left ? leftArm : rightArm, off = left ? rightArm : leftArm;
        main.xRot = Mth.lerp(w, main.xRot, p.rx());
        main.yRot = Mth.lerp(w, main.yRot, p.ry() * m);
        main.zRot = Mth.lerp(w, main.zRot, p.rz() * m);
        off.xRot = Mth.lerp(w, off.xRot, p.lx());
        off.yRot = Mth.lerp(w, off.yRot, p.ly() * m);
        off.zRot = Mth.lerp(w, off.zRot, p.lz() * m);
        body.yRot = Mth.lerp(w, body.yRot, p.body() * m);
        // Arms follow the twisting torso, like vanilla's attack swing.
        float by = body.yRot;
        rightArm.z = Mth.sin(by) * 5F;
        rightArm.x = -Mth.cos(by) * 5F;
        leftArm.z = -Mth.sin(by) * 5F;
        leftArm.x = Mth.cos(by) * 5F;
        rightArm.yRot += by;
        leftArm.yRot += by;
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        jacket.copyFrom(body);
    }

    public static boolean isBlade(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    /** Whole-body lean in degrees for this frame (skill pose plus sprint lean is added by the events). */
    public static float animLean(AbstractClientPlayer player, float partial) {
        ClientSkillVisuals.Visual v = ClientSkillVisuals.find(player.getId(), ClientSkillVisuals.Kind.ANIM);
        if (v == null) return 0F;
        PlayerPoses.Key[] keys = PlayerPoses.keys(PlayerAnim.byId(v.rgb));
        float t = v.progress(partial);
        return PlayerPoses.sample(keys, t).lean() * PlayerPoses.weight(t, keys);
    }
}
