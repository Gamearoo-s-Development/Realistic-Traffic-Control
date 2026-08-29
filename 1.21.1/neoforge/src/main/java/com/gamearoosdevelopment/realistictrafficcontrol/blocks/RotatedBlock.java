package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base for the mod's many "16-step rotation" blocks (poles, stands, cones, ...). Replaces the 1.12.2
 * meta&lt;-&gt;rotation dance ({@code getMetaFromState}/{@code getStateFromMeta}) with a plain
 * {@link RTCProperties#ROTATION} blockstate value, and the fixed {@code AxisAlignedBB} with a shared
 * {@link VoxelShape}.
 */
public class RotatedBlock extends Block {
    private final VoxelShape shape;

    public RotatedBlock(Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
        registerDefaultState(getStateDefinition().any().setValue(RTCProperties.ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int rotation = CustomAngleCalculator.getRotationForYaw(context.getRotation());
        return defaultBlockState().setValue(RTCProperties.ROTATION, rotation);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
