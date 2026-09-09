package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCDrops;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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
        registerDefaultState(defaultRotatedState());
    }

    protected BlockState defaultRotatedState() {
        return getStateDefinition().any().setValue(RTCProperties.ROTATION, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(RTCProperties.ROTATION, CustomAngleCalculator.rotationForPlacement(context));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return RTCProperties.rotate16(state, rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return RTCProperties.mirror16(state, mirror);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return RTCShapes.rotateY(shape, state.getValue(RTCProperties.ROTATION));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return RTCShapes.clipToBlock(getShape(state, level, pos, context));
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return RTCDrops.self(state);
    }
}
