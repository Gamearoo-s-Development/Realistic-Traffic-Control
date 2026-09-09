package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ConcreteBarrierBlockItem;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCBlockProperties;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Port of 1.12.2 {@code BlockConcreteBarrier} with dye + facing in blockstate (no tile entity). */
public class BlockConcreteBarrier extends Block {

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty DYE =
            RTCProperties.BARRIER_DYE;

    private static final VoxelShape SHAPE = Block.box(0, 0, 5, 16, 14, 11);

    public BlockConcreteBarrier(Properties properties) {
        super(RTCBlockProperties.groundSnapOffset(properties));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DYE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DYE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int dye = ConcreteBarrierBlockItem.getDye(context.getItemInHand());
        return defaultBlockState()
                .setValue(FACING, com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator.horizontalFacingForPlacement(context))
                .setValue(DYE, dye);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return RTCProperties.rotateFacing(state, rotation, FACING);
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return RTCProperties.mirrorFacing(state, mirror, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(asItem());
        stack.set(RTCDataComponents.BARRIER_DYE.get(), state.getValue(DYE));
        return stack;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        ItemStack stack = new ItemStack(asItem());
        stack.set(RTCDataComponents.BARRIER_DYE.get(), state.getValue(DYE));
        return java.util.List.of(stack);
    }
}
