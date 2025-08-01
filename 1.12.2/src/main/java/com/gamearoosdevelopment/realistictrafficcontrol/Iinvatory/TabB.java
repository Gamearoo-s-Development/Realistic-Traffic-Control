package com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TabB extends CreativeTabs {
	
	public TabB(String label) {
		super("b_tab");
		
		
	}
	
	@Override
	public ItemStack getTabIconItem() {
		return new ItemStack(ModBlocks.concrete_barrier, 1, 1);
	}
}
