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
                .setValue(RTCProperties.ROTATION, CustomAngleCalculator.rotationForPlacement(context))
                .setValue(RTCProperties.ACTIVE, false);
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
        return RTCShapes.rotateY(RTCShapes.blockBox(-6, 0, 6, 9, 16, 10),
                state.getValue(RTCProperties.ROTATION));
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

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return com.gamearoosdevelopment.realistictrafficcontrol.util.RTCDrops.self(state);
    }
}
