package com.projectzero.jobs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Job rarities and their draw weights (DESIGN-DOCUMENT.md §5). Weights are relative, but they
 * add up to 100 so they also read as percentages.
 * No client-only imports: the status window and the server both use this.
 */
public enum JobRarity {
    COMMON(50, ChatFormatting.WHITE, 0xFFE0E0E0),
    UNCOMMON(28, ChatFormatting.GREEN, 0xFF55FF7F),
    RARE(15, ChatFormatting.BLUE, 0xFF4F8CFF),
    EPIC(6, ChatFormatting.LIGHT_PURPLE, 0xFFC066FF),
    LEGENDARY(1, ChatFormatting.GOLD, 0xFFFFB020);

    private final int weight;
    private final ChatFormatting chatColor;
    private final int argb;

    JobRarity(int weight, ChatFormatting chatColor, int argb) {
        this.weight = weight;
        this.chatColor = chatColor;
        this.argb = argb;
    }

    public int weight() { return weight; }
    public ChatFormatting chatColor() { return chatColor; }
    /** Color for drawing on screens (ARGB). */
    public int argb() { return argb; }

    public String key() { return name().toLowerCase(Locale.ROOT); }

    public Component displayName() {
        return Component.translatable("rarity.projectzero." + key());
    }

    /** Parses "common", "RARE", ... Returns null when unknown. */
    public static JobRarity byName(String name) {
        for (JobRarity r : values()) {
            if (r.name().equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    public static JobRarity byOrdinal(int ordinal) {
        JobRarity[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
