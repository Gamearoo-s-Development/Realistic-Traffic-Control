package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;

import net.minecraft.world.level.block.Block;

/** Port of 1.12.2 {@code BlockType3BarrierRight}. */
public class BlockType3BarrierRight extends BlockType3BarrierBase {

    public BlockType3BarrierRight(Properties properties) {
        super(properties);
    }

    @Override
    public Block getBlockInstance() {
        return ModBlocks.TYPE_3_BARRIER_RIGHT.get();
    }
}
