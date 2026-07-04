package com.gamearoosdevelopment.realistictrafficcontrol.util;

public enum IdleBulbState {
	RED,
	GREEN;

	public static IdleBulbState fromOrdinal(int ordinal) {
		IdleBulbState[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return RED;
		}
		return values[ordinal];
	}
}
