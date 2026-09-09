package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Port of 1.12.2 {@code BlockCrossingGateLamps} (extends {@code BlockLampBase} directly, not rotatable pole-connect). */
public class BlockCrossingGateLamps extends BlockLampBase implements IHorizontalPoleConnectable {

    private static final VoxelShape POLE_SHAPE = Block.box(6, 0, 6, 10, 16, 10);

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
                CustomAngleCalculator.rotationForPlacement(context));
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return RTCProperties.rotate16(state, rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return RTCProperties.mirror16(state, mirror);
    }

    @Override
    public boolean canConnectHorizontalPole(BlockState state, Direction fromFacing) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return POLE_SHAPE;
    }
}
