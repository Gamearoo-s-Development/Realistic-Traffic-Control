package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignControllerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockDigitalSignController extends BlockDisplayControllerBase {
    public BlockDigitalSignController(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigitalSignControllerBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.DIGITAL_SIGN_CONTROLLER.get()
                ? (l, p, s, be) -> DigitalSignControllerBlockEntity.serverTick(l, p, s,
                        (DigitalSignControllerBlockEntity) be) : null;
    }
}
