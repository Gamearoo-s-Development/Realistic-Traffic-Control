package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLight2HozFrameGui extends BaseTrafficLightFrameGui
{
	public TrafficLight2HozFrameGui(InventoryPlayer inventory, ItemStack frameStack)
	{
		super(new TrafficLight2HozFrameContainer(inventory, frameStack));
		
		xSize = 174;
		ySize = 200;
	}
	
	@Override
	protected String getGuiPngName()
	{
		return "traffic_light_2_hoz_frame_gui.png";
	}
}
