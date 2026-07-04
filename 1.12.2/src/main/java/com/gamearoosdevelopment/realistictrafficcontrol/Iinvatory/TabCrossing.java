package com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TabCrossing extends CreativeTabs {

	public TabCrossing(String label) {
		super("crossing_tab");
	}

	@Override
	public ItemStack getTabIconItem() {
		return new ItemStack(ModBlocks.wig_wag);
	}
}
