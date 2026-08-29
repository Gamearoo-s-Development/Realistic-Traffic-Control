package com.gamearoosdevelopment.realistictrafficcontrol.util;

public enum FyaMode {
	OFF,
	ALWAYS,
	NIGHT_ONLY;

	public FyaMode next() {
		switch (this) {
			case OFF:
				return ALWAYS;
			case ALWAYS:
				return NIGHT_ONLY;
			case NIGHT_ONLY:
			default:
				return OFF;
		}
	}

	public static FyaMode fromOrdinal(int ordinal) {
		FyaMode[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return ALWAYS;
		}
		return values[ordinal];
	}

	public String getShortLabel() {
		switch (this) {
			case OFF:
				return "Off";
			case ALWAYS:
				return "On";
			case NIGHT_ONLY:
			default:
				return "Night";
		}
	}
}
