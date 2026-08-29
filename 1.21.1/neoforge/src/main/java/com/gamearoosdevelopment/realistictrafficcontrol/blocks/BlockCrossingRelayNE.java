package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockCrossingRelayNE extends RelayBlockBase {
    public BlockCrossingRelayNE(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected String registryName() {
        return "crossing_relay_ne";
    }
}
