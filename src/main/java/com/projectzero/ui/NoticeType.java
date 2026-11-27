package com.projectzero.ui;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Categories of bottom-right System notices (DESIGN-DOCUMENT.md §11). */
public enum NoticeType {
    LEVEL_UP(0xFF3FD0FF, "notice.projectzero.level_up"),
    SKILL(0xFF55A8FF, "notice.projectzero.skill"),
    QUEST(0xFFFFD34D, "notice.projectzero.quest"),
    GATE(0xFFB06CFF, "notice.projectzero.gate"),
    ITEM(0xFF5CE6A0, "notice.projectzero.item"),
    COOLDOWN(0xFF7FE8FF, "notice.projectzero.cooldown"),
    TITLE(0xFFFFB833, "notice.projectzero.title"),
    WARNING(0xFFFF5252, "notice.projectzero.warning");

    private final int accentArgb;
    private final String titleKey;

    NoticeType(int accentArgb, String titleKey) {
        this.accentArgb = accentArgb;
        this.titleKey = titleKey;
    }

    public int accentArgb() {
        return accentArgb;
    }

    public Component defaultTitle() {
        return Component.translatable(titleKey);
    }

    public static NoticeType byOrdinal(int ordinal) {
        NoticeType[] vals = values();
        return (ordinal >= 0 && ordinal < vals.length) ? vals[ordinal] : GATE;
    }

    public static @Nullable NoticeType byName(String raw) {
        if (raw == null) return null;
        String norm = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("LEVELUP".equals(norm)) return LEVEL_UP;
        for (NoticeType t : values()) {
            if (t.name().equals(norm)) return t;
        }
        return null;
    }
}
