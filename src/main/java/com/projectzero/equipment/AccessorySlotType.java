package com.projectzero.equipment;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Accessory equipment slot types (DESIGN-DOCUMENT.md §12).
 * 8 active slots (gauntlets, ring x2, amulet, earrings, belt, charm x2) + 1 spare disabled relic slot.
 */
public enum AccessorySlotType {
    GAUNTLETS("gauntlets", "GAU"),
    RING("ring", "RNG"),
    AMULET("amulet", "AML"),
    EARRINGS("earrings", "EAR"),
    BELT("belt", "BLT"),
    CHARM("charm", "CHM"),
    RELIC("relic", "RLC");

    private final String key;
    private final String shortLabel;

    AccessorySlotType(String key, String shortLabel) {
        this.key = key;
        this.shortLabel = shortLabel;
    }

    public String key() { return key; }
    public String shortLabel() { return shortLabel; }

    public Component displayName() {
        return Component.translatable("slot.projectzero." + key);
    }

    public static @Nullable AccessorySlotType byName(String raw) {
        if (raw == null) return null;
        String lower = raw.toLowerCase(Locale.ROOT);
        for (AccessorySlotType t : values()) {
            if (t.key.equals(lower) || t.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return t;
            }
        }
        if ("rings".equals(lower)) return RING;
        if ("charms".equals(lower)) return CHARM;
        return null;
    }
}
