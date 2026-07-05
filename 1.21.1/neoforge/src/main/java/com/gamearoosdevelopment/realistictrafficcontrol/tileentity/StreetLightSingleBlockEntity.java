package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code StreetLightSingleTileEntity}. */
public class StreetLightSingleBlockEntity extends BlockEntity {

    public StreetLightSingleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STREET_LIGHT_SINGLE.get(), pos, state);
    }
}
