package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLight8FrameGui extends BaseTrafficLightFrameGui {

	public TrafficLight8FrameGui(InventoryPlayer inventory, ItemStack frameStack) {
		super(new TrafficLight8FrameContainer(inventory, frameStack));
		
		xSize = 174;
		ySize = 210;
	}
	
	@Override
	protected String getGuiPngName() {
		return "traffic_light_8_frame_gui.png";
	}
	
}
