package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Vehicle-detection sensor pad queried by the control box's automator. Replaces the three separate 1.12.2
 * classes ({@code BlockTrafficSensorLeft/Straight/Right}) with one block carrying a {@link SensorKind};
 * the control box distinguishes movement type via {@link #getKind()} and the approach via the shared
 * 16-step {@link RTCProperties#ROTATION}.
 */
public class TrafficSensorBlock extends RotatedBlock {

    public enum SensorKind {
        LEFT,
        STRAIGHT,
        RIGHT
    }

    private final SensorKind kind;

    public TrafficSensorBlock(Properties properties, VoxelShape shape, SensorKind kind) {
        super(properties, shape);
        this.kind = kind;
    }

    public SensorKind getKind() {
        return kind;
    }
}
