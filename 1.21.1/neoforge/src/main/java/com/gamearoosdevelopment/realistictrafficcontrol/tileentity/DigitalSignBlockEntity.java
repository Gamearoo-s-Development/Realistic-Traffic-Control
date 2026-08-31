package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class DigitalSignBlockEntity extends SignBlockEntity {
    public DigitalSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_SIGN.get(), pos, state);
        setID(Sign.DEFAULT_BLANK_SIGN);
    }
}
