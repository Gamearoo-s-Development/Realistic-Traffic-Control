package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModSounds;

import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SafetranType3TileEntity extends BellBaseTileEntity {

	@SideOnly(Side.CLIENT)
	@Override
	protected SoundEvent getSoundEvent() {
		return ModSounds.safetranType3Event;
	}
	
}
