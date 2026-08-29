package com.gamearoosdevelopment.realistictrafficcontrol.util;

import java.lang.reflect.Field;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Helpers for {@link BlockBehaviour.Properties}. 1.12.2 blocks used {@code getOffset}; 1.21 uses
 * {@link BlockBehaviour.OffsetFunction} on properties (field is package-private, so we assign via reflection).
 */
public final class RTCBlockProperties {

    private RTCBlockProperties() {
    }

    /** Sit the block model on top of the collision shape of the block below (cones, barriers, etc.). */
    public static BlockBehaviour.Properties groundSnapOffset(BlockBehaviour.Properties properties) {
        properties = properties.offsetType(BlockBehaviour.OffsetType.XYZ).dynamicShape();
        setOffsetFunction(properties, RTCBlockProperties::groundSnapOffset);
        return properties;
    }

    private static Vec3 groundSnapOffset(BlockBehaviour.BlockStateBase state, BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        VoxelShape shape = level.getBlockState(below).getShape(level, below);
        double offsetY = 1.0 - shape.max(Direction.Axis.Y);
        return new Vec3(0, -offsetY, 0);
    }

    private static void setOffsetFunction(BlockBehaviour.Properties properties,
            BlockBehaviour.OffsetFunction offsetFunction) {
        try {
            Field field = BlockBehaviour.Properties.class.getDeclaredField("offsetFunction");
            field.setAccessible(true);
            field.set(properties, offsetFunction);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to assign block offset function", e);
        }
    }
}
