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
        if (!player.isSwimming() && !player.isFallFlying() && !player.isSleeping()) {
            locomotion(player, limbSwing, limbSwingAmount, ageInTicks, partial);
        }
        PlayerPoses.Key[] keys = null;
        float t = 0F;
        ClientSkillVisuals.Visual v = ClientSkillVisuals.find(player.getId(), ClientSkillVisuals.Kind.ANIM);
        if (v != null) {
            keys = PlayerPoses.keys(PlayerAnim.byId(v.rgb));
            t = v.progress(partial);
        } else if (attackTime > 0F && isBlade(player.getMainHandItem()) && !player.isUsingItem()) {
            keys = PlayerPoses.COMBO[MoveState.of(player).combo];
            t = attackTime;
        }
        if (keys == null) {
            copySkins();
            return;
        }
        boolean trail = isBlade(player.getMainHandItem());
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
        copySkins();
        if (trail) BladeTrail.recordWorld(this, player, partial);
    }

    /**
     * Movement on top of vanilla's walk: a stride with body bob and torso counter-twist, a stronger
     * arm pump and forward arms when sprinting, idle breathing, jump/fall poses and a landing crouch.
     * Pixel offsets only (y + = down), so the blocky look stays.
     */
    private void locomotion(AbstractClientPlayer player, float limbSwing, float amount, float age, float pt) {
        MoveState st = MoveState.of(player);
        float sprint = st.sprint(pt), air = st.air(pt), land = st.land(pt) * st.landPower, crouch = st.crouch(pt);
        float phase = limbSwing * 0.6662F;
        float move = Mth.clamp(amount, 0F, 1F) * (1F - air);
        float ground = 1F - air;

        // Stride: the body dips twice per cycle (each footfall) and twists against the legs.
        float bob = Math.abs(Mth.cos(phase)) * move * (0.6F + sprint * 0.9F);
        float twist = Mth.sin(phase) * move * (0.08F + sprint * 0.12F);
        // Sprint: arms pump harder and bend forward; the head counters the twist so the view stays level.
        rightArm.xRot += Mth.cos(phase + Mth.PI) * move * sprint * 0.55F - sprint * move * 0.25F;
        leftArm.xRot += Mth.cos(phase) * move * sprint * 0.55F - sprint * move * 0.25F;
        rightArm.zRot += sprint * move * 0.12F;
        leftArm.zRot -= sprint * move * 0.12F;
        // Idle breathing: a slow chest rise and arm drift, fading out as you move.
        float idle = (1F - Math.min(1F, amount * 3F)) * ground;
        float breathe = Mth.sin(age * 0.075F) * 0.35F * idle;
        // Airborne: arms lift out for balance; tuck one leg rising, reach both down when falling.
        float rise = Math.max(0F, st.rise), fall = Math.max(0F, -st.rise);
        rightArm.zRot += air * 0.35F;
        leftArm.zRot -= air * 0.35F;
        rightArm.xRot += air * (-0.25F * rise + 0.1F);
        leftArm.xRot += air * (-0.25F * rise + 0.1F);
        rightLeg.xRot = Mth.lerp(air, rightLeg.xRot, -0.55F * rise - 0.15F * fall);
        leftLeg.xRot = Mth.lerp(air, leftLeg.xRot, 0.25F * rise - 0.15F * fall);
        // Landing: knees give and arms drop, scaled by how hard you landed.
        float dip = land * 2.2F;
        rightArm.zRot += land * 0.25F;
        leftArm.zRot -= land * 0.25F;
        rightLeg.xRot -= land * 0.35F * (1F - crouch);
        leftLeg.xRot -= land * 0.35F * (1F - crouch);

        float dy = bob + breathe * 0.5F + dip;
        head.y += dy;
        body.y += dy;
        rightArm.y += dy + breathe * 0.4F;
        leftArm.y += dy + breathe * 0.4F;
        body.yRot += twist;
        head.yRot -= twist * 0.6F;
        // Arms ride the twisting torso.
        float by = body.yRot;
        rightArm.z = Mth.sin(by) * 5F;
        rightArm.x = -Mth.cos(by) * 5F;
        leftArm.z = -Mth.sin(by) * 5F;
        leftArm.x = Mth.cos(by) * 5F;
        rightArm.yRot += twist;
        leftArm.yRot += twist;
        hat.copyFrom(head);
    }

    private void copySkins() {
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        jacket.copyFrom(body);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
        hat.copyFrom(head);
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
