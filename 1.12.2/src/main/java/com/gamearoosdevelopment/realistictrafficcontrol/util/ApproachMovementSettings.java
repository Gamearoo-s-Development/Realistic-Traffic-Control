package com.gamearoosdevelopment.realistictrafficcontrol.util;

import net.minecraft.nbt.NBTTagCompound;

public class ApproachMovementSettings {
	public boolean straightEnabled = true;
	public boolean leftEnabled = true;
	public boolean rightEnabled = true;
	public boolean sharedTurns = false;
	public boolean noOpposingRightWithLeft = false;
	public IdleBulbMode straightIdle = IdleBulbMode.SOLID_RED;
	public IdleBulbMode leftIdle = IdleBulbMode.ARROW_RED;
	public IdleBulbMode rightIdle = IdleBulbMode.ARROW_RED;
	public FyaMode leftFya = FyaMode.ALWAYS;
	public FyaMode rightFya = FyaMode.ALWAYS;

	public NBTTagCompound writeToNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setBoolean("straightEnabled", straightEnabled);
		tag.setBoolean("leftEnabled", leftEnabled);
		tag.setBoolean("rightEnabled", rightEnabled);
		tag.setBoolean("sharedTurns", sharedTurns);
		tag.setBoolean("noOpposingRightWithLeft", noOpposingRightWithLeft);
		tag.setInteger("straightIdle", straightIdle.ordinal());
		tag.setInteger("leftIdle", leftIdle.ordinal());
		tag.setInteger("rightIdle", rightIdle.ordinal());
		tag.setInteger("leftFya", leftFya.ordinal());
		tag.setInteger("rightFya", rightFya.ordinal());
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
		if (tag.hasKey("sharedTurns")) {
			sharedTurns = tag.getBoolean("sharedTurns");
		}
		if (tag.hasKey("noOpposingRightWithLeft")) {
			noOpposingRightWithLeft = tag.getBoolean("noOpposingRightWithLeft");
		}
		if (tag.hasKey("straightIdle")) {
			straightIdle = IdleBulbMode.fromLegacyOrdinal(tag.getInteger("straightIdle"), true);
		}
		if (tag.hasKey("leftIdle")) {
			leftIdle = IdleBulbMode.fromLegacyOrdinal(tag.getInteger("leftIdle"), false);
		}
		if (tag.hasKey("rightIdle")) {
			rightIdle = IdleBulbMode.fromLegacyOrdinal(tag.getInteger("rightIdle"), false);
		}
		if (tag.hasKey("leftFya")) {
			leftFya = FyaMode.fromOrdinal(tag.getInteger("leftFya"));
		}
		if (tag.hasKey("rightFya")) {
			rightFya = FyaMode.fromOrdinal(tag.getInteger("rightFya"));
		}
	}

	public ApproachMovementSettings copy() {
		ApproachMovementSettings copy = new ApproachMovementSettings();
		copy.straightEnabled = straightEnabled;
		copy.leftEnabled = leftEnabled;
		copy.rightEnabled = rightEnabled;
		copy.sharedTurns = sharedTurns;
		copy.noOpposingRightWithLeft = noOpposingRightWithLeft;
		copy.straightIdle = straightIdle;
		copy.leftIdle = leftIdle;
		copy.rightIdle = rightIdle;
		copy.leftFya = leftFya;
		copy.rightFya = rightFya;
		return copy;
	}
}
