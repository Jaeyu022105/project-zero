package com.projectzero.ui;

/** Timing and layout constants for bottom-right System notices (DESIGN-DOCUMENT.md §11). */
public final class NoticeRules {
    private NoticeRules() {}

    public static final int SLIDE_IN_TICKS = 6;
    public static final int HOLD_TICKS = 80;       // ~4 seconds
    public static final int FADE_OUT_TICKS = 10;
    public static final int TOTAL_TICKS = SLIDE_IN_TICKS + HOLD_TICKS + FADE_OUT_TICKS;

    public static final int MAX_VISIBLE = 4;

    /** Minimum base skill cooldown (10s) required to emit a COOLDOWN-ready notice. */
    public static final int COOLDOWN_NOTICE_MIN_TICKS = 20 * 10;
}
