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
    private static final int SWIPE_RGB = 0x6A7288;
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
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getEntity() instanceof AbstractClientPlayer) event.getPoseStack().popPose();
    }

    // ---- first person ----

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ItemStack stack = event.getItemStack();
        if (player == null || stack.isEmpty() || player.isUsingItem()) return;
        float partial = event.getPartialTick();
        PlayerPoses.Key[] keys;
        float t;
        ClientSkillVisuals.Visual v = ClientSkillVisuals.find(player.getId(), ClientSkillVisuals.Kind.ANIM);
        if (v != null) {
            keys = PlayerPoses.keys(PlayerAnim.byId(v.rgb));
            t = v.progress(partial);
        } else if (event.getSwingProgress() > 0F && AnimatedPlayerModel.isBlade(stack)) {
            keys = PlayerPoses.SWING;
            t = event.getSwingProgress();
        } else {
            return;
        }
        event.setCanceled(true);
        float w = PlayerPoses.weight(t, keys);
        float[] fp = PlayerPoses.sample(keys, t).fp();
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        float i = right ? 1F : -1F;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // Vanilla's resting item-in-hand spot, then our offset on top.
        pose.translate(i * 0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
        pose.translate(i * fp[0] * w, fp[1] * w, fp[2] * w);
        pose.mulPose(Axis.YP.rotationDegrees(i * fp[4] * w));
        pose.mulPose(Axis.ZP.rotationDegrees(i * fp[5] * w));
        pose.mulPose(Axis.XP.rotationDegrees(fp[3] * w));
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                !right, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    // ---- swipe trail ----

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        for (AbstractClientPlayer p : mc.level.players()) {
            // swingTime is 0 on the first tick of a swing.
            if (p.swinging && p.swingTime == 0 && p.swingingArm == InteractionHand.MAIN_HAND
                    && AnimatedPlayerModel.isBlade(p.getMainHandItem())) {
                ClientSkillVisuals.add(p.getId(), ClientSkillVisuals.Kind.SLASH.ordinal(), SWIPE_RGB, 7);
            }
        }
    }
}
