package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of 1.12.2 {@code BlockHorizontalPole}: a horizontally-oriented cross bar that attaches to
 * traffic-light heads. Uses the vanilla {@link HorizontalDirectionalBlock#FACING} property.
 */
public class HorizontalPoleBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape NS_SHAPE = Block.box(7, 7, 0, 9, 9, 16);
    private static final VoxelShape EW_SHAPE = Block.box(0, 7, 7, 16, 9, 9);

    public HorizontalPoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? NS_SHAPE : EW_SHAPE;
    }
}
