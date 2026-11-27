package com.projectzero.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.projectzero.ProjectZero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A ribbon that traces the real blade: each frame we take the sword's hilt and tip through the same
 * transforms the game uses to draw it, and draw quads between recent samples ("trace the tip", alpha
 * 0 at the tail). Third person records in world space from the player model; first person records
 * in hand space from the held item, and is drawn right there with the hand.
 */
public final class BladeTrail {
    private BladeTrail() {}

    private static final ResourceLocation TEX = ProjectZero.id("textures/entity/fx/slash_arc.png");
    private static final long LIFE_NS = 130_000_000L;
    private static final int RGB = 0xC8DCFF;
    /** Hilt end and tip of a sword-shaped item, in item model space (0..1, y up). */
    private static final Vector3f HILT = new Vector3f(0.40F, 0.40F, 0.5F), TIP = new Vector3f(0.95F, 0.95F, 0.5F);
    /** Axes cut with the edge of the head only: a short band beside the top of the handle, not the whole length. */
    private static final Vector3f AXE_HEEL = new Vector3f(0.36F, 0.56F, 0.5F), AXE_TOE = new Vector3f(0.52F, 0.86F, 0.5F);

    private static boolean isAxe(ItemStack stack) { return stack.getItem() instanceof net.minecraft.world.item.AxeItem; }
    private static Vector3f base(ItemStack stack) { return isAxe(stack) ? AXE_HEEL : HILT; }
    private static Vector3f edge(ItemStack stack, Style style) { return isAxe(stack) ? AXE_TOE : reach(style); }

    private record Sample(Vec3 base, Vec3 tip, long t, int rgb, long life) {}

    /**
     * How a weapon's trail looks, from its material and enchantments: color, how far past the tip
     * the arc reaches (Sweeping Edge), how long it lingers (Knockback), and what it sheds from the tip
     * in third person (Fire Aspect embers, Smite and Bane sparks, Sharpness glints).
     */
    public record Style(int rgb, float reach, long life, int shed) {
        static final int SHED_NONE = 0, SHED_EMBER = 1, SHED_HOLY = 2, SHED_VENOM = 3, SHED_GLINT = 4;
    }

    public static Style styleOf(ItemStack stack) {
        int rgb = RGB;
        if (stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered) {
            net.minecraft.world.item.Tier tier = tiered.getTier();
            if (tier == net.minecraft.world.item.Tiers.NETHERITE) rgb = 0x9A6BFF;
            else if (tier == net.minecraft.world.item.Tiers.DIAMOND) rgb = 0x7FF0FF;
            else if (tier == net.minecraft.world.item.Tiers.GOLD) rgb = 0xFFD24A;
            else if (tier == net.minecraft.world.item.Tiers.IRON) rgb = 0xE0E8F0;
            else if (tier == net.minecraft.world.item.Tiers.STONE) rgb = 0xA8A8A8;
            else if (tier == net.minecraft.world.item.Tiers.WOOD) rgb = 0xC8A070;
        }
        float reach = 1F;
        long life = LIFE_NS;
        int shed = Style.SHED_NONE;
        int sharp = 0;
        for (var e : stack.getEnchantments().entrySet()) {
            var h = e.getKey();
            int lvl = e.getIntValue();
            if (h.is(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT)) { rgb = 0xFF7A20; shed = Style.SHED_EMBER; }
            else if (h.is(net.minecraft.world.item.enchantment.Enchantments.SMITE)) { if (shed == 0) { rgb = 0xFFE070; shed = Style.SHED_HOLY; } }
            else if (h.is(net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS)) { if (shed == 0) { rgb = 0x9AD84A; shed = Style.SHED_VENOM; } }
            else if (h.is(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)) sharp = lvl;
            else if (h.is(net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE)) reach += 0.15F * lvl;
            else if (h.is(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK)) life += 35_000_000L * lvl;
        }
        if (sharp > 0) {
            // Sharper = whiter, hotter edge.
            float k = Math.min(0.6F, sharp * 0.12F);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            rgb = ((int) Mth.lerp(k, r, 255) << 16) | ((int) Mth.lerp(k, g, 255) << 8) | (int) Mth.lerp(k, b, 255);
            if (shed == 0) shed = Style.SHED_GLINT;
        }
        return new Style(rgb, reach, life, shed);
    }

