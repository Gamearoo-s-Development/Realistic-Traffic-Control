package com.gamearoosdevelopment.realistictrafficcontrol.util;

public enum IdleBulbMode {
    ARROW_RED,
    ARROW_GREEN,
    ARROW_YELLOW,
    SOLID_RED,
    SOLID_YELLOW,
    SOLID_GREEN;

    public IdleBulbMode next() {
        IdleBulbMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public IdleBulbMode nextForStraight() {
        return switch (this) {
            case SOLID_YELLOW -> SOLID_GREEN;
            case SOLID_GREEN -> SOLID_RED;
            default -> SOLID_YELLOW;
        };
    }

    public IdleBulbMode normalizeForStraight() {
        if (isSolid()) {
            return this;
        }
        return switch (this) {
            case ARROW_GREEN -> SOLID_GREEN;
            case ARROW_YELLOW -> SOLID_YELLOW;
            default -> SOLID_RED;
        };
    }

    public boolean isSolid() {
        return this == SOLID_RED || this == SOLID_YELLOW || this == SOLID_GREEN;
    }

    public boolean isArrow() {
        return !isSolid();
    }

    public IdleBulbMode toArrowEquivalent() {
        return switch (this) {
            case SOLID_GREEN, ARROW_GREEN -> ARROW_GREEN;
            case SOLID_YELLOW, ARROW_YELLOW -> ARROW_YELLOW;
            default -> ARROW_RED;
        };
    }

    public String getShortLabel() {
        return switch (this) {
            case ARROW_RED -> "Arr Red";
            case ARROW_GREEN -> "Arr Grn";
            case ARROW_YELLOW -> "Arr Yel";
            case SOLID_RED -> "Ball Red";
            case SOLID_YELLOW -> "Ball Yel";
            default -> "Ball Grn";
        };
    }

    public static IdleBulbMode fromOrdinal(int ordinal) {
        IdleBulbMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return ARROW_RED;
        }
        return values[ordinal];
    }

    /** Maps legacy 2-state idle (red/green) to the expanded modes. */
    public static IdleBulbMode fromLegacyOrdinal(int ordinal, boolean straightMovement) {
        if (ordinal <= 1) {
            if (straightMovement) {
                return ordinal == 1 ? SOLID_GREEN : SOLID_RED;
            }
            return ordinal == 1 ? ARROW_GREEN : ARROW_RED;
        }
        return fromOrdinal(ordinal);
    }
}
