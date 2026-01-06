package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo.EnumCheckboxOrientation;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.google.common.collect.ImmutableList;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLight5HozFrameContainer extends BaseTrafficLightFrameContainer {

	public TrafficLight5HozFrameContainer(InventoryPlayer inventory, ItemStack frameStack) {
		super(inventory, frameStack);
	}
		
	@Override
	protected List<FrameSlotInfo> buildSlotInfo() {
		return ImmutableList
			.<FrameSlotInfo>builder()
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 0, 15, 76))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 1, 45, 76))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 2, 79, 76))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 3, 110, 76))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 4, 140, 76))
			.build();
	}


	@Override
	protected BaseItemTrafficLightFrame getValidFrameItem() {
		return ModItems.traffic_light_5_hoz_frame;
	}

	@Override
	protected int getYSize() {
		// TODO Auto-generated method stub
		return 263;
	}	
}
