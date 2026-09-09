package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Vehicle-detection sensor pad queried by the control box's automator. Replaces the three separate 1.12.2
 * classes ({@code BlockTrafficSensorLeft/Straight/Right}) with one block carrying a {@link SensorKind};
 * the control box distinguishes movement type via {@link #getKind()} and approach via
 * {@link BlockStateProperties#HORIZONTAL_FACING}.
 */
public class TrafficSensorBlock extends Block {

    public enum SensorKind {
        LEFT,
        STRAIGHT,
        RIGHT
    }

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    private final SensorKind kind;

    public TrafficSensorBlock(Properties properties, SensorKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    public SensorKind getKind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator.horizontalFacingForPlacement(context));
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return RTCProperties.rotateFacing(state, rotation, BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return RTCProperties.mirrorFacing(state, mirror, BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return com.gamearoosdevelopment.realistictrafficcontrol.util.RTCDrops.self(state);
    }
}
