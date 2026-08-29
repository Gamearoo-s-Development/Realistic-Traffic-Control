package com.gamearoosdevelopment.realistictrafficcontrol.util;

public enum FyaMode {
    OFF,
    ALWAYS,
    NIGHT_ONLY;

    public FyaMode next() {
        return switch (this) {
            case OFF -> ALWAYS;
            case ALWAYS -> NIGHT_ONLY;
            default -> OFF;
        };
    }

    public static FyaMode fromOrdinal(int ordinal) {
        FyaMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return ALWAYS;
        }
        return values[ordinal];
    }

    public String getShortLabel() {
        return switch (this) {
            case OFF -> "Off";
            case ALWAYS -> "On";
            default -> "Night";
        };
    }
}
