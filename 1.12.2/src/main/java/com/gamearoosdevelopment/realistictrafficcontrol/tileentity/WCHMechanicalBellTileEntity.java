package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModSounds;

import net.minecraft.util.SoundEvent;

public class WCHMechanicalBellTileEntity extends BellBaseTileEntity
{
	@Override
	protected SoundEvent getSoundEvent() 
	{
		return ModSounds.wch_mechanical_bell;
	}
}
