package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo.EnumCheckboxOrientation;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.google.common.collect.ImmutableList;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLightHozFrameContainer extends BaseTrafficLightFrameContainer {

	public TrafficLightHozFrameContainer(InventoryPlayer inventory, ItemStack frameStack)
	{
		super(inventory, frameStack);
	}
	
	@Override
	protected List<FrameSlotInfo> buildSlotInfo() {
		return ImmutableList
				.<FrameSlotInfo>builder()
				.add(new FrameSlotInfo(EnumCheckboxOrientation.ABOVE, 0, 60, 30))
				.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 1, 95, 30))
				.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 2, 130, 30))
				.build();
	}

	@Override
	protected BaseItemTrafficLightFrame getValidFrameItem() {
		return ModItems.traffic_light_hoz_frame;
	}

	@Override
	protected int getYSize() {
		return 200;
	}
}
