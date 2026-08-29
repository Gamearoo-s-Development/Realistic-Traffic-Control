package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockCrossingRelayTopSW extends RelayBlockBase {
    public BlockCrossingRelayTopSW(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected String registryName() {
        return "crossing_relay_top_sw";
    }
}
