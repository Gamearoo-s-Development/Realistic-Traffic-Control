package com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TabPoles extends CreativeTabs {
	
	public TabPoles(String label) {
		super("poles_tab");
		
		
	}
	
	@Override
	public ItemStack getTabIconItem() {
		return new ItemStack(ModBlocks.pole);
	}
}
