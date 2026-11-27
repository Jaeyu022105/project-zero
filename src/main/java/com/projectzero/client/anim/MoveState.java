package com.projectzero.client.anim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-player movement state for the locomotion animations, ticked on the client for every player:
 * airborne blend, landing squash timer (scaled by how hard you fell), sprint and crouch blends,
 * and turn speed for first-person weapon sway. Blends are smoothed so poses never pop.
 */
public final class MoveState {
    public float air, airO;          // 0 grounded .. 1 fully airborne
    public float sprint, sprintO;    // 0 .. 1
    public float crouch, crouchO;    // 0 .. 1
    public float land, landO;        // 1 on the landing tick, decays to 0
    public float landPower = 0.5F;   // 0.3 small hop .. 1 big drop
    public float turn, turnO;        // smoothed yaw speed (deg/tick), signed
    public float fly, flyO;          // 0 .. 1 creative/ability flight (not elytra)
    public float flySpeed, flySpeedO; // 0 .. 1 horizontal flight speed
    public float rise;               // + going up, - falling (for jump vs fall poses)
    /** Blade combo step (index into PlayerPoses.COMBO) and the tick its swing started. */
    public int combo;
    private int lastSwing = -1000;
    private boolean wasGround = true;
    private float lastYaw = Float.NaN;
    private int airTicks;
    private double peakY;

    private static final Map<Player, MoveState> STATES = new WeakHashMap<>();

    public static MoveState of(Player p) {
        return STATES.computeIfAbsent(p, k -> new MoveState());
    }

    public static void tickAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (AbstractClientPlayer p : mc.level.players()) of(p).tick(p);
    }

    private void tick(AbstractClientPlayer p) {
        airO = air; sprintO = sprint; crouchO = crouch; landO = land; turnO = turn; flyO = fly; flySpeedO = flySpeed;
        boolean flying = p.getAbilities().flying && !p.onGround() && !p.isPassenger();
        fly += ((flying ? 1F : 0F) - fly) * 0.2F;
        float hs = (float) p.getDeltaMovement().horizontalDistance();
        flySpeed += (Mth.clamp(hs / 0.5F, 0F, 1F) - flySpeed) * 0.2F;
        boolean ground = p.onGround() || p.isPassenger() || p.isInWater() || p.getAbilities().flying || p.onClimbable();
        air += ((ground ? 0F : 1F) - air) * (ground ? 0.6F : 0.35F);
        sprint += ((p.isSprinting() && !p.isSwimming() ? 1F : 0F) - sprint) * 0.25F;
        crouch += ((p.isCrouching() ? 1F : 0F) - crouch) * 0.35F;
        rise += ((float) Mth.clamp(p.getDeltaMovement().y * 4, -1, 1) - rise) * 0.4F;
        if (!ground) {
            if (wasGround) { airTicks = 0; peakY = p.getY(); }
            airTicks++;
            peakY = Math.max(peakY, p.getY());
        } else if (!wasGround && airTicks > 2) {
            float drop = (float) (peakY - p.getY());
            landPower = Mth.clamp(0.3F + drop * 0.12F, 0.3F, 1F);
            land = 1F;
        }
        land = Math.max(0F, land - 0.2F);
        wasGround = ground;
        float yaw = p.getYRot();
        if (!Float.isNaN(lastYaw)) turn += (Mth.clamp(Mth.wrapDegrees(yaw - lastYaw), -30F, 30F) - turn) * 0.4F;
        lastYaw = yaw;
    }

    /** Gap (ticks) after which a new swing restarts the combo. */
    public static final int COMBO_WINDOW = 16;

    /** Called when a blade swing starts: next combo step, or back to the first after a pause. */
    public int nextSwing(int tick) {
        combo = tick - lastSwing <= COMBO_WINDOW ? (combo + 1) % PlayerPoses.COMBO.length : 0;
        lastSwing = tick;
        return combo;
    }

    public float air(float pt) { return Mth.lerp(pt, airO, air); }
    public float sprint(float pt) { return Mth.lerp(pt, sprintO, sprint); }
    public float crouch(float pt) { return Mth.lerp(pt, crouchO, crouch); }
    public float land(float pt) { return Mth.lerp(pt, landO, land); }
    public float turn(float pt) { return Mth.lerp(pt, turnO, turn); }
    public float fly(float pt) { return Mth.lerp(pt, flyO, fly); }
    public float flySpeed(float pt) { return Mth.lerp(pt, flySpeedO, flySpeed); }
}
