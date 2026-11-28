package com.projectzero.companions;

public enum CompanionKind {
    SUMMON,
    PET;

    public static CompanionKind byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : SUMMON;
    }
}
