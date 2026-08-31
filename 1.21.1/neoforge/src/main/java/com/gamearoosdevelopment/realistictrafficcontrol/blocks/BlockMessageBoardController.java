package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardControllerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockMessageBoardController extends BlockDisplayControllerBase {
    public BlockMessageBoardController(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MessageBoardControllerBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.MESSAGE_BOARD_CONTROLLER.get()
                ? (l, p, s, be) -> MessageBoardControllerBlockEntity.serverTick(l, p, s,
                        (MessageBoardControllerBlockEntity) be) : null;
    }
}
