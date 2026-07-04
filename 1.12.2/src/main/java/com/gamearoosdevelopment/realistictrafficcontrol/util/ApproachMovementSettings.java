package com.gamearoosdevelopment.realistictrafficcontrol.util;

import net.minecraft.nbt.NBTTagCompound;

public class ApproachMovementSettings {
	public boolean straightEnabled = true;
	public boolean leftEnabled = true;
	public boolean rightEnabled = true;
	public IdleBulbState straightIdle = IdleBulbState.RED;
	public IdleBulbState leftIdle = IdleBulbState.RED;
	public IdleBulbState rightIdle = IdleBulbState.RED;

	public NBTTagCompound writeToNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setBoolean("straightEnabled", straightEnabled);
		tag.setBoolean("leftEnabled", leftEnabled);
		tag.setBoolean("rightEnabled", rightEnabled);
		tag.setInteger("straightIdle", straightIdle.ordinal());
		tag.setInteger("leftIdle", leftIdle.ordinal());
		tag.setInteger("rightIdle", rightIdle.ordinal());
		return tag;
	}

	public void readFromNBT(NBTTagCompound tag) {
		if (tag == null) {
			return;
		}
		if (tag.hasKey("straightEnabled")) {
			straightEnabled = tag.getBoolean("straightEnabled");
		}
		if (tag.hasKey("leftEnabled")) {
			leftEnabled = tag.getBoolean("leftEnabled");
		}
		if (tag.hasKey("rightEnabled")) {
			rightEnabled = tag.getBoolean("rightEnabled");
		}
		if (tag.hasKey("straightIdle")) {
			straightIdle = IdleBulbState.fromOrdinal(tag.getInteger("straightIdle"));
		}
		if (tag.hasKey("leftIdle")) {
			leftIdle = IdleBulbState.fromOrdinal(tag.getInteger("leftIdle"));
		}
		if (tag.hasKey("rightIdle")) {
			rightIdle = IdleBulbState.fromOrdinal(tag.getInteger("rightIdle"));
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
