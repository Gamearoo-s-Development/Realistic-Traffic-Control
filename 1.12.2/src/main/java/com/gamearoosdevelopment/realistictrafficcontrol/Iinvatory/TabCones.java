package com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TabCones extends CreativeTabs {
	
	public TabCones(String label) {
		super("cones_tab");
		
		
	}
	
	@Override
	public ItemStack getTabIconItem() {
		return new ItemStack(ModBlocks.drum);
	}
}
