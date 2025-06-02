 package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLightHozFrameGui extends BaseTrafficLightFrameGui {

	public TrafficLightHozFrameGui(InventoryPlayer inventory, ItemStack frameStack) {
		super(new TrafficLightHozFrameContainer(inventory, frameStack));
		
		xSize = 174;
		ySize = 200;
	}

	@Override
	protected String getGuiPngName() {
		return "traffic_light_hoz_frame_gui.png";
	}

}
