package com.gamearoosdevelopment.realistictrafficcontrol.util;

import java.util.Arrays;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Converts between the mod's 0-15 "rotation" property (16 rotational steps around Y) and vanilla
 * metadata / facing directions. Ported verbatim from 1.12.2 ({@code EnumFacing} -> {@link Direction},
 * {@code MathHelper} -> {@link Mth}).
 */
public class CustomAngleCalculator {
    private static final int[] metas = new int[] { 2, 4, 5, 6, 3, 7, 8, 9, 0, 10, 11, 12, 1, 13, 14, 15 };
    private static final int[] rotations = new int[] { 8, 12, 0, 4, 1, 2, 3, 5, 6, 7, 9, 10, 11, 13, 14, 15 };
    private static final int[] cardinals = new int[] { 0, 4, 8, 12 };

    public static int rotationToMeta(int rotation) {
        return metas[rotation];
    }

    public static int metaToRotation(int meta) {
        return rotations[meta];
    }

    public static int getRotationForYaw(float yaw) {
        return Mth.floor((double) ((yaw + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
    }

    /** Nearest 4-way step (S/W/N/E) for frame-style placement. */
    public static int getRotationForYawCardinal(float yaw) {
        int raw = getRotationForYaw(yaw);
        int best = cardinals[0];
        int bestDist = 16;
        for (int c : cardinals) {
            int dist = Math.abs(raw - c);
            dist = Math.min(dist, 16 - dist);
            if (dist < bestDist) {
                bestDist = dist;
                best = c;
            }
        }
        return best;
    }

    public static boolean isCardinal(int rotation) {
        return Arrays.stream(cardinals).anyMatch(num -> num == rotation);
    }

    public static boolean isNorthSouth(int rotation) {
        return isNorth(rotation) || isSouth(rotation);
    }

    public static boolean isNorth(int rotation) {
        return rotation >= 14 || rotation < 2;
    }

    public static boolean isEast(int rotation) {
        return rotation >= 2 && rotation < 6;
    }

    public static boolean isSouth(int rotation) {
        return rotation >= 6 && rotation < 10;
    }

    public static boolean isWest(int rotation) {
        return rotation >= 10 && rotation < 14;
    }

    // ONLY USE WHEN ROTATION IS CARDINAL!!
    public static Direction getFacingFromRotation(int rotation) {
        Direction workingFacing = Direction.NORTH;
        int rotationSteps = rotation / 4;
        for (int i = 0; i < rotationSteps; i++) {
            workingFacing = workingFacing.getClockWise();
        }

        return workingFacing;
    }

    public static Direction rotationToFacing(int rotation) {
        switch (rotation % 16) {
            case 0:
                return Direction.SOUTH;
            case 4:
                return Direction.WEST;
            case 8:
                return Direction.NORTH;
            case 12:
                return Direction.EAST;
            default:
                return Direction.NORTH; // fallback for diagonal or undefined
        }
    }

    public static boolean isRotationFacing(int rotation, Direction facing) {
        switch (facing) {
            case NORTH:
                return isNorth(rotation);
            case SOUTH:
                return isSouth(rotation);
            case WEST:
                return isWest(rotation);
            case EAST:
                return isEast(rotation);
            default:
                return false;
        }
    }
}
