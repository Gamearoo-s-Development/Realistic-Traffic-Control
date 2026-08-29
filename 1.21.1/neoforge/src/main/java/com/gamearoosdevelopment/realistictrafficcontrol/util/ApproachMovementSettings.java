package com.gamearoosdevelopment.realistictrafficcontrol.util;

import net.minecraft.nbt.CompoundTag;

/**
 * Per-approach movement configuration for the traffic light control box.
 */
public class ApproachMovementSettings {
    public boolean straightEnabled = true;
    public boolean leftEnabled = true;
    public boolean rightEnabled = true;
    public boolean sharedTurns = false;
    public IdleBulbMode straightIdle = IdleBulbMode.SOLID_RED;
    public IdleBulbMode leftIdle = IdleBulbMode.ARROW_RED;
    public IdleBulbMode rightIdle = IdleBulbMode.ARROW_RED;
    public FyaMode leftFya = FyaMode.ALWAYS;
    public FyaMode rightFya = FyaMode.ALWAYS;

    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("straightEnabled", straightEnabled);
        tag.putBoolean("leftEnabled", leftEnabled);
        tag.putBoolean("rightEnabled", rightEnabled);
        tag.putBoolean("sharedTurns", sharedTurns);
        tag.putInt("straightIdle", straightIdle.ordinal());
        tag.putInt("leftIdle", leftIdle.ordinal());
        tag.putInt("rightIdle", rightIdle.ordinal());
        tag.putInt("leftFya", leftFya.ordinal());
        tag.putInt("rightFya", rightFya.ordinal());
        return tag;
    }

    public void readFromNBT(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        if (tag.contains("straightEnabled")) {
            straightEnabled = tag.getBoolean("straightEnabled");
        }
        if (tag.contains("leftEnabled")) {
            leftEnabled = tag.getBoolean("leftEnabled");
        }
        if (tag.contains("rightEnabled")) {
            rightEnabled = tag.getBoolean("rightEnabled");
        }
        if (tag.contains("sharedTurns")) {
            sharedTurns = tag.getBoolean("sharedTurns");
        }
        if (tag.contains("straightIdle")) {
            straightIdle = IdleBulbMode.fromLegacyOrdinal(tag.getInt("straightIdle"), true);
        }
        if (tag.contains("leftIdle")) {
            leftIdle = IdleBulbMode.fromLegacyOrdinal(tag.getInt("leftIdle"), false);
        }
        if (tag.contains("rightIdle")) {
            rightIdle = IdleBulbMode.fromLegacyOrdinal(tag.getInt("rightIdle"), false);
        }
        if (tag.contains("leftFya")) {
            leftFya = FyaMode.fromOrdinal(tag.getInt("leftFya"));
        }
        if (tag.contains("rightFya")) {
            rightFya = FyaMode.fromOrdinal(tag.getInt("rightFya"));
        }
    }

    public ApproachMovementSettings copy() {
        ApproachMovementSettings copy = new ApproachMovementSettings();
        copy.straightEnabled = straightEnabled;
        copy.leftEnabled = leftEnabled;
        copy.rightEnabled = rightEnabled;
        copy.sharedTurns = sharedTurns;
        copy.straightIdle = straightIdle;
        copy.leftIdle = leftIdle;
        copy.rightIdle = rightIdle;
        copy.leftFya = leftFya;
        copy.rightFya = rightFya;
        return copy;
    }
}
