package com.gamearoosdevelopment.realistictrafficcontrol.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/** Persistent game-time schedule shared by digital-sign and message-board controllers. */
public class DisplaySchedule {
	public enum Mode {
		MANUAL(0, "Manual"),
		EVERY_SECOND(20, "Every second"),
		EVERY_MINUTE(20 * 60, "Every minute"),
		EVERY_HOUR(20 * 60 * 60, "Every hour"),
		GAME_TIMES(0, "At game times");

		private final long intervalTicks;
		private final String label;

		Mode(long intervalTicks, String label) {
			this.intervalTicks = intervalTicks;
			this.label = label;
		}

		public long getIntervalTicks() {
			return intervalTicks;
		}

		public String getLabel() {
			return label;
		}

		public boolean isInterval() {
			return intervalTicks > 0;
		}

		public String getLabel(int amount) {
			amount = Math.max(1, amount);
			switch (this) {
				case EVERY_SECOND: return "Every " + amount + " second" + (amount == 1 ? "" : "s");
				case EVERY_MINUTE: return "Every " + amount + " minute" + (amount == 1 ? "" : "s");
				case EVERY_HOUR: return "Every " + amount + " hour" + (amount == 1 ? "" : "s");
				default: return label;
			}
		}

		public Mode next() {
			Mode[] values = values();
			return values[(ordinal() + 1) % values.length];
		}

		public static Mode fromName(String name) {
			if (name != null) {
				String normalized = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
				if (normalized.equals("SECOND") || normalized.equals("SECONDS") || normalized.equals("EVERY_SEC")) return EVERY_SECOND;
				if (normalized.equals("MINUTE") || normalized.equals("MINUTES") || normalized.equals("EVERY_MIN")) return EVERY_MINUTE;
				if (normalized.equals("HOUR") || normalized.equals("HOURS")) return EVERY_HOUR;
				if (normalized.equals("TIME") || normalized.equals("TIMES") || normalized.equals("GAME_TIME")) return GAME_TIMES;
				if (normalized.equals("OFF")) return MANUAL;
				try {
					return valueOf(normalized);
				} catch (IllegalArgumentException ignored) { }
			}
			return MANUAL;
		}
	}

	private Mode mode = Mode.MANUAL;
	private int intervalAmount = 1;
	private final ArrayList<Integer> gameTimes = new ArrayList<>();
	private long lastIntervalBucket = Long.MIN_VALUE;
	private int lastDayTime = -1;

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode == null ? Mode.MANUAL : mode;
		resetClock();
	}

	public int getIntervalAmount() {
		return intervalAmount;
	}

	public void setIntervalAmount(int amount) {
		intervalAmount = Math.max(1, Math.min(1000000, amount));
		resetClock();
	}

	public List<Integer> getGameTimes() {
		return Collections.unmodifiableList(gameTimes);
	}

	public void setGameTimesFromText(String text) {
		gameTimes.clear();
		gameTimes.addAll(parseGameTimes(text));
		resetClock();
	}

	public String getGameTimesText() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < gameTimes.size(); i++) {
			if (i > 0) result.append(", ");
			result.append(formatGameTime(gameTimes.get(i)));
		}
		return result.toString();
	}

	public static int parseGameTime(String text) {
		List<Integer> parsed = parseGameTimes(text);
		return parsed.isEmpty() ? -1 : parsed.get(0);
	}

	/** Returns true once when the selected schedule boundary is crossed. */
	public boolean update(World world) {
		if (world == null || mode == Mode.MANUAL) return false;

		if (mode == Mode.GAME_TIMES) {
			int current = normalizeDayTime(world.getWorldTime());
			if (lastDayTime < 0) {
				lastDayTime = current;
				return false;
			}
			boolean trigger = crossedConfiguredTime(lastDayTime, current);
			lastDayTime = current;
			return trigger;
		}

		long interval = mode.getIntervalTicks() * (long) intervalAmount;
		long bucket = world.getTotalWorldTime() / interval;
		if (lastIntervalBucket == Long.MIN_VALUE) {
			lastIntervalBucket = bucket;
			return false;
		}
		if (bucket != lastIntervalBucket) {
			lastIntervalBucket = bucket;
			return true;
		}
		return false;
	}

	public void readFromNBT(NBTTagCompound compound, String prefix) {
		mode = Mode.fromName(compound.getString(prefix + "Mode"));
		intervalAmount = compound.hasKey(prefix + "IntervalAmount")
				? Math.max(1, Math.min(1000000, compound.getInteger(prefix + "IntervalAmount"))) : 1;
		gameTimes.clear();
		for (int value : compound.getIntArray(prefix + "Times")) {
			gameTimes.add(normalizeDayTime(value));
		}
		Collections.sort(gameTimes);
		resetClock();
	}

	public void writeToNBT(NBTTagCompound compound, String prefix) {
		compound.setString(prefix + "Mode", mode.name());
		compound.setInteger(prefix + "IntervalAmount", intervalAmount);
		int[] times = new int[gameTimes.size()];
		for (int i = 0; i < gameTimes.size(); i++) times[i] = gameTimes.get(i);
		compound.setIntArray(prefix + "Times", times);
	}

	private void resetClock() {
		lastIntervalBucket = Long.MIN_VALUE;
		lastDayTime = -1;
	}

	private boolean crossedConfiguredTime(int previous, int current) {
		if (previous == current || gameTimes.isEmpty()) return false;
		for (int configured : gameTimes) {
			if (current > previous) {
				if (configured > previous && configured <= current) return true;
			} else if (configured > previous || configured <= current) {
				return true;
			}
		}
		return false;
	}

	private static List<Integer> parseGameTimes(String text) {
		Set<Integer> parsed = new LinkedHashSet<>();
		if (text == null) return new ArrayList<>();
		for (String token : text.trim().split("[,;\\s]+")) {
			if (token.isEmpty()) continue;
			try {
				if (token.contains(":")) {
					String[] parts = token.split(":", 2);
					int hour = Integer.parseInt(parts[0]);
					int minute = Integer.parseInt(parts[1]);
					if (hour < 0 || hour > 23 || minute < 0 || minute > 59) continue;
					parsed.add(normalizeDayTime(((hour + 18) % 24) * 1000 + (minute * 1000 / 60)));
				} else {
					parsed.add(normalizeDayTime(Long.parseLong(token)));
				}
			} catch (NumberFormatException ignored) { }
		}
		ArrayList<Integer> result = new ArrayList<>(parsed);
		Collections.sort(result);
		return result;
	}

	public static String formatGameTime(int dayTime) {
		dayTime = normalizeDayTime(dayTime);
		int hour = ((dayTime / 1000) + 6) % 24;
		int minute = Math.round((dayTime % 1000) * 60.0F / 1000.0F);
		if (minute >= 60) {
			minute = 0;
			hour = (hour + 1) % 24;
		}
		return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
	}

	public static int normalizeDayTime(long value) {
		long normalized = value % 24000L;
		if (normalized < 0) normalized += 24000L;
		return (int) normalized;
	}
}
