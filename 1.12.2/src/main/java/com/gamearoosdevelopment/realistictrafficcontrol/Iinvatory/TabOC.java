package com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TabOC extends CreativeTabs {
	
	public TabOC(String label) {
		
		super("oc_tab");
		
		
	}
	
	@Override
	public ItemStack getTabIconItem() {
		
		return new ItemStack(ModItems.traffic_light_card, 1, 1);
	}
}
