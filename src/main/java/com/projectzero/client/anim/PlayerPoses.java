package com.projectzero.client.anim;

import com.projectzero.anim.PlayerAnim;
import net.minecraft.util.Mth;

/**
 * Keyframed poses for {@link PlayerAnim}. A key holds the third-person pose (arm rotations in
 * radians, body twist, whole-body lean in degrees) and the first-person item offset at a time
 * 0..1 of the animation. Before the first key the pose is the first key; after the last it holds.
 * {@link #weight} blends with the vanilla pose: in over the first 15%, out over the last 30%.
 * Arm yRot: + swings toward the model's right side. zRot on the right arm: + is outward.
 */
public final class PlayerPoses {
    private PlayerPoses() {}

    /** One keyframe. fp = first-person offset {tx, ty, tz, rotX, rotY, rotZ} (degrees). */
    public record Key(float t, float rx, float ry, float rz, float lx, float ly, float lz,
                      float body, float lean, float[] fp) {}

    private static Key k(float t, float rx, float ry, float rz, float lx, float ly, float lz, float body, float lean,
                         float... fp) {
        return new Key(t, rx, ry, rz, lx, ly, lz, body, lean, fp);
    }

    // Wind-up, strike, held end pose (game-feel skill, section 1).
    private static final Key[] SLASH = {
            k(0.25F, -1.7F, 1.3F, 0.3F, -0.4F, 0F, -0.2F, 0.5F, 0F, 0.30F, 0.10F, 0.05F, 10, 50, -30),
            k(0.42F, -1.5F, -1.1F, 0.1F, 0.3F, 0F, -0.3F, -0.6F, 4F, -0.35F, -0.05F, -0.15F, -10, -60, 35),
            k(0.70F, -1.4F, -1.2F, 0.1F, 0.3F, 0F, -0.3F, -0.6F, 3F, -0.35F, -0.05F, -0.15F, -10, -60, 35)};
    private static final Key[] SLAM = {
            k(0.30F, -3.0F, 0F, 0F, -3.0F, 0F, 0F, 0F, -6F, 0F, 0.30F, 0.10F, 45, 0, 0),
            k(0.45F, -0.7F, 0F, 0F, -0.7F, 0F, 0F, 0F, 12F, 0F, -0.25F, -0.20F, -70, 0, 0),
            k(0.70F, -0.6F, 0F, 0F, -0.6F, 0F, 0F, 0F, 10F, 0F, -0.25F, -0.20F, -70, 0, 0)};
    private static final Key[] PUNCH = {
            k(0.28F, 0.7F, 0F, 0.25F, -0.6F, 0F, -0.1F, 0.5F, -2F, 0.05F, -0.05F, 0.15F, 0, 0, 0),
            k(0.42F, -1.65F, -0.1F, 0F, 0.5F, 0F, -0.1F, -0.45F, 6F, -0.10F, 0.05F, -0.45F, -10, 0, 0),
            k(0.70F, -1.6F, -0.1F, 0F, 0.5F, 0F, -0.1F, -0.45F, 5F, -0.10F, 0.05F, -0.45F, -10, 0, 0)};
    private static final Key[] THROW = {
            k(0.30F, -2.8F, 0.2F, 0.3F, -0.8F, 0F, -0.2F, 0.35F, -4F, 0.05F, 0.30F, 0.15F, 40, 0, 0),
            k(0.45F, -1.1F, -0.3F, 0F, 0.2F, 0F, -0.2F, -0.35F, 6F, -0.05F, 0F, -0.40F, -30, 0, 0),
            k(0.70F, -1.0F, -0.3F, 0F, 0.2F, 0F, -0.2F, -0.35F, 5F, -0.05F, 0F, -0.40F, -30, 0, 0)};
    private static final Key[] GUARD = {
            k(0.20F, -1.3F, -0.6F, 0F, -1.5F, 0.6F, 0F, 0F, 2F, -0.25F, 0.10F, 0F, 0, -40, 20),
            k(0.80F, -1.3F, -0.6F, 0F, -1.5F, 0.6F, 0F, 0F, 2F, -0.25F, 0.10F, 0F, 0, -40, 20)};
    private static final Key[] CAST_UP = {
            k(0.30F, -3.0F, 0F, 0.15F, -0.3F, 0F, -0.4F, 0F, -6F, 0F, 0.35F, 0F, 35, 0, 0),
            k(0.75F, -3.0F, 0F, 0.15F, -0.3F, 0F, -0.4F, 0F, -6F, 0F, 0.35F, 0F, 35, 0, 0)};
    private static final Key[] NOVA = {
            k(0.35F, -2.8F, 0F, 0.5F, -2.8F, 0F, -0.5F, 0F, -4F, 0F, 0.30F, 0F, 30, 0, 0),
            k(0.50F, -1.4F, 0F, 1.5F, -1.4F, 0F, -1.5F, 0F, -10F, 0.10F, 0.10F, -0.20F, -10, 0, -20),
            k(0.75F, -1.4F, 0F, 1.5F, -1.4F, 0F, -1.5F, 0F, -10F, 0.10F, 0.10F, -0.20F, -10, 0, -20)};
    private static final Key[] PUSH = {
            k(0.30F, -1.0F, -0.5F, 0F, -1.0F, 0.5F, 0F, 0F, -3F, -0.10F, 0F, 0.10F, 0, 0, 0),
            k(0.45F, -1.6F, 0.15F, 0F, -1.6F, -0.15F, 0F, 0F, 6F, -0.10F, 0.05F, -0.40F, 0, 0, 0),
            k(0.75F, -1.6F, 0.15F, 0F, -1.6F, -0.15F, 0F, 0F, 6F, -0.10F, 0.05F, -0.40F, 0, 0, 0)};
    private static final Key[] ROAR = {
            k(0.35F, 0.3F, 0F, 0.9F, 0.3F, 0F, -0.9F, 0F, -10F, 0.15F, -0.10F, 0.10F, -20, 0, -25),
            k(0.75F, 0.3F, 0F, 0.9F, 0.3F, 0F, -0.9F, 0F, -10F, 0.15F, -0.10F, 0.10F, -20, 0, -25)};
    private static final Key[] DASH = {
            k(0.20F, 0.9F, 0F, 0.25F, 0.9F, 0F, -0.25F, 0F, 16F, 0F, -0.15F, 0.10F, -20, 0, 0),
            k(0.60F, 0.9F, 0F, 0.25F, 0.9F, 0F, -0.25F, 0F, 16F, 0F, -0.15F, 0.10F, -20, 0, 0)};
    /** A plain sword or axe swing: a diagonal slice instead of the vanilla chop. t = attack progress. */
    public static final Key[] SWING = {
            k(0.10F, -2.2F, 0.9F, 0.4F, -0.3F, 0F, -0.1F, 0.35F, 0F, 0.25F, 0.12F, 0F, 10, 45, -30),
            k(0.50F, -1.0F, -0.9F, 0.1F, 0.2F, 0F, -0.2F, -0.40F, 3F, -0.35F, 0.04F, -0.15F, -5, -55, 35),
            k(0.85F, -0.9F, -0.9F, 0.1F, 0.2F, 0F, -0.2F, -0.40F, 2F, -0.35F, 0.04F, -0.15F, -5, -55, 35)};

