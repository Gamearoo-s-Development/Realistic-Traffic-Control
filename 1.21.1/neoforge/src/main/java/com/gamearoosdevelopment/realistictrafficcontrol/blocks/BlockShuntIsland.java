package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.ShuntIslandBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockShuntIsland extends ShuntBlockBase {

    public BlockShuntIsland(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity newShuntEntity(BlockPos pos, BlockState state) {
        return new ShuntIslandBlockEntity(pos, state);
    }
}
