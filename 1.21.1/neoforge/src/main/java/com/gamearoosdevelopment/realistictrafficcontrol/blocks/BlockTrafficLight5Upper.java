package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Top half of the tall 5-bulb vertical traffic signal. Port of 1.12.2 {@code BlockTrafficLight5Upper}.
 */
public class BlockTrafficLight5Upper extends Block {

    public BlockTrafficLight5Upper(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(RTCProperties.ROTATION, 0)
                .setValue(RTCProperties.MOUNT_FACE, net.minecraft.core.Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION, RTCProperties.MOUNT_FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(RTCProperties.ROTATION, CustomAngleCalculator.rotationForPlacement(context))
                .setValue(RTCProperties.MOUNT_FACE, com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly.mountFace(context));
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes.rotateYPoleMounted(
                box(3, 0, -6, 13, 16, 0),
                state.getValue(RTCProperties.ROTATION),
                com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly.mountCardinal(state));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockState(pos.below()).is(ModBlocks.TRAFFIC_LIGHT_5.get())) {
            level.removeBlock(pos.below(), false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
