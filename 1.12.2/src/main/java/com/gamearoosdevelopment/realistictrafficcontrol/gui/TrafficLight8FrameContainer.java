package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo.EnumCheckboxOrientation;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.google.common.collect.ImmutableList;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLight8FrameContainer extends BaseTrafficLightFrameContainer {

	public TrafficLight8FrameContainer(InventoryPlayer player, ItemStack frameStack) {
		super(player, frameStack);
	}
	
	@Override
	protected List<FrameSlotInfo> buildSlotInfo()
	{
		return ImmutableList
			.	<FrameSlotInfo>builder()
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 0, 76, 11))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 1, 76, 48))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 2, 55, 85))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 3, 98, 85))
			
			.build();
	}

	@Override
	protected int getYSize() {
		return 210;
	}

	@Override
	protected BaseItemTrafficLightFrame getValidFrameItem() {
		return ModItems.traffic_light_8_frame;
	}

}
