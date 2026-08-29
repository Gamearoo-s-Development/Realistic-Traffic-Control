package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;

import net.minecraft.util.EnumFacing;

public final class TrafficLightOcApproachHelper {
	private TrafficLightOcApproachHelper() {}

	public static EnumFacing parseApproach(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Direction must not be null");
		}
		switch (name.trim().toLowerCase()) {
			case "n":
			case "north":
				return EnumFacing.NORTH;
			case "s":
			case "south":
				return EnumFacing.SOUTH;
			case "e":
			case "east":
				return EnumFacing.EAST;
			case "w":
			case "west":
				return EnumFacing.WEST;
			default:
				throw new IllegalArgumentException("Invalid direction: " + name + " (use north/south/east/west or n/s/e/w)");
		}
	}

	public static String approachName(EnumFacing facing) {
		return facing == null ? "unknown" : facing.getName().toLowerCase();
	}

	public static EnumFacing resolveApproach(BaseTrafficLightTileEntity light) {
		return TrafficLightFacingResolver.resolveApproachFacing(light);
	}
}