    /** Combo step 2: backhand, from low left rising to high right. */
    public static final Key[] SWING_BACK = {
            k(0.10F, -1.2F, -1.0F, 0.1F, 0.2F, 0F, -0.2F, -0.40F, 0F, -0.30F, 0.04F, -0.05F, -5, -50, 35),
            k(0.50F, -2.0F, 0.9F, 0.5F, -0.3F, 0F, -0.1F, 0.40F, 2F, 0.30F, 0.15F, -0.10F, 15, 50, -35),
            k(0.85F, -1.9F, 0.9F, 0.5F, -0.3F, 0F, -0.1F, 0.40F, 1F, 0.30F, 0.15F, -0.10F, 15, 50, -35)};
    /** Combo step 3: overhead chop straight down. */
    public static final Key[] SWING_CHOP = {
            k(0.12F, -3.0F, 0.1F, 0.1F, -0.6F, 0F, -0.2F, 0F, -3F, 0F, 0.28F, 0.10F, 50, 0, -10),
            k(0.50F, -0.6F, -0.1F, 0F, 0.3F, 0F, -0.2F, 0F, 7F, -0.05F, -0.05F, -0.25F, -40, 0, 5),
            k(0.85F, -0.5F, -0.1F, 0F, 0.3F, 0F, -0.2F, 0F, 6F, -0.05F, -0.05F, -0.25F, -40, 0, 5)};
    /** Combo step 4: pull back and thrust. */
    public static final Key[] SWING_THRUST = {
            k(0.15F, -1.3F, 0.2F, 0.1F, -0.4F, 0F, -0.2F, 0.45F, -2F, 0.05F, 0.08F, 0.15F, -30, 0, 0),
            k(0.45F, -1.6F, -0.05F, 0F, 0.4F, 0F, -0.2F, -0.35F, 6F, -0.10F, 0.12F, -0.40F, -35, 0, 0),
            k(0.85F, -1.55F, -0.05F, 0F, 0.4F, 0F, -0.2F, -0.35F, 5F, -0.10F, 0.12F, -0.40F, -35, 0, 0)};
    // ---- bare hands: boxing ----
    /** Loose guard, fists by the chin. Held (t = 0.5) while you've been punching. */
    public static final Key[] BOX_GUARD = {
            k(0.50F, -1.15F, -0.35F, 0F, -1.25F, 0.40F, 0F, 0.15F, 2F, 0, 0, 0, 0, 0, 0)};
    public static final Key[] JAB = {
            k(0.12F, -1.20F, -0.35F, 0F, -1.25F, 0.40F, 0F, 0.20F, 1F, 0, 0, 0, 0, 0, 0),
            k(0.38F, -1.62F, 0.05F, 0F, -1.25F, 0.45F, 0F, -0.15F, 4F, 0, 0, 0, 0, 0, 0),
            k(0.75F, -1.62F, 0.05F, 0F, -1.25F, 0.45F, 0F, -0.15F, 4F, 0, 0, 0, 0, 0, 0)};
    public static final Key[] CROSS = {
            k(0.12F, -1.15F, -0.40F, 0F, -1.20F, 0.35F, 0F, -0.25F, 1F, 0, 0, 0, 0, 0, 0),
            k(0.38F, -1.15F, -0.45F, 0F, -1.62F, -0.05F, 0F, 0.50F, 5F, 0, 0, 0, 0, 0, 0),
            k(0.75F, -1.15F, -0.45F, 0F, -1.62F, -0.05F, 0F, 0.50F, 5F, 0, 0, 0, 0, 0, 0)};
    public static final Key[] HOOK = {
            k(0.15F, -1.30F, 0.80F, -0.4F, -1.25F, 0.40F, 0F, 0.45F, 1F, 0, 0, 0, 0, 0, 0),
            k(0.42F, -1.45F, -0.75F, -0.1F, -1.20F, 0.45F, 0F, -0.55F, 3F, 0, 0, 0, 0, 0, 0),
            k(0.75F, -1.45F, -0.75F, -0.1F, -1.20F, 0.45F, 0F, -0.55F, 3F, 0, 0, 0, 0, 0, 0)};
    public static final Key[] UPPERCUT = {
            k(0.18F, -0.50F, 0.10F, 0.1F, -1.25F, 0.40F, 0F, 0.35F, 4F, 0, 0, 0, 0, 0, 0),
            k(0.45F, -2.60F, -0.20F, 0F, -1.10F, 0.40F, 0F, -0.30F, -5F, 0, 0, 0, 0, 0, 0),
            k(0.78F, -2.60F, -0.20F, 0F, -1.10F, 0.40F, 0F, -0.30F, -5F, 0, 0, 0, 0, 0, 0)};
    /** Jab, cross, hook, uppercut: same chain rules as the blade combo. */
    public static final Key[][] FIST_COMBO = {JAB, CROSS, HOOK, UPPERCUT};

