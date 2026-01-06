package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo.EnumCheckboxOrientation;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.google.common.collect.ImmutableList;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLight4FrameContainer extends BaseTrafficLightFrameContainer
{
	public TrafficLight4FrameContainer(InventoryPlayer inventory, ItemStack frameStack)
	{
		super(inventory, frameStack);
	}
	
	@Override
	protected List<FrameSlotInfo> buildSlotInfo() 
	{
		return ImmutableList
			.<FrameSlotInfo>builder()
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 0, 79, 44))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 1, 79, 75))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 2, 79, 106))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 3, 79, 139))
			.build();
	}
	
	@Override
	protected BaseItemTrafficLightFrame getValidFrameItem() 
	{
		return ModItems.traffic_light_4_frame;
	}

	@Override
	protected int getYSize() 
	{
		return 263;
	}	
}