    private static final Map<AbstractClientPlayer, ArrayDeque<Sample>> WORLD = new WeakHashMap<>();
    private static final ArrayDeque<Sample> HAND = new ArrayDeque<>();

    // ---- recording ----

    /** Third person: called from the model right after posing it, once per rendered frame. */
    static void recordWorld(AnimatedPlayerModel model, AbstractClientPlayer p, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (p == mc.player && mc.screen != null) return;          // inventory preview, not the world
        boolean left = p.getMainArm() == HumanoidArm.LEFT;
        ItemStack stack = p.getMainHandItem();
        PoseStack pose = new PoseStack();
        // LivingEntityRenderer: body yaw, flip, player scale, model offset (plus the crouch offset).
        if (p.isCrouching()) pose.translate(0, -0.125, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot)));
        pose.scale(-1F, -1F, 1F);
        pose.scale(0.9375F, 0.9375F, 0.9375F);
        pose.translate(0, -1.501, 0);
        // ItemInHandLayer: hand, then the held-item turn and offset.
        model.translateToHand(left ? HumanoidArm.LEFT : HumanoidArm.RIGHT, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90F));
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        pose.translate((left ? -1F : 1F) / 16F, 0.125F, -0.625F);
        itemTransform(pose, stack, p, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left);
        Vec3 at = p.getPosition(partial);
        Matrix4f m = pose.last().pose();
        Style style = styleOf(stack);
        Vec3 hilt = at.add(point(m, base(stack))), tip = at.add(point(m, edge(stack, style)));
        add(WORLD.computeIfAbsent(p, k -> new ArrayDeque<>()), hilt, tip, style);
        shed(p, tip, style);
    }

    /** First person: called with the pose already set up to draw the held item. */
    static void recordHand(PoseStack pose, AbstractClientPlayer p, ItemStack stack, boolean leftHand) {
        pose.pushPose();
        itemTransform(pose, stack, p, leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, leftHand);
        Matrix4f m = pose.last().pose();
        Style style = styleOf(stack);
        Vec3 hilt = point(m, base(stack)), tip = point(m, edge(stack, style));
        add(HAND, hilt, tip, style);
        shedHand(hilt, tip, style, true);
        pose.popPose();
    }

    /** First person, not swinging: enchanted blades still smolder or glint a little. */
    static void idleHand(PoseStack pose, AbstractClientPlayer p, ItemStack stack, boolean leftHand) {
        Style style = styleOf(stack);
        if (style.shed() == Style.SHED_NONE) return;
        pose.pushPose();
        itemTransform(pose, stack, p, leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, leftHand);
        Matrix4f m = pose.last().pose();
        shedHand(point(m, base(stack)), point(m, edge(stack, style)), style, false);
        pose.popPose();
    }

    // ---- first-person motes: tiny particles living in hand space, drawn with the hand ----

    private static final ResourceLocation MOTE_TEX = ProjectZero.id("textures/particle/mana_glow_0.png");
    private static final java.util.List<HandMote> MOTES = new java.util.ArrayList<>();
    private static long motesAt;

    private static final class HandMote {
        double x, y, z, vx, vy, vz, gravity;
        float age, life, size;
        int rgb;
    }

    private static void shedHand(Vec3 hilt, Vec3 tip, Style style, boolean swinging) {
        if (style.shed() == Style.SHED_NONE || MOTES.size() > 80) return;
        java.util.Random r = java.util.concurrent.ThreadLocalRandom.current();
        float chance = swinging ? 0.9F : (style.shed() == Style.SHED_EMBER ? 0.18F : 0.04F);
        int n = swinging ? 2 : 1;
        for (int k = 0; k < n; k++) {
            if (r.nextFloat() > chance) continue;
            double along = 0.35 + r.nextDouble() * 0.65;
            HandMote m = new HandMote();
            m.x = Mth.lerp(along, hilt.x, tip.x);
            m.y = Mth.lerp(along, hilt.y, tip.y);
            m.z = Mth.lerp(along, hilt.z, tip.z);
            switch (style.shed()) {
                case Style.SHED_EMBER -> {
                    m.rgb = r.nextInt(4) == 0 ? 0xFFD070 : 0xFF6A18;
                    m.vx = (r.nextDouble() - 0.5) * 0.08; m.vy = 0.10 + r.nextDouble() * 0.12; m.vz = (r.nextDouble() - 0.5) * 0.08;
                    m.life = 0.45F + r.nextFloat() * 0.45F; m.size = 0.010F + r.nextFloat() * 0.010F;
                }
                case Style.SHED_HOLY -> {
                    m.rgb = 0xFFE070;
                    m.vx = (r.nextDouble() - 0.5) * 0.2; m.vy = (r.nextDouble() - 0.3) * 0.2; m.vz = (r.nextDouble() - 0.5) * 0.2;
                    m.life = 0.25F; m.size = 0.012F;
                }
                case Style.SHED_VENOM -> {
                    m.rgb = 0x9AD84A; m.gravity = -0.7; m.life = 0.5F; m.size = 0.010F;
                }
                default -> { m.rgb = 0xFFFFFF; m.life = 0.15F; m.size = 0.022F; }
            }
            MOTES.add(m);
        }
    }

    private static void drawMotes(MultiBufferSource buffers) {
        long now = System.nanoTime();
        float dt = motesAt == 0 ? 0F : Math.min(0.05F, (now - motesAt) / 1.0E9F);
        motesAt = now;
        if (MOTES.isEmpty()) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(MOTE_TEX));
        PoseStack.Pose p = new PoseStack().last();
        for (Iterator<HandMote> it = MOTES.iterator(); it.hasNext(); ) {
            HandMote m = it.next();
            m.age += dt;
            if (m.age >= m.life) { it.remove(); continue; }
            m.vy += m.gravity * dt;
            m.x += m.vx * dt; m.y += m.vy * dt; m.z += m.vz * dt;
            float k = 1F - m.age / m.life;
            float s = m.size * (0.6F + 0.4F * k);
            float r = ((m.rgb >> 16) & 0xFF) / 255F * k, g = ((m.rgb >> 8) & 0xFF) / 255F * k, b = (m.rgb & 0xFF) / 255F * k;
            // Hand space looks down -Z, so a quad in the XY plane faces the camera.
            float x = (float) m.x, y = (float) m.y, z = (float) m.z;
            mote(vc, p, x - s, y - s, z, 0, 1, r, g, b);
            mote(vc, p, x + s, y - s, z, 1, 1, r, g, b);
            mote(vc, p, x + s, y + s, z, 1, 0, r, g, b);
            mote(vc, p, x - s, y + s, z, 0, 0, r, g, b);
        }
    }

    private static void mote(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, float r, float g, float b) {
        vc.addVertex(p, x, y, z).setColor(r, g, b, 1F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(p, 0, 0, 1);
    }

    private static void itemTransform(PoseStack pose, ItemStack stack, AbstractClientPlayer p, ItemDisplayContext ctx, boolean left) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel baked = mc.getItemRenderer().getModel(stack, p.level(), p, p.getId());
        baked.getTransforms().getTransform(ctx).apply(left, pose);
        pose.translate(-0.5F, -0.5F, -0.5F);
    }

    private static Vec3 point(Matrix4f m, Vector3f local) {
        Vector3f v = m.transformPosition(new Vector3f(local));
        return new Vec3(v.x, v.y, v.z);
    }

    /** The tip pushed out along the blade by Sweeping Edge. */
    private static Vector3f reach(Style style) {
        return new Vector3f(TIP).sub(HILT).mul(style.reach()).add(HILT);
    }

    private static void add(ArrayDeque<Sample> trail, Vec3 base, Vec3 tip, Style style) {
        long now = System.nanoTime();
        trail.addLast(new Sample(base, tip, now, style.rgb(), style.life()));
        prune(trail, now);
    }

    private static void prune(ArrayDeque<Sample> trail, long now) {
        while (!trail.isEmpty() && now - trail.peekFirst().t() > trail.peekFirst().life()) trail.removeFirst();
    }

    /** Enchanted blades shed a little from the tip as they cut (world trails only, a few per frame). */
    private static void shed(AbstractClientPlayer p, Vec3 tip, Style style) {
        if (style.shed() == Style.SHED_NONE || p.level().random.nextInt(3) != 0) return;
        var level = p.level();
        var r = level.random;
        double vx = (r.nextDouble() - 0.5) * 0.04, vy = 0.02 + r.nextDouble() * 0.03, vz = (r.nextDouble() - 0.5) * 0.04;
        switch (style.shed()) {
            case Style.SHED_EMBER -> level.addParticle(com.projectzero.fx.Fx.glow(0xFF7A20, 0.6F), tip.x, tip.y, tip.z, vx, vy, vz);
            case Style.SHED_HOLY -> level.addParticle(com.projectzero.fx.Fx.spark(0xFFE070, 0.7F), tip.x, tip.y, tip.z, vx, vy, vz);
            case Style.SHED_VENOM -> level.addParticle(com.projectzero.fx.Fx.drop(0x9AD84A, 0.8F), tip.x, tip.y, tip.z, vx, 0, vz);
            case Style.SHED_GLINT -> { if (r.nextInt(3) == 0) level.addParticle(com.projectzero.fx.Fx.spark(0xFFFFFF, 0.6F), tip.x, tip.y, tip.z, 0, 0, 0); }
            default -> {}
        }
    }

    // ---- drawing ----

    /** World trails, relative to the camera; pose is the level view matrix. */
    public static void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 cam) {
        long now = System.nanoTime();
        VertexConsumer vc = null;
        for (Iterator<Map.Entry<AbstractClientPlayer, ArrayDeque<Sample>>> it = WORLD.entrySet().iterator(); it.hasNext(); ) {
            ArrayDeque<Sample> trail = it.next().getValue();
            prune(trail, now);
            if (trail.size() < 2) continue;
            if (vc == null) vc = buffers.getBuffer(RenderType.eyes(TEX));
            ribbon(vc, pose.last(), trail, cam, now);
        }
    }

    /** The first-person trail, in hand space (identity pose), drawn with the hand. */
    static void renderHand(MultiBufferSource buffers) {
        drawMotes(buffers);
        long now = System.nanoTime();
        prune(HAND, now);
        if (HAND.size() < 2) return;
        ribbon(buffers.getBuffer(RenderType.eyes(TEX)), new PoseStack().last(), HAND, Vec3.ZERO, now);
    }

    private static void ribbon(VertexConsumer vc, PoseStack.Pose p, ArrayDeque<Sample> trail, Vec3 off, long now) {
        Sample prev = null;
        for (Sample s : trail) {
            float r = ((s.rgb() >> 16) & 0xFF) / 255F, g = ((s.rgb() >> 8) & 0xFF) / 255F, b = (s.rgb() & 0xFF) / 255F;
            if (prev != null) {
                float k0 = fade(prev, now), k1 = fade(s, now);
                Vec3 b0 = prev.base().subtract(off), t0 = prev.tip().subtract(off);
                Vec3 b1 = s.base().subtract(off), t1 = s.tip().subtract(off);
                Vec3[] q = {b0, t0, t1, b1};
                float[] k = {k0, k0, k1, k1};
                float[] v = {0F, 1F, 1F, 0F};
                for (int i = 0; i < 4; i++) vertex(vc, p, q[i], k[i], v[i], r, g, b);
                for (int i = 3; i >= 0; i--) vertex(vc, p, q[i], k[i], v[i], r, g, b);
            }
            prev = s;
        }
    }

    /** Newest samples full bright, the tail fades to nothing. */
    private static float fade(Sample s, long now) {
        float f = 1F - (now - s.t()) / (float) s.life();
        return Math.max(0F, f * f);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, Vec3 at, float k, float v, float r, float g, float b) {
        vc.addVertex(p, (float) at.x, (float) at.y, (float) at.z).setColor(r * k, g * k, b * k, 1F).setUv(k, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }
}
