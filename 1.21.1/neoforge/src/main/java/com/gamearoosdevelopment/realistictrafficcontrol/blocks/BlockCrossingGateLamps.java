package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

/**
 * Port of 1.12.2 {@code BlockCrossingGateLamps}.
 */
public class BlockCrossingGateLamps extends BlockRotatableCrossingLamps {

    public BlockCrossingGateLamps(Properties properties) {
        super(properties);
    }

    @Override
    public String getLampRegistryName() {
        return "crossing_gate_lamps";
    }
}
