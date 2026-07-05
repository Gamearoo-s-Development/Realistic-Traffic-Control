package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.WigWagBlockEntity;
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

/** Port of 1.12.2 {@code BlockWigWag}. Swinging arm rendered by BER. */
public class BlockWigWag extends Block implements EntityBlock {

    public BlockWigWag(Properties properties) {
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
        if (state.getBlock() != ModBlocks.WIG_WAG.get()) {
            return super.getShape(state, level, pos, context);
        }
        return switch (state.getValue(RTCProperties.ROTATION)) {
            case 0 -> RTCShapes.blockBox(-6, 0, 10, 9, 16, 6);
            case 8 -> RTCShapes.blockBox(7, 0, 10, 22, 16, 6);
            case 4 -> RTCShapes.blockBox(6, 0, 9, 10, 16, -6);
            case 12 -> RTCShapes.blockBox(6, 0, 6, 10, 16, 21);
            case 1, 15, 7, 9, 3, 5, 11, 13 -> RTCShapes.blockBox(6, 0, 6, 12, 16, 12);
            case 2, 6, 10, 14 -> RTCShapes.blockBox(3.2, 0, 3.2, 12.8, 16, 12.8);
            default -> super.getShape(state, level, pos, context);
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WigWagBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.WIG_WAG.get(),
                (lvl, pos, st, be) -> WigWagBlockEntity.clientTick(lvl, pos, st, be));
    }

    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> served, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == served ? (BlockEntityTicker<A>) ticker : null;
    }
}