    // ---- tools and other items: each family swings its own way ----
    /** Pickaxe: up over the shoulder, a short hard pick down. */
    public static final Key[] PICK = {
            k(0.15F, -2.70F, 0.20F, 0.2F, -0.40F, 0F, -0.1F, 0.15F, -2F, 0.05F, 0.20F, 0.10F, 45, 10, -5),
            k(0.45F, -0.90F, -0.10F, 0F, -0.20F, 0F, -0.1F, -0.10F, 6F, 0F, -0.02F, -0.15F, -45, 0, 5),
            k(0.75F, -0.95F, -0.10F, 0F, -0.20F, 0F, -0.1F, -0.10F, 5F, 0F, -0.02F, -0.15F, -45, 0, 5)};
    /** Axe: a heavy two-step, big wind-up chop then a rising backhand. */
    public static final Key[] AXE_CHOP = {
            k(0.18F, -2.60F, 0.70F, 0.5F, -0.50F, 0F, -0.2F, 0.45F, -3F, 0.25F, 0.20F, 0.05F, 30, 40, -35),
            k(0.48F, -0.70F, -0.70F, 0.1F, 0.10F, 0F, -0.2F, -0.45F, 8F, -0.30F, -0.02F, -0.15F, -40, -45, 30),
            k(0.80F, -0.65F, -0.70F, 0.1F, 0.10F, 0F, -0.2F, -0.45F, 7F, -0.30F, -0.02F, -0.15F, -40, -45, 30)};
    public static final Key[][] AXE = {AXE_CHOP, SWING_BACK};
    /**
     * Shovel: both hands on the shaft (off hand reaching across), lean in and drive it down and
     * forward with bent knees, then lever the scoop up. Low and two-handed, never above the chest.
     */
    public static final Key[] SHOVEL = {
            k(0.18F, -0.35F, -0.20F, 0.05F, -0.85F, 0.55F, 0F, 0.20F, 3F, 0F, -0.10F, 0.05F, -20, 0, 0),
            k(0.42F, -0.85F, -0.25F, 0F, -0.70F, 0.55F, 0F, -0.10F, 12F, 0F, -0.05F, -0.30F, -35, 0, 0),
            k(0.64F, -1.15F, -0.20F, 0F, -0.95F, 0.50F, 0F, 0F, 5F, 0F, 0.12F, -0.20F, 15, 0, 0),
            k(0.86F, -1.10F, -0.20F, 0F, -0.90F, 0.50F, 0F, 0F, 4F, 0F, 0.12F, -0.20F, 15, 0, 0)};
    /** Hoe: reach out and rake back toward you. */
    public static final Key[] HOE = {
            k(0.20F, -1.55F, 0.30F, 0F, -0.30F, 0F, -0.1F, 0.20F, 5F, 0.05F, 0.05F, -0.35F, -10, 15, 0),
            k(0.55F, -0.80F, -0.10F, 0F, -0.30F, 0F, -0.1F, -0.10F, 1F, 0F, -0.08F, 0.05F, -35, -5, 0),
            k(0.80F, -0.80F, -0.10F, 0F, -0.30F, 0F, -0.1F, -0.10F, 1F, 0F, -0.08F, 0.05F, -35, -5, 0)};
    /** Trident: a straight spear thrust. */
    public static final Key[] STAB = {
            k(0.18F, -1.40F, 0.30F, 0.1F, -0.60F, 0.2F, 0F, 0.45F, -3F, 0.05F, 0.05F, 0.20F, -70, 0, 0),
            k(0.42F, -1.62F, -0.05F, 0F, -0.30F, 0.2F, 0F, -0.30F, 8F, -0.08F, 0.08F, -0.50F, -80, 0, 0),
            k(0.78F, -1.60F, -0.05F, 0F, -0.30F, 0.2F, 0F, -0.30F, 7F, -0.08F, 0.08F, -0.50F, -80, 0, 0)};
    /** Blocks and other placeables: a short push forward to set it down. */
    public static final Key[] PLACE = {
            k(0.15F, -0.90F, 0F, 0F, -0.10F, 0F, -0.1F, 0F, 0F, 0F, 0F, 0.05F, 5, 0, 0),
            k(0.40F, -1.35F, -0.10F, 0F, -0.10F, 0F, -0.1F, -0.05F, 3F, -0.05F, 0F, -0.20F, -15, -10, 0),
            k(0.70F, -1.30F, -0.10F, 0F, -0.10F, 0F, -0.1F, -0.05F, 2F, -0.05F, 0F, -0.20F, -15, -10, 0)};
    /** Fishing rod: lift back, flick out. */
    public static final Key[] FLICK = {
            k(0.20F, -2.40F, 0F, 0.1F, -0.30F, 0F, -0.1F, 0.10F, -3F, 0F, 0.22F, 0.10F, 40, 0, 0),
            k(0.42F, -1.30F, 0F, 0F, -0.30F, 0F, -0.1F, -0.10F, 4F, 0F, 0.02F, -0.15F, -25, 0, 0),
            k(0.75F, -1.25F, 0F, 0F, -0.30F, 0F, -0.1F, -0.10F, 3F, 0F, 0.02F, -0.15F, -25, 0, 0)};
    /** Anything else: a light wrist tap. */
    public static final Key[] TAP = {
            k(0.15F, -0.80F, 0.15F, 0F, -0.10F, 0F, -0.1F, 0.05F, 0F, 0.02F, 0.05F, 0.05F, 10, 0, 0),
            k(0.40F, -1.25F, -0.15F, 0F, -0.10F, 0F, -0.1F, -0.05F, 2F, -0.04F, -0.02F, -0.12F, -20, -5, 0),
            k(0.70F, -1.20F, -0.15F, 0F, -0.10F, 0F, -0.1F, -0.05F, 2F, -0.04F, -0.02F, -0.12F, -20, -5, 0)};

