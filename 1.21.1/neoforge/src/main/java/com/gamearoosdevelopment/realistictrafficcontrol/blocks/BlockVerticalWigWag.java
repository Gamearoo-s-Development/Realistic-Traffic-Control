package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.VerticalWigWagBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Port of 1.12.2 {@code BlockVerticalWigWag}. */
public class BlockVerticalWigWag extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(2, 0, 3, 14, 10, 13);

    public BlockVerticalWigWag(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(RTCProperties.ROTATION, 0)
                .setValue(RTCProperties.ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION, RTCProperties.ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(RTCProperties.ROTATION, CustomAngleCalculator.getRotationForYaw(context.getRotation()))
                .setValue(RTCProperties.ACTIVE, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(RTCProperties.ACTIVE) ? 15 : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VerticalWigWagBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.VERTICAL_WIG_WAG.get(),
                (lvl, pos, st, be) -> VerticalWigWagBlockEntity.clientTick(lvl, pos, st, be));
    }

    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> served, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == served ? (BlockEntityTicker<A>) ticker : null;
    }
}
