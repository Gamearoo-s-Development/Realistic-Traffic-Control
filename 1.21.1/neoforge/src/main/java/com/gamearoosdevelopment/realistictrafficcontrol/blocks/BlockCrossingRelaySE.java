package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockCrossingRelaySE extends RelayBlockBase {
    public BlockCrossingRelaySE(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected String registryName() {
        return "crossing_relay_se";
    }
}
