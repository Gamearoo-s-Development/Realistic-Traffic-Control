package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Resolves which cardinal approach a traffic light serves, preferring an explicitly configured facing and
 * otherwise deriving it from the block's 16-step rotation. Ported from 1.12.2
 * ({@code BaseTrafficLightTileEntity} -&gt; {@link TrafficLightBlockEntity}, {@code EnumFacing} -&gt;
 * {@link Direction}).
 */
public final class TrafficLightFacingResolver {
    private TrafficLightFacingResolver() {
    }

    public static boolean isFacing(TrafficLightBlockEntity tl, Direction approach) {
        return approach != null && tl != null && resolveApproachFacing(tl) == approach;
    }

    public static Direction resolveApproachFacing(TrafficLightBlockEntity tl) {
        if (tl == null || tl.getLevel() == null) {
            return Direction.NORTH;
        }

        Direction configured = tl.getConfiguredApproachFacing();
        if (configured != null) {
            return configured;
        }

        BlockState state = tl.getLevel().getBlockState(tl.getBlockPos());
        if (!(state.getBlock() instanceof BlockBaseTrafficLight)) {
            return Direction.NORTH;
        }

        int rotation = state.getValue(RTCProperties.ROTATION);
        for (Direction facing : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            if (CustomAngleCalculator.isRotationFacing(rotation, facing)) {
                return facing;
            }
        }

        return CustomAngleCalculator.rotationToFacing(rotation);
    }

    public static Direction getOppositeApproach(Direction approach) {
        return approach == null ? Direction.NORTH : approach.getOpposite();
    }

    public static Direction getClockwiseApproach(Direction approach) {
        return approach == null ? Direction.EAST : approach.getClockWise();
    }

    /** Left phase that serves a right-only approach's protected right. */
    public static Direction getCoupledLeftApproachForRightOnly(Direction rightOnlyApproach) {
        return rightOnlyApproach == null ? Direction.NORTH : rightOnlyApproach.getCounterClockWise();
    }
}
