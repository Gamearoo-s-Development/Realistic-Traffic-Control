package com.gamearoosdevelopment.realistictrafficcontrol.util;

/**
 * 16-step Y rotation used by traffic-light frames and other RTC blocks. Matches the 1.12.2 blockstate
 * {@code y} values ({@code 0, 337, 315, …}) mapped to {@code rotation=0..15}.
 */
public final class RTCRotation {

    public static final float[] DEGREES = {
            0, 337, 315, 292, 270, 247, 225, 202, 180, 157, 135, 112, 90, 67, 45, 22
    };

    private RTCRotation() {
    }

    public static float degreesForStep(int rotation) {
        if (rotation < 0 || rotation >= DEGREES.length) {
            return 0;
        }
        return DEGREES[rotation];
    }

    /** Y rotation applied to placed blocks and BER bulbs; matches 1.12.2 TESR {@code rotation * -22.5F}. */
    public static float placementRotationDegrees(int rotation) {
        return (rotation & 15) * -22.5f;
    }

    /**
     * Steps away from the nearest 90° mount (0/4/8/12). Frames and signs sit like
     * that cardinal, then yaw this many 22.5° ticks around the pole.
     */
    public static int deltaFromCardinal(int rotation) {
        int raw = rotation & 15;
        int card = CustomAngleCalculator.nearestCardinal(raw);
        int delta = raw - card;
        if (delta > 8) {
            delta -= 16;
        } else if (delta < -8) {
            delta += 16;
        }
        return delta;
    }

    /** Neighbor offset from the frame/sign block to the pole it mounts on, at a cardinal. */
    public static float poleOffsetX(int cardinal) {
        return switch (cardinal & 15) {
            case 4 -> 1.0F;
            case 12 -> -1.0F;
            default -> 0.0F;
        };
    }

    public static float poleOffsetZ(int cardinal) {
        return switch (cardinal & 15) {
            case 8 -> 1.0F;
            case 0 -> -1.0F;
            default -> 0.0F;
        };
    }

    /**
     * Sit on {@code mountCardinal} (the clicked pole face), then yaw
     * {@code rotation - mount} around that pole.
     */
    public static void rotatePoleMountedXZ(float[] xz, int rotation) {
        rotatePoleMountedXZ(xz, rotation, CustomAngleCalculator.nearestCardinal(rotation));
    }

    public static void rotatePoleMountedXZ(float[] xz, int rotation, int mountCardinal) {
        int extra = wrapDelta((rotation & 15) - (mountCardinal & 15));
        rotateAround(xz, 0.5F, 0.5F, (float) Math.toRadians(placementRotationDegrees(mountCardinal)));
        if (extra != 0) {
            rotateAround(xz, 0.5F + poleOffsetX(mountCardinal), 0.5F + poleOffsetZ(mountCardinal),
                    (float) Math.toRadians(placementRotationDegrees(extra)));
        }
    }

    /**
     * Same as {@link #rotatePoleMountedXZ} but the point lives in a neighbor cell
     * ({@code originDx}/{@code originDz} = this cell minus the hoz cell).
     */
    public static void rotateParentedXZ(float[] xz, int rotation, int mountCardinal, int originDx, int originDz) {
        xz[0] += originDx;
        xz[1] += originDz;
        rotatePoleMountedXZ(xz, rotation, mountCardinal);
        xz[0] -= originDx;
        xz[1] -= originDz;
    }

    public static int wrapDelta(int delta) {
        int value = delta % 16;
        if (value > 8) {
            value -= 16;
        } else if (value < -8) {
            value += 16;
        }
        return value;
    }

    public static void rotateAround(float[] xz, float pivotX, float pivotZ, float radians) {
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float x = xz[0] - pivotX;
        float z = xz[1] - pivotZ;
        xz[0] = x * cos + z * sin + pivotX;
        xz[1] = -x * sin + z * cos + pivotZ;
    }

    /** Same Y spins as {@link #rotatePoleMountedXZ} but for a direction (no translation). */
    public static void rotatePoleMountedDirectionXZ(float[] xz, int rotation, int mountCardinal) {
        int extra = wrapDelta((rotation & 15) - (mountCardinal & 15));
        rotateAround(xz, 0F, 0F, (float) Math.toRadians(placementRotationDegrees(mountCardinal)));
        if (extra != 0) {
            rotateAround(xz, 0F, 0F, (float) Math.toRadians(placementRotationDegrees(extra)));
        }
    }
}
