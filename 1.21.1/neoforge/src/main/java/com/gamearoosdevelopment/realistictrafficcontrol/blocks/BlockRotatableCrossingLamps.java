package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Crossing-style lamps with 16-step horizontal rotation. Pole "auto connect" arms are not used on 1.21
 * (1.12 drew them only from Forge extended state; connection flags are not replicated in world blockstate).
 */
public abstract class BlockRotatableCrossingLamps extends BlockLampBase implements IHorizontalPoleConnectable {

    public BlockRotatableCrossingLamps(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(RTCProperties.ROTATION, 0)
                .setValue(RTCProperties.LAMP_STATE, CrossingLampState.Off));
    }

    @Override
    protected void defineLampState(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(RTCProperties.ROTATION,
                CustomAngleCalculator.getRotationForYaw(context.getRotation()));
    }

    @Override
    public boolean canConnectHorizontalPole(BlockState state, Direction fromFacing) {
        return true;
    }
}
