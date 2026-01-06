package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo.EnumCheckboxOrientation;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.google.common.collect.ImmutableList;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLight7FrameContainer extends BaseTrafficLightFrameContainer {

	public TrafficLight7FrameContainer(InventoryPlayer player, ItemStack frameStack) {
		super(player, frameStack);
	}
	
	@Override
	protected List<FrameSlotInfo> buildSlotInfo() 
	{
		return ImmutableList
				.<FrameSlotInfo>builder()
				.add(new FrameSlotInfo(EnumCheckboxOrientation.LEFT, 0, 59, 11))
				.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 1, 110, 11))
				.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 2, 83, 52))
				
				.build();
	}

	@Override
	protected int getYSize() {
		
		return 210;
	}

	@Override
	protected BaseItemTrafficLightFrame getValidFrameItem() {
	
		return ModItems.traffic_light_7_frame;
	}

}
