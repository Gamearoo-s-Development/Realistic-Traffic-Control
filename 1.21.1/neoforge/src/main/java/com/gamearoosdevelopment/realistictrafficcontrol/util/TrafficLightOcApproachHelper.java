package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;

import net.minecraft.core.Direction;

public final class TrafficLightOcApproachHelper {
    private TrafficLightOcApproachHelper() {
    }

    public static Direction parseApproach(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Direction must not be null");
        }
        return switch (name.trim().toLowerCase()) {
            case "n", "north" -> Direction.NORTH;
            case "s", "south" -> Direction.SOUTH;
            case "e", "east" -> Direction.EAST;
            case "w", "west" -> Direction.WEST;
            default -> throw new IllegalArgumentException(
                    "Invalid direction: " + name + " (use north/south/east/west or n/s/e/w)");
        };
    }

    public static String approachName(Direction facing) {
        return facing == null ? "unknown" : facing.getName().toLowerCase();
    }

    public static Direction resolveApproach(TrafficLightBlockEntity light) {
        return TrafficLightFacingResolver.resolveApproachFacing(light);
    }
}
