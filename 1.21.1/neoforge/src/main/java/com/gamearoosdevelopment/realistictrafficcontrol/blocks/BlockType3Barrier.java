package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.world.level.block.Block;

/** Port of 1.12.2 {@code BlockType3Barrier}. */
public class BlockType3Barrier extends BlockType3BarrierBase {

    public BlockType3Barrier(Properties properties) {
        super(properties);
    }

    @Override
    public Block getBlockInstance() {
        return this;
    }
}
