package com.projectzero.skills;

import java.util.Locale;

public enum SkillType {
    ACTIVE,
    PASSIVE;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SkillType byName(String name) {
        for (SkillType t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}
