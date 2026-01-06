package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketHandler;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketTrafficLightFrameGuiUpdate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiCheckBox;

public abstract class BaseTrafficLightFrameGui extends GuiContainer {

	ItemStack frameStack;
	BaseTrafficLightFrameContainer container;
	public BaseTrafficLightFrameGui(BaseTrafficLightFrameContainer container)
	{
		super(container);
		this.container = container;
	}
	
	@Override
	public void initGui() {
		super.initGui();
		ItemStack frameStack = container.getFrameStack();
		BaseItemTrafficLightFrame frameItem = (BaseItemTrafficLightFrame)frameStack.getItem();
		
		int left = (width / 2) - (xSize / 2);
		int top = (height / 2) - (ySize / 2);
		for (int slotIndex = 0; slotIndex < container.getFrameSlotInfos().size(); slotIndex++)
		{
			FrameSlotInfo slotInfo = container.getFrameSlotInfos().get(slotIndex);
			SlotItemHandlerListenable primarySlot = slotInfo.getPrimarySlot();
			int x = left + primarySlot.xPos;
			int y = top + primarySlot.yPos;
			GuiCheckBox allowFlash = new GuiCheckBox(slotIndex * 10, 0, 0, "Allow Flash", true);
			allowFlash.setIsChecked(frameItem.getAlwaysFlash(frameStack, slotInfo.getSlotIndex()));
			attachSlotListener(slotInfo.getPrimaryContainerSlotIndex(), slotIndex);
			if (slotInfo.hasSecondarySlot())
			{
				attachSlotListener(slotInfo.getSecondaryContainerSlotIndex(), slotIndex);
			}
			switch(slotInfo.getCheckboxOrientation())
			{
				case ABOVE:
					y -= 24;
					break;
				case BELOW:
					y += 28;
					break;
				case LEFT:
					x -= allowFlash.getButtonWidth() + 12;
					break;
				case RIGHT:
					int offset = slotInfo.hasSecondarySlot() ? 52 : 30;
					x += offset;
					break;
			}
			allowFlash.x = x;
			allowFlash.y = y;
			buttonList.add(allowFlash);
			updateCheckboxVisibility(slotIndex);
		}
	}
	
	private void updateCheckboxVisibility(int slotIndex)
	{
		GuiCheckBox box = findCheckboxById(slotIndex * 10);
		if (box == null)
		{
			return;
		}
		FrameSlotInfo slotInfo = container.getFrameSlotInfos().get(slotIndex);
		boolean primaryHasStack = container.getSlot(slotInfo.getPrimaryContainerSlotIndex()).getHasStack();
		boolean secondaryHasStack = slotInfo.hasSecondarySlot() && container.getSlot(slotInfo.getSecondaryContainerSlotIndex()).getHasStack();
		box.visible = primaryHasStack || secondaryHasStack;
	}

	private void attachSlotListener(int containerSlotIndex, int slotGroupIndex)
	{
		Slot slot = container.getSlot(containerSlotIndex);
		if (slot instanceof SlotItemHandlerListenable)
		{
			((SlotItemHandlerListenable)slot).setOnSlotChangedListener(ind -> updateCheckboxVisibility(slotGroupIndex));
		}
	}
	
	private GuiCheckBox findCheckboxById(int id)
	{
		for(GuiButton button : buttonList)
		{
			if (button instanceof GuiCheckBox && button.id == id)
			{
				return (GuiCheckBox)button;
			}
		}
		
		return null;
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/gui/" + getGuiPngName()));
		drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, xSize, ySize);
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (!(button instanceof GuiCheckBox))
		{
			return;
		}
		
		GuiCheckBox checkbox = (GuiCheckBox)button;
		
		int type = button.id % 10;
		int slotId = button.id / 10;
		
		switch(type)
		{
			case 0: // Allow Flash
				BaseItemTrafficLightFrame baseFrameItem = (BaseItemTrafficLightFrame)container.getFrameStack().getItem();
				baseFrameItem.handleGuiAlwaysUpdate(container.getFrameStack(), slotId, checkbox.isChecked());
				
				PacketTrafficLightFrameGuiUpdate packet = new PacketTrafficLightFrameGuiUpdate();
				packet.slotId = slotId;
				packet.alwaysFlash = checkbox.isChecked();
				PacketHandler.INSTANCE.sendToServer(packet);
				break;
		}
	}
	
	protected abstract String getGuiPngName();
}
