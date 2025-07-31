package com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TabTools extends CreativeTabs {
	
	public TabTools(String label) {
		super("tools_tab");
		
		
	}
	
	@Override
	public ItemStack getTabIconItem() {
		return new ItemStack(ModItems.cover_hook);
	}
}
