package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;

import net.minecraft.block.Block;

public class BlockType3BarrierRight extends BlockType3BarrierBase {

	@Override
	protected String getName() {
		return "type_3_barrier_right";
	}

	@Override
	public Block getBlockInstance() {
		return ModBlocks.type_3_barrier_right;
	}

}
