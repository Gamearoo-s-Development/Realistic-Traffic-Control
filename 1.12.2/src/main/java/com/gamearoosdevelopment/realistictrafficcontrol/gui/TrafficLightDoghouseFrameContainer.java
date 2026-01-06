package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo.EnumCheckboxOrientation;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.google.common.collect.ImmutableList;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class TrafficLightDoghouseFrameContainer extends BaseTrafficLightFrameContainer {

	public TrafficLightDoghouseFrameContainer(InventoryPlayer player, ItemStack frameStack) {
		super(player, frameStack);
	}
	
	@Override
	protected List<FrameSlotInfo> buildSlotInfo() {
		return ImmutableList
			.<FrameSlotInfo>builder()
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 0, 79, 10))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.LEFT, 1, 48, 54))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.LEFT, 2, 48, 86))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 3, 110, 54))
			.add(new FrameSlotInfo(EnumCheckboxOrientation.RIGHT, 4, 110, 86))
			.build();
	}

	@Override
	protected int getYSize() {
		// TODO Auto-generated method stub
		return 210;
	}

	@Override
	protected BaseItemTrafficLightFrame getValidFrameItem() {
		// TODO Auto-generated method stub
		return ModItems.traffic_light_doghouse_frame;
	}

}
