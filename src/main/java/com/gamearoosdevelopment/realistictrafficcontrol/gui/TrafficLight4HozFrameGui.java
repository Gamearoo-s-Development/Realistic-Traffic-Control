package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLight4HozFrameGui extends BaseTrafficLightFrameGui
{
	public TrafficLight4HozFrameGui(InventoryPlayer inventory, ItemStack frameStack)
	{
		super(new TrafficLight4HozFrameContainer(inventory, frameStack));
		
		xSize = 174;
		ySize = 263;
	}
	
	@Override
	protected String getGuiPngName() {
		return "traffic_light_4_hoz_frame_gui.png";
	}
}
