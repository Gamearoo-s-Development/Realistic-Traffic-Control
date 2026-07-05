package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code StreetLightDoubleTileEntity}. */
public class StreetLightDoubleBlockEntity extends BlockEntity {

    public StreetLightDoubleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STREET_LIGHT_DOUBLE.get(), pos, state);
    }
}
