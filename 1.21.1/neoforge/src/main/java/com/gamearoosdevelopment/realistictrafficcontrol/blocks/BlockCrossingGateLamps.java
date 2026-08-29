package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Port of 1.12.2 {@code BlockCrossingGateLamps} (extends {@code BlockLampBase} directly, not rotatable pole-connect). */
public class BlockCrossingGateLamps extends BlockLampBase implements IHorizontalPoleConnectable {

    public BlockCrossingGateLamps(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(RTCProperties.ROTATION, 0)
                .setValue(RTCProperties.LAMP_STATE, CrossingLampState.Off));
    }

    @Override
    public String getLampRegistryName() {
        return "crossing_gate_lamps";
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
    public boolean canConnectHorizontalPole(BlockState state, net.minecraft.core.Direction fromFacing) {
        return true;
    }
}
