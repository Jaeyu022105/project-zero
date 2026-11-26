package com.projectzero.leveling;

import net.minecraft.network.chat.Component;

/** The five core stats. Every stat starts at {@link #BASE_VALUE}. */
public enum Stat {
    STRENGTH("strength", "STR"),
    AGILITY("agility", "AGI"),
    VITALITY("vitality", "VIT"),
    SENSE("sense", "SEN"),
    INTELLIGENCE("intelligence", "INT");

    public static final int BASE_VALUE = 10;

    private final String key;
    private final String shortName;

    Stat(String key, String shortName) {
        this.key = key;
        this.shortName = shortName;
    }

    public String key() {
        return key;
    }

    public String shortName() {
        return shortName;
    }

    public Component displayName() {
        return Component.translatable("stat.projectzero." + key);
    }

    public Component description() {
        return Component.translatable("stat.projectzero." + key + ".desc");
    }

    public static Stat byOrdinal(int ordinal) {
        Stat[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
