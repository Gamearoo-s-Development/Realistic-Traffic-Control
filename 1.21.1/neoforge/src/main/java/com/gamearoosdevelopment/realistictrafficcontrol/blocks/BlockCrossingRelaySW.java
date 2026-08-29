package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockCrossingRelaySW extends RelayBlockBase {
    public BlockCrossingRelaySW(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected String registryName() {
        return "crossing_relay_sw";
    }
}
