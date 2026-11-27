package com.projectzero.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import com.projectzero.anim.PlayerAnim;
import com.projectzero.client.ClientSkillVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
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

    /** Swaps each player renderer's model for {@link AnimatedPlayerModel}. Called from AddLayers. */
    public static void install(PlayerRenderer renderer, EntityModelSet models, boolean slim) {
        try {
            for (Field f : LivingEntityRenderer.class.getDeclaredFields()) {
                if (EntityModel.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(renderer, new AnimatedPlayerModel(models.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim));
                    return;
                }
            }
            ProjectZero.LOGGER.warn("Player animations off: no model field on LivingEntityRenderer");
        } catch (ReflectiveOperationException | RuntimeException e) {
            ProjectZero.LOGGER.warn("Player animations off: could not install model", e);
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
        float lean = cur[0] + AnimatedPlayerModel.animLean(p, partial);
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
    public static void onRenderTick(TickEvent.RenderTickEvent.Pre event) {
        frame++;
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

        if (!mainHand) return;
        BladeTrail.renderHand(event.getMultiBufferSource());   // the fading tail, even after the swing ends
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || player.isUsingItem()) return;
        PlayerPoses.Key[] keys;
        float t;
        ClientSkillVisuals.Visual v = ClientSkillVisuals.find(player.getId(), ClientSkillVisuals.Kind.ANIM);
        if (v != null) {
            keys = PlayerPoses.keys(PlayerAnim.byId(v.rgb));
            t = v.progress(partial);
        } else if (event.getSwingProgress() > 0F && AnimatedPlayerModel.isBlade(stack)) {
            keys = PlayerPoses.COMBO[MoveState.of(player).combo];
            t = event.getSwingProgress();
        } else {
            return;
        }
        event.setCanceled(true);
        float w = PlayerPoses.weight(t, keys);
        float[] fp = PlayerPoses.sample(keys, t).fp();
        pose.pushPose();
        // Vanilla's resting item-in-hand spot, then our offset on top.
        pose.translate(i * 0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
        pose.translate(i * fp[0] * w, fp[1] * w, fp[2] * w);
        pose.mulPose(Axis.YP.rotationDegrees(i * fp[4] * w));
        pose.mulPose(Axis.ZP.rotationDegrees(i * fp[5] * w));
        pose.mulPose(Axis.XP.rotationDegrees(fp[3] * w));
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                i > 0 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                i < 0, pose, event.getMultiBufferSource(), event.getPackedLight());
        if (AnimatedPlayerModel.isBlade(stack)) BladeTrail.recordHand(pose, player, stack, i < 0);
        pose.popPose();
    }

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
            if (p.swinging && p.swingTime == 0 && p.swingingArm == InteractionHand.MAIN_HAND
                    && AnimatedPlayerModel.isBlade(p.getMainHandItem())) {
                // Advance the combo; the blade itself draws its trail (BladeTrail).
                MoveState.of(p).nextSwing(p.tickCount);
            }
        }
    }
}
