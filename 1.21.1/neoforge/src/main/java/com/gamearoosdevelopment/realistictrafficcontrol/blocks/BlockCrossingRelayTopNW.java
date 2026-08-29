package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockCrossingRelayTopNW extends RelayBlockBase {
    public BlockCrossingRelayTopNW(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected String registryName() {
        return "crossing_relay_top_nw";
    }
}
