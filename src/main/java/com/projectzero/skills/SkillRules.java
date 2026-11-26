package com.projectzero.skills;

/** Balance numbers for mana and the skills foundation (DESIGN-DOCUMENT.md §4). */
public final class SkillRules {
    private SkillRules() {}

    /** Number of active skill keybind slots (Z, X, C, V). */
    public static final int ACTIVE_SLOTS = 4;

    /** Base flat mana regenerated per second. */
    public static final double MANA_REGEN_BASE_PER_SECOND = 1.0;

    /** Additional mana regenerated per second as a fraction of max mana (2%). */
    public static final double MANA_REGEN_MAX_FRACTION_PER_SECOND = 0.02;

    public static double manaRegenPerSecond(int maxMana) {
        return MANA_REGEN_BASE_PER_SECOND + maxMana * MANA_REGEN_MAX_FRACTION_PER_SECOND;
    }
}
