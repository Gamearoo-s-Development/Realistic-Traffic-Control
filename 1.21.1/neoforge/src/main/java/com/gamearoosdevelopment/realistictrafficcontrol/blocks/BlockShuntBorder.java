package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.ShuntBorderBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockShuntBorder extends ShuntBlockBase {

    public BlockShuntBorder(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity newShuntEntity(BlockPos pos, BlockState state) {
        return new ShuntBorderBlockEntity(pos, state);
    }
}
