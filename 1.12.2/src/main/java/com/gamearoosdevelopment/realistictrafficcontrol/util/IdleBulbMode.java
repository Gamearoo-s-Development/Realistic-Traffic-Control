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
		switch (this) {
			case SOLID_YELLOW:
				return SOLID_GREEN;
			case SOLID_GREEN:
				return SOLID_RED;
			case SOLID_RED:
			default:
				return SOLID_YELLOW;
		}
	}

	public IdleBulbMode normalizeForStraight() {
		if (isSolid()) {
			return this;
		}
		switch (this) {
			case ARROW_GREEN:
				return SOLID_GREEN;
			case ARROW_YELLOW:
				return SOLID_YELLOW;
			case ARROW_RED:
			default:
				return SOLID_RED;
		}
	}

	public boolean isSolid() {
		return this == SOLID_RED || this == SOLID_YELLOW || this == SOLID_GREEN;
	}

	public boolean isArrow() {
		return !isSolid();
	}

	public IdleBulbMode toArrowEquivalent() {
		switch (this) {
			case SOLID_GREEN:
			case ARROW_GREEN:
				return ARROW_GREEN;
			case SOLID_YELLOW:
			case ARROW_YELLOW:
				return ARROW_YELLOW;
			case SOLID_RED:
			case ARROW_RED:
			default:
				return ARROW_RED;
		}
	}

	public String getShortLabel() {
		switch (this) {
			case ARROW_RED:
				return "Arr Red";
			case ARROW_GREEN:
				return "Arr Grn";
			case ARROW_YELLOW:
				return "Arr Yel";
			case SOLID_RED:
				return "Ball Red";
			case SOLID_YELLOW:
				return "Ball Yel";
			case SOLID_GREEN:
			default:
				return "Ball Grn";
		}
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
