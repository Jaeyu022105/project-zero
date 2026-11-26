package com.projectzero.gates;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Gate scale (DESIGN-DOCUMENT.md §9): controls portal frame dimensions, dungeon hall size, and mob count.
 */
public enum GateSize {
    SMALL(2, 2, 9, 22, 4),
    MEDIUM(2, 3, 12, 30, 7),
    LARGE(3, 4, 16, 38, 11);

    private final int portalWidth;
    private final int portalHeight;
    private final int roomHalfWidth;
    private final int roomLength;
    private final int mobCount;

    GateSize(int portalWidth, int portalHeight, int roomHalfWidth, int roomLength, int mobCount) {
        this.portalWidth = portalWidth;
        this.portalHeight = portalHeight;
        this.roomHalfWidth = roomHalfWidth;
        this.roomLength = roomLength;
        this.mobCount = mobCount;
    }

    public int portalWidth() { return portalWidth; }
    public int portalHeight() { return portalHeight; }
    public int roomHalfWidth() { return roomHalfWidth; }
    public int roomLength() { return roomLength; }
    public int mobCount() { return mobCount; }

    public static @Nullable GateSize byName(String raw) {
        if (raw == null) return null;
        for (GateSize s : values()) {
            if (s.name().equalsIgnoreCase(raw)) return s;
        }
        if ("huge".equalsIgnoreCase(raw)) return LARGE;
        return null;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
