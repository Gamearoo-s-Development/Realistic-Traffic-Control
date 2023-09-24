package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo.EnumCheckboxOrientation;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.google.common.collect.ImmutableList;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class TrafficLight1FrameContainer extends BaseTrafficLightFrameContainer
{
	public TrafficLight1FrameContainer(InventoryPlayer inventory, ItemStack frameStack)
	{
		super(inventory, frameStack);
	}
	
	@Override
	protected List<FrameSlotInfo> getItemSlots(IItemHandler frameStackHandler)
	{
		return ImmutableList
				.<FrameSlotInfo>builder()
				.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, new SlotItemHandlerListenable(frameStackHandler, 0, 79, 44)))
				.build();
	}
	
	@Override
	protected BaseItemTrafficLightFrame getValidFrameItem()
	{
		return ModItems.traffic_light_1_frame;
	}
	
	@Override
	protected int getYSize()
	{
		return 200;
	}
}
