package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Crossing-style lamps with 16-step horizontal rotation and horizontal-pole connection hints.
 * Port of 1.12.2 {@code BlockRotatableCrossingLamps}.
 */
public abstract class BlockRotatableCrossingLamps extends BlockLampBase implements IHorizontalPoleConnectable {

    public BlockRotatableCrossingLamps(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(RTCProperties.ROTATION, 0)
                .setValue(RTCProperties.LAMP_STATE, CrossingLampState.Off)
                .setValue(RTCProperties.NORTH, false)
                .setValue(RTCProperties.SOUTH, false)
                .setValue(RTCProperties.EAST, false)
                .setValue(RTCProperties.WEST, false)
                .setValue(RTCProperties.DOWN, false));
    }

    @Override
    protected void defineLampState(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION, RTCProperties.NORTH, RTCProperties.SOUTH, RTCProperties.EAST,
                RTCProperties.WEST, RTCProperties.DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(RTCProperties.ROTATION,
                CustomAngleCalculator.getRotationForYaw(context.getRotation()));
        return withConnectionHints(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return withConnectionHints(level, pos, state);
    }

    public static BlockState withConnectionHints(BlockGetter level, BlockPos pos, BlockState state) {
        return state
                .setValue(RTCProperties.NORTH, checkDirection(level, pos, Direction.NORTH))
                .setValue(RTCProperties.WEST, checkDirection(level, pos, Direction.WEST))
                .setValue(RTCProperties.SOUTH, checkDirection(level, pos, Direction.SOUTH))
                .setValue(RTCProperties.EAST, checkDirection(level, pos, Direction.EAST))
                .setValue(RTCProperties.DOWN, checkDirection(level, pos, Direction.DOWN));
    }

    private static boolean checkDirection(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState otherState = level.getBlockState(pos.relative(facing));
        if (facing == Direction.DOWN && otherState.getBlock().builtInRegistryHolder().key().location().getNamespace()
                .equalsIgnoreCase(ModRealisticTrafficControl.MODID)) {
            return true;
        }
        if (otherState.getBlock() instanceof IHorizontalPoleConnectable connectable) {
            return connectable.canConnectHorizontalPole(otherState, facing.getOpposite());
        }
        return !otherState.getFaceOcclusionShape(level, pos, facing).isEmpty();
    }

    @Override
    public boolean canConnectHorizontalPole(BlockState state, Direction fromFacing) {
        return true;
    }
}
