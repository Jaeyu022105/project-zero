package com.projectzero.skills;

import java.util.Locale;

public enum SkillSource {
    BASIC,
    JOB,
    MILESTONE,
    SKILLBOOK;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SkillSource byName(String name) {
        for (SkillSource s : values()) {
            if (s.name().equalsIgnoreCase(name)) return s;
        }
        return null;
    }
}
