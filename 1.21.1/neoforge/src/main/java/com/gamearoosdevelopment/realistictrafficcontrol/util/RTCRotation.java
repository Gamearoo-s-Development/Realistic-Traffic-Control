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
}
