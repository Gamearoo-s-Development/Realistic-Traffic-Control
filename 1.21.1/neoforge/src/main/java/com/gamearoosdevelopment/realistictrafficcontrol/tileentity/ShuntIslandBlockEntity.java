package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.function.Consumer;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class ShuntIslandBlockEntity extends ShuntBaseBlockEntity {

    public ShuntIslandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHUNT.get(), pos, state);
    }

    @Override
    protected Consumer<BlockState> getRelayAddOrRemoveShuntMethod(RelayBlockEntity relay) {
        return blockState -> {
            BlockPos originPos = getTrackOrigin();
            Direction facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            relay.addOrRemoveShuntIsland(originPos, facing);
        };
    }
}
