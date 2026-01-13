package com.gamearoosdevelopment.realistictrafficcontrol.item;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.GuiProxy;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class ItemTrafficLightHozFrame extends BaseItemTrafficLightFrame {

	public ItemTrafficLightHozFrame()
	{
		super("traffic_light_hoz_frame");
	}
	
	@Override
	protected int getGuiID() {
		return GuiProxy.GUI_IDs.TRAFFIC_LIGHT_HOZ_FRAME;
	}

	@Override
	public int getBulbCount() {
		return 3;
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		addFrameIdentityTooltip(tooltip);
		IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		ItemStack subStack = handler.getStackInSlot(0);
		if (subStack != ItemStack.EMPTY)
		{
			tooltip.add("Top: " + subStack.getItem().getItemStackDisplayName(subStack));
		}
		subStack = handler.getStackInSlot(1);
		if (subStack != ItemStack.EMPTY)
		{
			tooltip.add("Middle: " + subStack.getItem().getItemStackDisplayName(subStack));
		}
		
		subStack = handler.getStackInSlot(2);
		if (subStack != ItemStack.EMPTY)
		{
			tooltip.add("Bottom: " + subStack.getItem().getItemStackDisplayName(subStack));
		}
	}

	@Override
	protected BlockBaseTrafficLight getBaseBlockTrafficLight() {
		return ModBlocks.traffic_light_hoz;
	}
	
}
