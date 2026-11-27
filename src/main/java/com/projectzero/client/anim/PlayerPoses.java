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
            k(0.50F, -1.0F, -0.9F, 0.1F, 0.2F, 0F, -0.2F, -0.40F, 3F, -0.35F, -0.08F, -0.15F, -10, -55, 35),
            k(0.85F, -0.9F, -0.9F, 0.1F, 0.2F, 0F, -0.2F, -0.40F, 2F, -0.35F, -0.08F, -0.15F, -10, -55, 35)};

    /** Combo step 2: backhand, from low left rising to high right. */
    public static final Key[] SWING_BACK = {
            k(0.10F, -1.2F, -1.0F, 0.1F, 0.2F, 0F, -0.2F, -0.40F, 0F, -0.30F, -0.08F, -0.05F, -10, -50, 35),
            k(0.50F, -2.0F, 0.9F, 0.5F, -0.3F, 0F, -0.1F, 0.40F, 2F, 0.30F, 0.15F, -0.10F, 15, 50, -35),
            k(0.85F, -1.9F, 0.9F, 0.5F, -0.3F, 0F, -0.1F, 0.40F, 1F, 0.30F, 0.15F, -0.10F, 15, 50, -35)};
    /** Combo step 3: overhead chop straight down. */
    public static final Key[] SWING_CHOP = {
            k(0.12F, -3.0F, 0.1F, 0.1F, -0.6F, 0F, -0.2F, 0F, -3F, 0F, 0.28F, 0.10F, 50, 0, -10),
            k(0.50F, -0.6F, -0.1F, 0F, 0.3F, 0F, -0.2F, 0F, 7F, -0.05F, -0.25F, -0.20F, -75, 0, 5),
            k(0.85F, -0.5F, -0.1F, 0F, 0.3F, 0F, -0.2F, 0F, 6F, -0.05F, -0.25F, -0.20F, -75, 0, 5)};
    /** Combo step 4: pull back and thrust. */
    public static final Key[] SWING_THRUST = {
            k(0.15F, -1.3F, 0.2F, 0.1F, -0.4F, 0F, -0.2F, 0.45F, -2F, 0.05F, -0.02F, 0.15F, -50, 0, 0),
            k(0.45F, -1.6F, -0.05F, 0F, 0.4F, 0F, -0.2F, -0.35F, 6F, -0.10F, 0.05F, -0.45F, -60, 0, 0),
            k(0.85F, -1.55F, -0.05F, 0F, 0.4F, 0F, -0.2F, -0.35F, 5F, -0.10F, 0.05F, -0.45F, -60, 0, 0)};
    /** The blade combo, in order. Spamming walks through it; a pause resets to the first slash. */
    public static final Key[][] COMBO = {SWING, SWING_BACK, SWING_CHOP, SWING_THRUST};

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
