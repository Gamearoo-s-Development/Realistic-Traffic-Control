package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModSounds;

import net.minecraft.util.SoundEvent;

public class WaysideHornTileEntity extends BellBaseTileEntity
{
	@Override
	protected SoundEvent getSoundEvent() 
	{
		return ModSounds.waysideHornEvent;
	}
}