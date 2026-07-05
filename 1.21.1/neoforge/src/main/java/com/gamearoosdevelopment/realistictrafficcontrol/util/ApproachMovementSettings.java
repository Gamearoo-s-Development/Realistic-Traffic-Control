package com.gamearoosdevelopment.realistictrafficcontrol.util;

import net.minecraft.nbt.CompoundTag;

/**
 * Per-approach movement configuration (straight/left/right enabled + idle bulb) for the control box.
 * Ported verbatim from 1.12.2 ({@code NBTTagCompound} -&gt; {@link CompoundTag}, {@code hasKey} -&gt;
 * {@code contains}).
 */
public class ApproachMovementSettings {
    public boolean straightEnabled = true;
    public boolean leftEnabled = true;
    public boolean rightEnabled = true;
    public IdleBulbState straightIdle = IdleBulbState.RED;
    public IdleBulbState leftIdle = IdleBulbState.RED;
    public IdleBulbState rightIdle = IdleBulbState.RED;

    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("straightEnabled", straightEnabled);
        tag.putBoolean("leftEnabled", leftEnabled);
        tag.putBoolean("rightEnabled", rightEnabled);
        tag.putInt("straightIdle", straightIdle.ordinal());
        tag.putInt("leftIdle", leftIdle.ordinal());
        tag.putInt("rightIdle", rightIdle.ordinal());
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
        if (tag.contains("straightIdle")) {
            straightIdle = IdleBulbState.fromOrdinal(tag.getInt("straightIdle"));
        }
        if (tag.contains("leftIdle")) {
            leftIdle = IdleBulbState.fromOrdinal(tag.getInt("leftIdle"));
        }
        if (tag.contains("rightIdle")) {
            rightIdle = IdleBulbState.fromOrdinal(tag.getInt("rightIdle"));
        }
    }

    public ApproachMovementSettings copy() {
        ApproachMovementSettings copy = new ApproachMovementSettings();
        copy.straightEnabled = straightEnabled;
        copy.leftEnabled = leftEnabled;
        copy.rightEnabled = rightEnabled;
        copy.straightIdle = straightIdle;
        copy.leftIdle = leftIdle;
        copy.rightIdle = rightIdle;
        return copy;
    }
}