    // Individual placement silhouettes for the new Skywood blocks and sapling.
    public static final Key[] PLACE_SKYWOOD_STRIPPED_LOG = placement(-0.32F, 0.18F, -3F, 8F);
    public static final Key[] PLACE_SKYWOOD_WOOD = placement(-0.20F, 0.25F, -2F, 14F);
    public static final Key[] PLACE_SKYWOOD_STRIPPED_WOOD = placement(-0.10F, 0.14F, -4F, 20F);
    public static final Key[] PLACE_SKYWOOD_SLAB = placement(0.15F, 0.10F, -1F, 28F);
    public static final Key[] PLACE_SKYWOOD_STAIRS = placement(0.25F, 0.22F, 2F, 35F);
    public static final Key[] PLACE_SKYWOOD_FENCE = placement(-0.18F, 0.30F, -2F, 44F);
    public static final Key[] PLACE_SKYWOOD_FENCE_GATE = placement(-0.28F, 0.35F, -5F, 52F);
    public static final Key[] PLACE_SKYWOOD_DOOR = placement(0.34F, 0.38F, 4F, 60F);
    public static final Key[] PLACE_SKYWOOD_TRAPDOOR = placement(0.18F, 0.12F, 1F, 68F);
    public static final Key[] PLACE_SKYWOOD_BUTTON = placement(-0.40F, 0.08F, -6F, 76F);
    public static final Key[] PLACE_SKYWOOD_PRESSURE_PLATE = placement(0.42F, 0.09F, 6F, 84F);
    public static final Key[] PLACE_SKYWOOD_SAPLING = placement(0.05F, 0.04F, 0F, 92F);
    public static final Key[] PLACE_BAOBAB_STRIPPED_LOG = placement(0.22F, 0.20F, -4F, 101F);
    public static final Key[] PLACE_BAOBAB_WOOD = placement(0.12F, 0.28F, -2F, 109F);
    public static final Key[] PLACE_BAOBAB_STRIPPED_WOOD = placement(0.32F, 0.16F, -5F, 117F);
    public static final Key[] PLACE_BAOBAB_SLAB = placement(-0.12F, 0.11F, -2F, 125F);
    public static final Key[] PLACE_BAOBAB_STAIRS = placement(-0.22F, 0.25F, 2F, 133F);
    public static final Key[] PLACE_BAOBAB_FENCE = placement(0.18F, 0.33F, -3F, 141F);
    public static final Key[] PLACE_BAOBAB_FENCE_GATE = placement(0.28F, 0.40F, -6F, 149F);
    public static final Key[] PLACE_BAOBAB_DOOR = placement(-0.34F, 0.42F, 5F, 157F);
    public static final Key[] PLACE_BAOBAB_TRAPDOOR = placement(-0.18F, 0.14F, 2F, 165F);
    public static final Key[] PLACE_BAOBAB_BUTTON = placement(0.40F, 0.06F, -7F, 173F);
    public static final Key[] PLACE_BAOBAB_PRESSURE_PLATE = placement(-0.42F, 0.07F, 7F, 181F);
    public static final Key[] PLACE_BAOBAB_SAPLING = placement(-0.05F, 0.05F, 0F, 189F);

