package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockPedCrossingLamps extends BlockRotatableCrossingLamps {

	@Override
	public String getLampRegistryName() {
		return "ped_crossing_lamps";
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand,
			EnumFacing facing, float hitX, float hitY, float hitZ) {
		return false;
	}
}