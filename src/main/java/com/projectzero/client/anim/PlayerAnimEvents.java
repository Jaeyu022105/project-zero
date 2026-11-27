package com.projectzero.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import com.projectzero.anim.PlayerAnim;
import com.projectzero.client.ClientSkillVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Player animation hooks: whole-body lean (skill poses and sprinting), first-person item motion for
 * skills and blade swings, and the faint swipe trail on sword swings.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PlayerAnimEvents {
    private PlayerAnimEvents() {}

    private static final float SPRINT_LEAN = 6F;
    private static final Map<Player, float[]> LEAN = new WeakHashMap<>();

    /**
     * Swaps each player renderer's model for {@link AnimatedPlayerModel} (with elbows and knees),
     * and its armor models for jointed ones. Called from AddLayers, after vanilla added its layers.
     */
    public static void install(PlayerRenderer renderer, EntityModelSet models, boolean slim) {
        try {
            for (Field f : LivingEntityRenderer.class.getDeclaredFields()) {
                if (EntityModel.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(renderer, new AnimatedPlayerModel(models.bakeLayer(slim ? Joints.PLAYER_SLIM : Joints.PLAYER), slim));
                } else if (java.util.List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    for (Object layer : (java.util.List<?>) f.get(renderer)) {
                        if (layer instanceof net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<?, ?, ?>) jointArmor(layer, models);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ProjectZero.LOGGER.warn("Player animations off: could not install model", e);
        }
    }

    /** The armor layer holds its inner model first, then the outer one. */
    private static void jointArmor(Object layer, EntityModelSet models) throws ReflectiveOperationException {
        int n = 0;
        for (Field f : net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer.class.getDeclaredFields()) {
            if (!net.minecraft.client.model.HumanoidModel.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            f.set(layer, new JointedArmorModel(models.bakeLayer(n++ == 0 ? Joints.ARMOR_INNER : Joints.ARMOR_OUTER)));
            if (n == 2) return;
        }
    }

    // ---- whole-body lean ----

    /** Lowest priority so we only push when the render really happens (Post pops). */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.isCanceled() || !(event.getEntity() instanceof AbstractClientPlayer p)) return;
        float partial = event.getPartialTick();
        float[] cur = LEAN.computeIfAbsent(p, k -> new float[1]);
        float target = p.isSprinting() && p.onGround() && !p.isSwimming() ? SPRINT_LEAN : 0F;
        cur[0] += (target - cur[0]) * 0.12F;
        MoveState ms = MoveState.of(p);
        // Flight leans into the direction of travel, harder the faster you go.
        float lean = cur[0] + AnimatedPlayerModel.animLean(p, partial) + ms.fly(partial) * ms.flySpeed(partial) * 22F;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        if (Math.abs(lean) > 0.05F) {
            float yaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            pose.mulPose(Axis.XP.rotationDegrees(lean));
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
        }
        float land = MoveState.of(p).land(partial) * MoveState.of(p).landPower;
        if (land > 0.01F) pose.scale(1F + land * 0.05F, 1F - land * 0.09F, 1F + land * 0.05F);
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getEntity() instanceof AbstractClientPlayer) event.getPoseStack().popPose();
    }

    // ---- first person ----

    private static long frame;
    private static long snapFrame = -1;
    private static final org.joml.Matrix4f SNAP_POSE = new org.joml.Matrix4f();
    private static final org.joml.Matrix3f SNAP_NORMAL = new org.joml.Matrix3f();

    @SubscribeEvent
    public static void onRenderLevel(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY) frame++;
    }

    /**
     * Every first-person frame: both hands move with your body (stride, sprint pump, jump, landing,
     * crouch, breathing, turn sway). The main hand also plays skill and blade-swing motion.
     * Hands are rendered by vanilla right after this event without their own push, so we snapshot
     * the pose on the first hand and restore it for the second, keeping the two hands independent.
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        PoseStack pose = event.getPoseStack();
        if (snapFrame == frame) {
            pose.last().pose().set(SNAP_POSE);
            pose.last().normal().set(SNAP_NORMAL);
        } else {
            SNAP_POSE.set(pose.last().pose());
            SNAP_NORMAL.set(pose.last().normal());
            snapFrame = frame;
        }
        float partial = event.getPartialTick();
        boolean mainHand = event.getHand() == InteractionHand.MAIN_HAND;
        HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        float i = arm == HumanoidArm.RIGHT ? 1F : -1F;
        if (!player.isUsingItem()) locomotion(pose, player, partial, i, mainHand);

        ItemStack stack = event.getItemStack();
        // Bare hands box: both fists come up into a guard and throw jab, cross, hook, uppercut.
        if (stack.isEmpty() && player.getMainHandItem().isEmpty() && !player.isUsingItem() && !player.isInvisible()
                && (mainHand || MoveState.of(player).boxing(partial) > 0.01F || player.getAttackAnim(partial) > 0F)) {
            event.setCanceled(true);
            float dip = mainHand ? event.getEquipProgress() : 0F;
            renderFist(pose, event.getMultiBufferSource(), event.getPackedLight(), player, arm, mainHand, partial, dip);
            if (mainHand) BladeTrail.renderHand(event.getMultiBufferSource());
            return;
        }
        if (!mainHand) return;
        // Vanilla keeps: using items (bows, shields, food), maps.
        if (stack.isEmpty() || player.isUsingItem() || player.isInvisible()
                || stack.getItem() instanceof net.minecraft.world.item.MapItem) {
            BladeTrail.renderHand(event.getMultiBufferSource());   // let any tail or motes finish
            return;
        }
        PlayerPoses.Key[] keys = null;
        float t = 0F;
        ClientSkillVisuals.Visual v = ClientSkillVisuals.find(player.getId(), ClientSkillVisuals.Kind.ANIM);
        float swing = event.getSwingProgress();
        if (v != null) {
            keys = PlayerPoses.keys(PlayerAnim.byId(v.rgb));
            t = v.progress(partial);
        } else if (swing > 0F) {
            // Each item family swings its own way (see PlayerPoses.forHeld).
            keys = PlayerPoses.forHeld(stack, MoveState.of(player).combo);
            t = swing;
        }
        event.setCanceled(true);
        pose.pushPose();
        if (keys == null) {
            // At rest: vanilla's held-item spot (with the equip dip when you switch items).
            float sq = Mth.sqrt(swing);
            pose.translate(i * -0.4F * Mth.sin(sq * Mth.PI), 0.2F * Mth.sin(sq * Mth.TWO_PI), -0.2F * Mth.sin(swing * Mth.PI));
            pose.translate(i * 0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
            float f = Mth.sin(swing * swing * Mth.PI), f1 = Mth.sin(sq * Mth.PI);
            pose.mulPose(Axis.YP.rotationDegrees(i * (45F + f * -20F)));
            pose.mulPose(Axis.ZP.rotationDegrees(i * f1 * -20F));
            pose.mulPose(Axis.XP.rotationDegrees(f1 * -80F));
            pose.mulPose(Axis.YP.rotationDegrees(i * -45F));
        } else {
            // Our swing. The cooldown dip (equip progress) is ignored so spam-slashing never sinks the blade.
            float w = PlayerPoses.weight(t, keys);
            float[] fp = PlayerPoses.sample(keys, t).fp();
            pose.translate(i * 0.56F, -0.52F, -0.72F);
            pose.translate(i * fp[0] * w, fp[1] * w, fp[2] * w);
            pose.mulPose(Axis.YP.rotationDegrees(i * fp[4] * w));
            pose.mulPose(Axis.ZP.rotationDegrees(i * fp[5] * w));
            pose.mulPose(Axis.XP.rotationDegrees(fp[3] * w));
        }
        renderGripArm(pose, event.getMultiBufferSource(), event.getPackedLight(), player, arm, i);
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                i > 0 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                i < 0, pose, event.getMultiBufferSource(), event.getPackedLight());
        if (keys != null && AnimatedPlayerModel.isBlade(stack)) BladeTrail.recordHand(pose, player, stack, i < 0);
        else if (AnimatedPlayerModel.isBlade(stack)) BladeTrail.idleHand(pose, player, stack, i < 0);
        pose.popPose();
        BladeTrail.renderHand(event.getMultiBufferSource());
    }

    /**
     * Your arm, holding the item: vanilla's empty-hand arm, moved so the fist sits on the item's
     * grip (worked out from both vanilla transforms), and carried by the same pose as the item.
     */
    private static void renderGripArm(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
                                      AbstractClientPlayer player, HumanoidArm arm, float i) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        pose.pushPose();
        pose.translate(i * GRIP_X, GRIP_Y, GRIP_Z);
        pose.mulPose(Axis.YP.rotationDegrees(i * 45F));
        pose.translate(i * -1F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(i * 120F));
        pose.mulPose(Axis.XP.rotationDegrees(200F));
        pose.mulPose(Axis.YP.rotationDegrees(i * -135F));
        pose.translate(i * 5.6F, 0F, 0F);
        com.projectzero.client.GauntletLayer.renderFirstPerson(pose, buffers, light, player, arm);
        if (arm == HumanoidArm.RIGHT) renderer.renderRightHand(pose, buffers, light, player);
        else renderer.renderLeftHand(pose, buffers, light, player);
        pose.popPose();
    }

    /**
     * One bare fist in first person. At rest it sits where vanilla's empty arm does; while you've been
     * punching both fists come up into a guard: the main fist is the lead (it throws the jab and the
     * hook), the other is the rear fist at the chin (cross and uppercut). The idle fist tucks in while
     * its partner punches.
     */
    private static void renderFist(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
                                   LocalPlayer player, HumanoidArm arm, boolean main, float pt, float dip) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        MoveState st = MoveState.of(player);
        float f = arm == HumanoidArm.RIGHT ? 1F : -1F;
        float g = st.boxing(pt);
        float swing = player.getAttackAnim(pt);
        int punch = st.combo % PlayerPoses.FIST_COMBO.length;
        float[] q = fistPose(main, g, swing, punch);
        q[1] -= dip * 0.6F * (1F - g);
        pose.pushPose();
        // Put the fist itself at q, then aim the forearm back toward the elbow: the arm's long axis
        // starts pointing up, tips forward by the pitch and turns in by the yaw; roll twists the fist.
        pose.translate(f * q[0], q[1], q[2]);
        pose.mulPose(Axis.YP.rotationDegrees(f * q[3]));
        pose.mulPose(Axis.XP.rotationDegrees(-q[4]));
        pose.mulPose(Axis.YP.rotationDegrees(f * q[5]));
        // The arm mesh hangs from the shoulder pivot (5 px out, 2 down) and is 12 px long: move its
        // hand end onto the origin.
        pose.translate(f * 6F / 16F, -12F / 16F, 0F);
        com.projectzero.client.GauntletLayer.renderFirstPerson(pose, buffers, light, player, arm);
        if (arm == HumanoidArm.RIGHT) renderer.renderRightHand(pose, buffers, light, player);
        else renderer.renderLeftHand(pose, buffers, light, player);
        pose.popPose();
    }

    // Fist poses for the right hand (the left mirrors x, yaw and roll): {x, y, z, yaw in, pitch forward, roll}.
    // Worked out by projecting the arm mesh through the hand camera (70 degree FOV) frame by frame.
    private static final float[] FIST_REST = {0.56F, -0.42F, -0.80F, -6F, 42F, 0F};
    /** Main fist leads: further out, forearm tipped forward. Off fist is the rear hand, upright by the chin. */
    private static final float[] LEAD_GUARD = {0.20F, -0.20F, -0.80F, 14F, 35F, 0F};
    private static final float[] REAR_GUARD = {0.24F, -0.22F, -0.64F, 14F, 22F, 0F};
    /** Where the idle fist pulls to while its partner throws (the shoulder turns it in). */
    private static final float[] LEAD_TUCK = {0.23F, -0.22F, -0.70F, 18F, 32F, 0F};
    private static final float[] REAR_TUCK = {0.22F, -0.24F, -0.58F, 16F, 18F, 0F};
    /** Load-up before each punch (null: straight from the guard) and where it lands. Jab, cross, hook, uppercut. */
    private static final float[][] FIST_WIND = {null, null,
            {0.46F, -0.14F, -0.78F, -10F, 78F, -20F},        // hook: elbow swings out wide
            {0.20F, -0.42F, -0.66F, 18F, 30F, 0F}};          // uppercut: dip low
    private static final float[][] FIST_HIT = {
            {0.07F, -0.11F, -1.25F, 6F, 84F, -25F},          // jab: dead straight at the crosshair, fist turns over
            {0.05F, -0.11F, -1.25F, 8F, 84F, 25F},           // cross: rear hand across the middle
            {0.03F, -0.10F, -0.78F, 62F, 86F, -70F},         // hook: forearm flat, sweeping across
            {0.06F, -0.12F, -0.86F, 12F, 28F, 20F}};         // uppercut: rises upright under the chin line

    /**
     * Where one fist is this frame. A punch snaps out (wind 0-12%, drive 12-32% easing out), holds
     * on the hit (to 45%), then glides back to the guard. The fist not throwing tucks in.
     */
    private static float[] fistPose(boolean main, float g, float swing, int punch) {
        float[] q = lerp6(FIST_REST, main ? LEAD_GUARD : REAR_GUARD, g);
        if (swing <= 0F) return q;
        boolean throwing = (punch % 2 == 0) == main;
        if (!throwing) {
            float k = swing < 0.45F ? smooth(swing / 0.25F) : 1F - smooth((swing - 0.45F) / 0.55F);
            return lerp6(q, main ? LEAD_TUCK : REAR_TUCK, k * Math.max(g, 0.5F));
        }
        float[] wind = FIST_WIND[punch] == null ? q : FIST_WIND[punch];
        float[] hit = FIST_HIT[punch];
        if (swing < 0.12F) return lerp6(q, wind, smooth(swing / 0.12F));
        if (swing < 0.32F) {
            float k = (swing - 0.12F) / 0.20F;
            k = 1F - (1F - k) * (1F - k) * (1F - k);
            float[] r = lerp6(wind, hit, k);
            if (punch == 2) r[2] -= 0.16F * Mth.sin(k * Mth.PI);   // the hook travels on an arc
            return r;
        }
        if (swing < 0.45F) return hit.clone();
        return lerp6(hit, q, smooth((swing - 0.45F) / 0.55F));
    }

    private static float[] lerp6(float[] a, float[] b, float t) {
        float[] r = new float[6];
        for (int n = 0; n < 6; n++) r[n] = a[n] + (b[n] - a[n]) * t;
        return r;
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    /** Vanilla's empty-arm spot relative to the held-item spot, plus the shift that puts the fist on the grip. */
    private static final float GRIP_X = 0.64F - 0.56F - 0.125F, GRIP_Y = -0.60F + 0.52F - 0.126F, GRIP_Z = 0.325F;

    /** First-person body motion for one hand (i = +1 right, -1 left). Small: Minecraft already bobs the view. */
    private static void locomotion(PoseStack pose, LocalPlayer p, float pt, float i, boolean main) {
        MoveState st = MoveState.of(p);
        float sprint = st.sprint(pt), air = st.air(pt), crouch = st.crouch(pt), turn = st.turn(pt);
        float land = st.land(pt) * st.landPower;
        float walk = Mth.lerp(pt, p.walkDistO, p.walkDist);
        float speed = Mth.clamp(Mth.lerp(pt, p.oBob, p.bob) * 10F, 0F, 1F) * (1F - air);
        float phase = walk * Mth.PI;
        // Hands swing opposite each other with the stride (like arms pumping), harder when sprinting.
        float swing = Mth.sin(phase) * i;
        float pump = speed * (0.35F + sprint * 0.65F);
        float tx = -i * sprint * speed * 0.06F;
        float ty = -Math.abs(Mth.cos(phase)) * pump * 0.05F - sprint * speed * 0.07F;
        float tz = swing * pump * 0.06F;
        float rx = swing * pump * 5F - sprint * speed * 5F;
        float rz = i * sprint * speed * 7F;
        // Idle breathing.
        float age = p.tickCount + pt;
        float idle = (1F - speed) * (1F - air);
        ty += Mth.sin(age * 0.075F + (main ? 0F : 1.3F)) * 0.008F * idle;
        // Jump lifts the hands, falling lets them float up a bit more, landing pushes them down.
        ty += air * (0.05F * Math.max(0F, st.rise) + 0.07F * Math.max(0F, -st.rise)) - land * 0.12F;
        rx += air * -4F + land * 6F;
        // Flight: hands float on a slow bob and trail a little with speed.
        float fly = st.fly(pt), flySpeed = st.flySpeed(pt);
        ty += fly * (Mth.sin(age * 0.09F + (main ? 0F : 0.8F)) * 0.012F - flySpeed * 0.04F);
        tz += fly * flySpeed * 0.05F;
        rx -= fly * flySpeed * 6F;
        // Crouch: hands sink and pull in.
        ty -= crouch * 0.05F;
        tx -= i * crouch * 0.03F;
        // Turning: the hands lag behind the view.
        float lag = Mth.clamp(turn, -20F, 20F);
        tx -= lag * 0.0035F;
        float ry = -lag * 0.35F;
        pose.translate(tx, ty, tz);
        pose.mulPose(Axis.XP.rotationDegrees(rx));
        pose.mulPose(Axis.YP.rotationDegrees(ry));
        pose.mulPose(Axis.ZP.rotationDegrees(rz));
    }

    // ---- swipe trail ----

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        MoveState.tickAll();
        for (AbstractClientPlayer p : mc.level.players()) {
            // swingTime is 0 on the first tick of a swing.
            if (p.swinging && p.swingTime == 0 && p.swingingArm == InteractionHand.MAIN_HAND) {
                // Advance the combo for every main-hand swing; blades draw their own trail (BladeTrail).
                // Breaking blocks bare-handed doesn't raise the boxing guard.
                boolean mining = p == mc.player && mc.gameMode != null && mc.gameMode.isDestroying();
                MoveState.of(p).nextSwing(p.tickCount, p.getMainHandItem().isEmpty() && !mining);
            }
        }
    }
}
