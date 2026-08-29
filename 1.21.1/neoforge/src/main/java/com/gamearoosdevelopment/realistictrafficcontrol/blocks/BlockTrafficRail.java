package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Port of 1.12.2 {@code BlockTrafficRail}: guardrail segments that chain along a facing axis. */
public class BlockTrafficRail extends Block {

    public BlockTrafficRail(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(RTCProperties.ISFURTHESTLEFT, true)
                .setValue(RTCProperties.ISFURTHESTRIGHT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, RTCProperties.ISFURTHESTLEFT, RTCProperties.ISFURTHESTRIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withChainHints(defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection()), context.getLevel(),
                context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return withChainHints(state, level, pos);
    }

    private static BlockState withChainHints(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        boolean furthestLeft = true;
        boolean furthestRight = true;

        BlockState left = level.getBlockState(pos.relative(facing.getCounterClockWise()));
        if (left.getBlock() instanceof BlockTrafficRail) {
            furthestLeft = left.getValue(BlockStateProperties.HORIZONTAL_FACING) != facing;
        }
        BlockState right = level.getBlockState(pos.relative(facing.getClockWise()));
        if (right.getBlock() instanceof BlockTrafficRail) {
            furthestRight = right.getValue(BlockStateProperties.HORIZONTAL_FACING) != facing;
        }
        return state.setValue(RTCProperties.ISFURTHESTLEFT, furthestLeft)
                .setValue(RTCProperties.ISFURTHESTRIGHT, furthestRight);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState actual = withChainHints(state, level, pos);
        double leftAmount = actual.getValue(RTCProperties.ISFURTHESTLEFT) ? 0 : 0.25;
        double rightAmount = actual.getValue(RTCProperties.ISFURTHESTRIGHT) ? 0 : 0.25;
        return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case NORTH -> RTCShapes.unitBox(0.25 - leftAmount, 0, 0.75, 0.75 + rightAmount, 1.25, 0.40625);
            case SOUTH -> RTCShapes.unitBox(0.25 - rightAmount, 0, 0.25, 0.75 + leftAmount, 1.25, 0.59375);
            case EAST -> RTCShapes.unitBox(0.25, 0, 0.25 - leftAmount, 0.59375, 1.25, 0.75 + rightAmount);
            case WEST -> RTCShapes.unitBox(0.40625, 0, 0.25 - rightAmount, 0.75, 1.25, 0.75 + leftAmount);
            default -> Shapes.block();
        };
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.getBlock() instanceof BlockTrafficRail) {
                    level.setBlock(neighbor, withChainHints(neighborState, level, neighbor), Block.UPDATE_CLIENTS);
                }
            }
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }
}
