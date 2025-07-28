package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo.EnumCheckboxOrientation;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.google.common.collect.ImmutableList;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class TrafficLight4HozFrameContainer extends BaseTrafficLightFrameContainer
{
	public TrafficLight4HozFrameContainer(InventoryPlayer inventory, ItemStack frameStack)
	{
		super(inventory, frameStack);
	}
	
	@Override
	protected List<FrameSlotInfo> getItemSlots(IItemHandler frameStackHandler) 
	{
		return ImmutableList
			.<FrameSlotInfo>builder()
			.add(new FrameSlotInfo(EnumCheckboxOrientation.LEFT, new SlotItemHandlerListenable(frameStackHandler, 0, 30, 90)))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.ABOVE, new SlotItemHandlerListenable(frameStackHandler, 1, 62, 90)))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.BELOW, new SlotItemHandlerListenable(frameStackHandler, 2, 95, 90)))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, new SlotItemHandlerListenable(frameStackHandler, 3, 125, 90)))
			.build();
	}
	
	@Override
	protected BaseItemTrafficLightFrame getValidFrameItem() 
	{
		return ModItems.traffic_light_4_hoz_frame;
	}

	@Override
	protected int getYSize() 
	{
		return 263;
	}	
}
