package com.gamearoosdevelopment.realistictrafficcontrol.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/** Persistent game-time schedule shared by display controllers. */
public final class DisplaySchedule {
    public enum Mode {
        MANUAL(0), EVERY_SECOND(20), EVERY_MINUTE(1200), EVERY_HOUR(72000), GAME_TIMES(0);

        private final long ticks;
        Mode(long ticks) { this.ticks = ticks; }
        public long ticks() { return ticks; }
        public boolean isInterval() { return ticks > 0; }
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
        public static Mode fromName(String name) {
            if (name != null) {
                String normalized = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
                if (normalized.equals("SECOND") || normalized.equals("SECONDS")
                        || normalized.equals("EVERY_SEC")) return EVERY_SECOND;
                if (normalized.equals("MINUTE") || normalized.equals("MINUTES")
                        || normalized.equals("EVERY_MIN")) return EVERY_MINUTE;
                if (normalized.equals("HOUR") || normalized.equals("HOURS")) return EVERY_HOUR;
                if (normalized.equals("TIME") || normalized.equals("TIMES")
                        || normalized.equals("GAME_TIME")) return GAME_TIMES;
                if (normalized.equals("OFF")) return MANUAL;
                try { return valueOf(normalized); } catch (IllegalArgumentException ignored) { }
            }
            return MANUAL;
        }
    }

    private Mode mode = Mode.MANUAL;
    private int intervalAmount = 1;
    private final ArrayList<Integer> gameTimes = new ArrayList<>();
    private long lastBucket = Long.MIN_VALUE;
    private int lastDayTime = -1;

    public Mode getMode() { return mode; }
    public void setMode(Mode value) { mode = value == null ? Mode.MANUAL : value; reset(); }
    public int getIntervalAmount() { return intervalAmount; }
    public void setIntervalAmount(int value) { intervalAmount = Math.max(1, Math.min(1_000_000, value)); reset(); }
    public List<Integer> getGameTimes() { return Collections.unmodifiableList(gameTimes); }
    public void setGameTimesFromText(String text) { gameTimes.clear(); gameTimes.addAll(parseGameTimes(text)); reset(); }
    public String getGameTimesText() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < gameTimes.size(); i++) {
            if (i > 0) result.append(", ");
            result.append(formatGameTime(gameTimes.get(i)));
        }
        return result.toString();
    }

    public boolean update(Level level) {
        if (level == null || mode == Mode.MANUAL) return false;
        if (mode == Mode.GAME_TIMES) {
            int now = normalizeDayTime(level.getDayTime());
            if (lastDayTime < 0) { lastDayTime = now; return false; }
            boolean trigger = false;
            for (int configured : gameTimes) {
                if (now > lastDayTime ? configured > lastDayTime && configured <= now
                        : now < lastDayTime && (configured > lastDayTime || configured <= now)) {
                    trigger = true;
                    break;
                }
            }
            lastDayTime = now;
            return trigger;
        }
        long bucket = level.getGameTime() / (mode.ticks() * intervalAmount);
        if (lastBucket == Long.MIN_VALUE) { lastBucket = bucket; return false; }
        if (bucket != lastBucket) { lastBucket = bucket; return true; }
        return false;
    }

    public void load(CompoundTag tag, String prefix) {
        mode = Mode.fromName(tag.getString(prefix + "Mode"));
        intervalAmount = Math.max(1, Math.min(1_000_000,
                tag.contains(prefix + "IntervalAmount") ? tag.getInt(prefix + "IntervalAmount") : 1));
        gameTimes.clear();
        for (int time : tag.getIntArray(prefix + "Times")) gameTimes.add(normalizeDayTime(time));
        Collections.sort(gameTimes);
        reset();
    }

    public void save(CompoundTag tag, String prefix) {
        tag.putString(prefix + "Mode", mode.name());
        tag.putInt(prefix + "IntervalAmount", intervalAmount);
        tag.putIntArray(prefix + "Times", gameTimes);
    }

    private void reset() { lastBucket = Long.MIN_VALUE; lastDayTime = -1; }

    public static int parseGameTime(String text) {
        List<Integer> values = parseGameTimes(text);
        return values.isEmpty() ? -1 : values.get(0);
    }

    private static List<Integer> parseGameTimes(String text) {
        Set<Integer> result = new LinkedHashSet<>();
        if (text == null) return new ArrayList<>();
        for (String token : text.trim().split("[,;\\s]+")) {
            try {
                if (token.contains(":")) {
                    String[] parts = token.split(":", 2);
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60)
                        result.add(normalizeDayTime(((hour + 18) % 24) * 1000L + minute * 1000L / 60L));
                } else if (!token.isEmpty()) {
                    result.add(normalizeDayTime(Long.parseLong(token)));
                }
            } catch (NumberFormatException ignored) { }
        }
        ArrayList<Integer> values = new ArrayList<>(result);
        Collections.sort(values);
        return values;
    }

    public static String formatGameTime(int dayTime) {
        dayTime = normalizeDayTime(dayTime);
        int hour = (dayTime / 1000 + 6) % 24;
        int minute = Math.round((dayTime % 1000) * 60F / 1000F);
        if (minute == 60) { minute = 0; hour = (hour + 1) % 24; }
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    public static int normalizeDayTime(long value) {
        long result = value % 24000L;
        return (int) (result < 0 ? result + 24000L : result);
    }
}
