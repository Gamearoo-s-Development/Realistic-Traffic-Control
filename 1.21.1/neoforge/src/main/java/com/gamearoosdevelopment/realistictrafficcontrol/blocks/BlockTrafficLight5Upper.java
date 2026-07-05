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
        registerDefaultState(defaultBlockState().setValue(RTCProperties.ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int rotation = CustomAngleCalculator.getRotationForYaw(context.getRotation());
        return defaultBlockState().setValue(RTCProperties.ROTATION, rotation);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int rotation = state.getValue(RTCProperties.ROTATION);
        return switch (rotation) {
            case 0 -> box(3, 0, 7, 13, 16, 12);
            case 8 -> box(3, 0, 4, 13, 16, 9);
            case 4 -> box(4, 0, 3, 9, 16, 13);
            case 12 -> box(7, 0, 3, 12, 16, 13);
            case 1, 15, 7, 9, 3, 5, 11, 13 -> box(6, 0, 6, 12, 16, 12);
            case 2, 6, 10, 14 -> box(3.2, 0, 3.2, 12.8, 16, 12.8);
            default -> Block.box(0, 0, 0, 16, 16, 16);
        };
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
