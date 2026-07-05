package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.WireAnchorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Port of 1.12.2 {@code BlockWireAnchor}. Wood variant derived from block below. */
public class BlockWireAnchor extends Block implements EntityBlock {

    public BlockWireAnchor(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RTCProperties.WOOD, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.WOOD);
    }

    public static BlockState withWoodVariant(BlockGetter level, BlockPos pos, BlockState state) {
        boolean wood = level.getBlockState(pos.below()).is(ModBlocks.WOOD_POLE.get());
        return state.setValue(RTCProperties.WOOD, wood);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WireAnchorBlockEntity(pos, state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return withWoodVariant(level, pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            level.setBlock(pos, withWoodVariant(level, pos, state), Block.UPDATE_CLIENTS);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }
}
