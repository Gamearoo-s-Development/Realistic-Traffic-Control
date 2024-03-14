package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLight5HozFrameGui extends BaseTrafficLightFrameGui {

	public TrafficLight5HozFrameGui(InventoryPlayer inventory, ItemStack frameStack)
	{
		super(new TrafficLight5HozFrameContainer(inventory, frameStack));
		
		xSize = 174;
		ySize = 263;
	}
	
	@Override
	protected String getGuiPngName() {
		return "traffic_light_5_hoz_frame_gui.png";
	}

	
}