    private static Key[] placement(float rx, float rz, float lean, float firstPersonTurn) {
        return new Key[] {
                k(0.16F, rx - 0.25F, 0.12F, rz, -0.35F, 0F, -0.12F, 0.12F, -lean * 0.3F,
                        0.02F, 0.12F, 0.06F, firstPersonTurn * 0.5F, 0, -8),
                k(0.43F, rx + 0.42F, -0.10F, rz * 0.4F, 0.22F, 0F, -0.08F, -0.18F, lean,
                        -0.12F, 0.02F, -0.22F, -firstPersonTurn, 4, 12),
                k(0.76F, rx + 0.38F, -0.08F, rz * 0.35F, 0.20F, 0F, -0.08F, -0.16F, lean * 0.8F,
                        -0.10F, 0.02F, -0.20F, -firstPersonTurn * 0.8F, 3, 9)
        };
    }

    /** The swing for whatever is in the main hand, at this step of the chain. */
    public static Key[] forHeld(net.minecraft.world.item.ItemStack stack, int combo) {
        net.minecraft.world.item.Item item = stack.getItem();
        if (stack.isEmpty()) return FIST_COMBO[combo % FIST_COMBO.length];
        if (item instanceof net.minecraft.world.item.SwordItem) return COMBO[combo % COMBO.length];
        if (item instanceof net.minecraft.world.item.AxeItem) return AXE[combo % AXE.length];
        if (item instanceof net.minecraft.world.item.PickaxeItem) return PICK;
        if (item instanceof net.minecraft.world.item.ShovelItem) return SHOVEL;
        if (item instanceof net.minecraft.world.item.HoeItem) return HOE;
        if (item instanceof net.minecraft.world.item.TridentItem) return STAB;
        if (item instanceof net.minecraft.world.item.MaceItem) return SLAM;
        if (item instanceof net.minecraft.world.item.FishingRodItem) return FLICK;
        if (item instanceof net.minecraft.world.item.SnowballItem || item instanceof net.minecraft.world.item.EggItem
                || item instanceof net.minecraft.world.item.EnderpearlItem || item instanceof net.minecraft.world.item.ThrowablePotionItem
                || item instanceof net.minecraft.world.item.ExperienceBottleItem) return THROW;
        if (item == com.projectzero.registry.ModNature.STRIPPED_SKYWOOD_LOG.get().asItem()) return PLACE_SKYWOOD_STRIPPED_LOG;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_WOOD.get().asItem()) return PLACE_SKYWOOD_WOOD;
        if (item == com.projectzero.registry.ModNature.STRIPPED_SKYWOOD_WOOD.get().asItem()) return PLACE_SKYWOOD_STRIPPED_WOOD;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_SLAB.get().asItem()) return PLACE_SKYWOOD_SLAB;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_STAIRS.get().asItem()) return PLACE_SKYWOOD_STAIRS;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_FENCE.get().asItem()) return PLACE_SKYWOOD_FENCE;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_FENCE_GATE.get().asItem()) return PLACE_SKYWOOD_FENCE_GATE;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_DOOR.get().asItem()) return PLACE_SKYWOOD_DOOR;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_TRAPDOOR.get().asItem()) return PLACE_SKYWOOD_TRAPDOOR;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_BUTTON.get().asItem()) return PLACE_SKYWOOD_BUTTON;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_PRESSURE_PLATE.get().asItem()) return PLACE_SKYWOOD_PRESSURE_PLATE;
        if (item == com.projectzero.registry.ModNature.SKYWOOD_SAPLING.get().asItem()) return PLACE_SKYWOOD_SAPLING;
        if (item == com.projectzero.registry.ModNature.STRIPPED_BAOBAB_LOG.get().asItem()) return PLACE_BAOBAB_STRIPPED_LOG;
        if (item == com.projectzero.registry.ModNature.BAOBAB_WOOD.get().asItem()) return PLACE_BAOBAB_WOOD;
        if (item == com.projectzero.registry.ModNature.STRIPPED_BAOBAB_WOOD.get().asItem()) return PLACE_BAOBAB_STRIPPED_WOOD;
        if (item == com.projectzero.registry.ModNature.BAOBAB_SLAB.get().asItem()) return PLACE_BAOBAB_SLAB;
        if (item == com.projectzero.registry.ModNature.BAOBAB_STAIRS.get().asItem()) return PLACE_BAOBAB_STAIRS;
        if (item == com.projectzero.registry.ModNature.BAOBAB_FENCE.get().asItem()) return PLACE_BAOBAB_FENCE;
        if (item == com.projectzero.registry.ModNature.BAOBAB_FENCE_GATE.get().asItem()) return PLACE_BAOBAB_FENCE_GATE;
        if (item == com.projectzero.registry.ModNature.BAOBAB_DOOR.get().asItem()) return PLACE_BAOBAB_DOOR;
        if (item == com.projectzero.registry.ModNature.BAOBAB_TRAPDOOR.get().asItem()) return PLACE_BAOBAB_TRAPDOOR;
        if (item == com.projectzero.registry.ModNature.BAOBAB_BUTTON.get().asItem()) return PLACE_BAOBAB_BUTTON;
        if (item == com.projectzero.registry.ModNature.BAOBAB_PRESSURE_PLATE.get().asItem()) return PLACE_BAOBAB_PRESSURE_PLATE;
        if (item == com.projectzero.registry.ModNature.BAOBAB_SAPLING.get().asItem()) return PLACE_BAOBAB_SAPLING;
        if (item instanceof net.minecraft.world.item.BlockItem) return PLACE;
        return TAP;
    }

    /** The blade combo, in order. Spamming walks through it; a pause resets to the first slash. */
    public static final Key[][] COMBO = {SWING, SWING_BACK, SWING_CHOP, SWING_THRUST};

    /**
     * Joint bends per key, as {main elbow, off elbow, knees} in radians (elbow + = forearm forward,
     * knee + = shin back). Wind-ups coil the arm; strikes snap it straight; casts stay loose.
     */
    private static final java.util.Map<Key[], float[][]> JOINTS = new java.util.IdentityHashMap<>();

    static {
        joints(SWING, 1.0F, 0.6F, 0.15F, 0.15F, 0.6F, 0.2F, 0.2F, 0.6F, 0.2F);
        joints(SWING_BACK, 1.3F, 0.6F, 0.15F, 0.2F, 0.6F, 0.2F, 0.25F, 0.6F, 0.2F);
        joints(SWING_CHOP, 1.2F, 0.7F, 0.1F, 0.1F, 0.6F, 0.45F, 0.15F, 0.6F, 0.4F);
        joints(SWING_THRUST, 1.6F, 0.7F, 0.25F, 0F, 0.6F, 0.3F, 0.05F, 0.6F, 0.3F);
        joints(SLASH, 1.0F, 0.6F, 0.15F, 0.15F, 0.6F, 0.3F, 0.2F, 0.6F, 0.25F);
        joints(SLAM, 1.2F, 1.2F, 0.1F, 0.1F, 0.1F, 0.6F, 0.2F, 0.2F, 0.5F);
        joints(PUNCH, 1.8F, 1.2F, 0.3F, 0F, 1.0F, 0.3F, 0.05F, 1.0F, 0.25F);
        joints(THROW, 1.4F, 0.5F, 0.15F, 0.1F, 0.5F, 0.2F, 0.15F, 0.5F, 0.2F);
        joints(GUARD, 1.1F, 1.3F, 0.2F, 1.1F, 1.3F, 0.2F);
        joints(CAST_UP, 0.2F, 0.5F, 0.1F, 0.2F, 0.5F, 0.1F);
        joints(NOVA, 1.2F, 1.2F, 0.2F, 0.1F, 0.1F, 0.35F, 0.1F, 0.1F, 0.3F);
        joints(PUSH, 1.4F, 1.4F, 0.2F, 0F, 0F, 0.3F, 0.05F, 0.05F, 0.25F);
        joints(ROAR, 0.6F, 0.6F, 0.3F, 0.6F, 0.6F, 0.3F);
        joints(DASH, 0.9F, 0.9F, 0.5F, 0.9F, 0.9F, 0.5F);
        joints(BOX_GUARD, 1.7F, 1.8F, 0.25F);
        joints(JAB, 1.7F, 1.8F, 0.25F, 0.05F, 1.8F, 0.3F, 0.1F, 1.8F, 0.3F);
        joints(CROSS, 1.8F, 1.7F, 0.25F, 1.8F, 0.05F, 0.35F, 1.8F, 0.1F, 0.35F);
        joints(HOOK, 1.6F, 1.8F, 0.3F, 1.4F, 1.8F, 0.3F, 1.4F, 1.8F, 0.3F);
        joints(UPPERCUT, 1.9F, 1.8F, 0.55F, 1.0F, 1.8F, 0.1F, 1.1F, 1.8F, 0.1F);
        joints(PICK, 1.3F, 0.4F, 0.15F, 0.2F, 0.4F, 0.3F, 0.25F, 0.4F, 0.3F);
        joints(AXE_CHOP, 1.2F, 0.6F, 0.2F, 0.1F, 0.6F, 0.45F, 0.15F, 0.6F, 0.4F);
        joints(SHOVEL, 0.6F, 0.7F, 0.35F, 0.5F, 0.6F, 0.7F, 0.6F, 0.7F, 0.25F, 0.6F, 0.7F, 0.2F);
        joints(HOE, 0.2F, 0.3F, 0.2F, 1.0F, 0.3F, 0.15F, 1.0F, 0.3F, 0.15F);
        joints(STAB, 1.6F, 0.6F, 0.3F, 0.05F, 0.6F, 0.35F, 0.1F, 0.6F, 0.35F);
        joints(PLACE, 0.7F, 0.3F, 0.1F, 0.2F, 0.3F, 0.15F, 0.25F, 0.3F, 0.15F);
        joints(FLICK, 1.2F, 0.3F, 0.1F, 0.3F, 0.3F, 0.1F, 0.35F, 0.3F, 0.1F);
        joints(TAP, 0.6F, 0.3F, 0.1F, 0.3F, 0.3F, 0.1F, 0.3F, 0.3F, 0.1F);
        joints(PLACE_SKYWOOD_STRIPPED_LOG, 0.7F, 0.35F, 0.1F, 0.25F, 0.3F, 0.1F, 0.3F, 0.35F, 0.1F);
        joints(PLACE_SKYWOOD_WOOD, 0.7F, 0.35F, 0.1F, 0.25F, 0.3F, 0.1F, 0.3F, 0.35F, 0.1F);
        joints(PLACE_SKYWOOD_STRIPPED_WOOD, 0.7F, 0.35F, 0.1F, 0.25F, 0.3F, 0.1F, 0.3F, 0.35F, 0.1F);
        joints(PLACE_SKYWOOD_SLAB, 0.65F, 0.3F, 0.1F, 0.2F, 0.35F, 0.1F, 0.2F, 0.3F, 0.1F);
        joints(PLACE_SKYWOOD_STAIRS, 0.8F, 0.35F, 0.1F, 0.25F, 0.35F, 0.1F, 0.3F, 0.35F, 0.1F);
        joints(PLACE_SKYWOOD_FENCE, 0.75F, 0.35F, 0.15F, 0.3F, 0.35F, 0.1F, 0.3F, 0.3F, 0.1F);
        joints(PLACE_SKYWOOD_FENCE_GATE, 0.85F, 0.4F, 0.15F, 0.3F, 0.4F, 0.15F, 0.35F, 0.4F, 0.15F);
        joints(PLACE_SKYWOOD_DOOR, 0.8F, 0.45F, 0.15F, 0.25F, 0.4F, 0.15F, 0.3F, 0.4F, 0.15F);
        joints(PLACE_SKYWOOD_TRAPDOOR, 0.6F, 0.3F, 0.1F, 0.2F, 0.3F, 0.1F, 0.2F, 0.3F, 0.1F);
        joints(PLACE_SKYWOOD_BUTTON, 0.5F, 0.25F, 0.1F, 0.15F, 0.25F, 0.1F, 0.2F, 0.25F, 0.1F);
        joints(PLACE_SKYWOOD_PRESSURE_PLATE, 0.55F, 0.25F, 0.1F, 0.15F, 0.25F, 0.1F, 0.2F, 0.25F, 0.1F);
        joints(PLACE_SKYWOOD_SAPLING, 0.45F, 0.25F, 0.1F, 0.2F, 0.25F, 0.1F, 0.2F, 0.25F, 0.1F);
        joints(PLACE_BAOBAB_STRIPPED_LOG, 0.7F, 0.35F, 0.1F, 0.25F, 0.3F, 0.1F, 0.3F, 0.35F, 0.1F);
        joints(PLACE_BAOBAB_WOOD, 0.7F, 0.35F, 0.1F, 0.25F, 0.3F, 0.1F, 0.3F, 0.35F, 0.1F);
        joints(PLACE_BAOBAB_STRIPPED_WOOD, 0.7F, 0.35F, 0.1F, 0.25F, 0.3F, 0.1F, 0.3F, 0.35F, 0.1F);
        joints(PLACE_BAOBAB_SLAB, 0.65F, 0.3F, 0.1F, 0.2F, 0.35F, 0.1F, 0.2F, 0.3F, 0.1F);
        joints(PLACE_BAOBAB_STAIRS, 0.8F, 0.35F, 0.1F, 0.25F, 0.35F, 0.1F, 0.3F, 0.35F, 0.1F);
        joints(PLACE_BAOBAB_FENCE, 0.75F, 0.35F, 0.15F, 0.3F, 0.35F, 0.1F, 0.3F, 0.3F, 0.1F);
        joints(PLACE_BAOBAB_FENCE_GATE, 0.85F, 0.4F, 0.15F, 0.3F, 0.4F, 0.15F, 0.35F, 0.4F, 0.15F);
        joints(PLACE_BAOBAB_DOOR, 0.8F, 0.45F, 0.15F, 0.25F, 0.4F, 0.15F, 0.3F, 0.4F, 0.15F);
        joints(PLACE_BAOBAB_TRAPDOOR, 0.6F, 0.3F, 0.1F, 0.2F, 0.3F, 0.1F, 0.2F, 0.3F, 0.1F);
        joints(PLACE_BAOBAB_BUTTON, 0.5F, 0.25F, 0.1F, 0.15F, 0.25F, 0.1F, 0.2F, 0.25F, 0.1F);
        joints(PLACE_BAOBAB_PRESSURE_PLATE, 0.55F, 0.25F, 0.1F, 0.15F, 0.25F, 0.1F, 0.2F, 0.25F, 0.1F);
        joints(PLACE_BAOBAB_SAPLING, 0.45F, 0.25F, 0.1F, 0.2F, 0.25F, 0.1F, 0.2F, 0.25F, 0.1F);
    }

    private static void joints(Key[] keys, float... v) {
        float[][] rows = new float[keys.length][3];
        for (int i = 0; i < keys.length; i++) System.arraycopy(v, i * 3, rows[i], 0, 3);
        JOINTS.put(keys, rows);
    }

    /** {main elbow, off elbow, knees} at time t, eased between keys like {@link #sample}. */
    public static float[] sampleJoints(Key[] keys, float t) {
        float[][] rows = JOINTS.get(keys);
        if (rows == null) return new float[3];
        if (t <= keys[0].t()) return rows[0];
        for (int i = 0; i < keys.length - 1; i++) {
            if (t <= keys[i + 1].t()) {
                float f = (t - keys[i].t()) / (keys[i + 1].t() - keys[i].t());
                f = 1F - (1F - f) * (1F - f) * (1F - f);
                float[] out = new float[3];
                for (int j = 0; j < 3; j++) out[j] = Mth.lerp(f, rows[i][j], rows[i + 1][j]);
                return out;
            }
        }
        return rows[rows.length - 1];
    }

    public static Key[] keys(PlayerAnim anim) {
        return switch (anim) {
            case SLASH -> SLASH; case SLAM -> SLAM; case PUNCH -> PUNCH; case THROW -> THROW; case GUARD -> GUARD;
            case CAST_UP -> CAST_UP; case NOVA -> NOVA; case PUSH -> PUSH; case ROAR -> ROAR; case DASH -> DASH;
        };
    }

    /** Blend weight with the vanilla pose at time t. */
    public static float weight(float t, Key[] keys) {
        float in = Math.min(1F, t / Math.max(0.05F, keys[0].t() * 0.6F));
        float out = t > 0.7F ? Math.max(0F, (1F - t) / 0.3F) : 1F;
        return smooth(Math.min(in, out));
    }

    /** The pose at time t, eased between keys (fast strike, soft settle). */
    public static Key sample(Key[] keys, float t) {
        if (t <= keys[0].t()) return keys[0];
        for (int i = 0; i < keys.length - 1; i++) {
            Key a = keys[i], b = keys[i + 1];
            if (t <= b.t()) {
                float f = (t - a.t()) / (b.t() - a.t());
                f = 1F - (1F - f) * (1F - f) * (1F - f);   // ease out: the strike snaps
                float[] fp = new float[6];
                for (int j = 0; j < 6; j++) fp[j] = Mth.lerp(f, a.fp()[j], b.fp()[j]);
                return new Key(t, Mth.lerp(f, a.rx(), b.rx()), Mth.lerp(f, a.ry(), b.ry()), Mth.lerp(f, a.rz(), b.rz()),
                        Mth.lerp(f, a.lx(), b.lx()), Mth.lerp(f, a.ly(), b.ly()), Mth.lerp(f, a.lz(), b.lz()),
                        Mth.lerp(f, a.body(), b.body()), Mth.lerp(f, a.lean(), b.lean()), fp);
            }
        }
        return keys[keys.length - 1];
    }

    private static float smooth(float x) {
        return x * x * (3F - 2F * x);
    }
}
