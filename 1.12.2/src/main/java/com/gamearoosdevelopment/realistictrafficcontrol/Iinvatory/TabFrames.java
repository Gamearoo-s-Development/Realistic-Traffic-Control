package com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TabFrames extends CreativeTabs {
	
	public TabFrames(String label) {
		super("frames_tab");
		
		
	}
	
	@Override
	public ItemStack getTabIconItem() {
		return new ItemStack(ModItems.traffic_light_frame);
	}
}
