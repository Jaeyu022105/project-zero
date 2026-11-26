package com.projectzero.client;

/**
 * Client-side state of the eyelid/fade overlay (see {@code VisionOverlayPacket}). No client-only
 * imports, so the packet handler can reference it on either side. Times use the wall clock.
 */
public final class VisionOverlayState {
    private VisionOverlayState() {}

    private static float from;
    private static float to;
    private static long startMs;
    private static long durationMs = 1;
    private static boolean eyelids;

    /** Moves from the current coverage to {@code target} over {@code ticks}. */
    public static void set(float target, int ticks, boolean eyelidStyle) {
        from = coverage();
        to = Math.max(0F, Math.min(1F, target));
        startMs = System.currentTimeMillis();
        durationMs = Math.max(1L, ticks * 50L);
        eyelids = eyelidStyle;
    }

    /** 0 = clear, 1 = fully dark. Eased in and out. */
    public static float coverage() {
        float p = (System.currentTimeMillis() - startMs) / (float) durationMs;
        p = Math.max(0F, Math.min(1F, p));
        p = p * p * (3F - 2F * p);
        return from + (to - from) * p;
    }

    public static boolean eyelids() {
        return eyelids;
    }

    public static void reset() {
        from = 0F;
        to = 0F;
        startMs = 0L;
        durationMs = 1L;
    }
}
