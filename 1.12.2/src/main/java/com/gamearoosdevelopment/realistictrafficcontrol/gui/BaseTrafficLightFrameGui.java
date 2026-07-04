package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer.FrameSlotInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketHandler;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketTrafficLightFrameFacingUpdate;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketTrafficLightFrameGuiUpdate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiCheckBox;

public abstract class BaseTrafficLightFrameGui extends GuiContainer {

	private static final int ID_FACING_AUTO = 1000;
	private static final int ID_FACING_NORTH = 1001;
	private static final int ID_FACING_SOUTH = 1002;
	private static final int ID_FACING_EAST = 1003;
	private static final int ID_FACING_WEST = 1004;

	private GuiButtonExtSelectable facingAutoButton;
	private GuiButtonExtSelectable facingNorthButton;
	private GuiButtonExtSelectable facingSouthButton;
	private GuiButtonExtSelectable facingEastButton;
	private GuiButtonExtSelectable facingWestButton;

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

		int facingY = guiTop - 24;
		facingAutoButton = new GuiButtonExtSelectable(ID_FACING_AUTO, guiLeft + 44, facingY, 34, 16, "Auto");
		facingNorthButton = new GuiButtonExtSelectable(ID_FACING_NORTH, guiLeft + 82, facingY, 22, 16, "N");
		facingSouthButton = new GuiButtonExtSelectable(ID_FACING_SOUTH, guiLeft + 106, facingY, 22, 16, "S");
		facingEastButton = new GuiButtonExtSelectable(ID_FACING_EAST, guiLeft + 130, facingY, 22, 16, "E");
		facingWestButton = new GuiButtonExtSelectable(ID_FACING_WEST, guiLeft + 154, facingY, 22, 16, "W");
		buttonList.add(facingAutoButton);
		buttonList.add(facingNorthButton);
		buttonList.add(facingSouthButton);
		buttonList.add(facingEastButton);
		buttonList.add(facingWestButton);
		updateFacingButtonSelection();
	}

	private void updateFacingButtonSelection() {
		EnumFacing facing = ((BaseItemTrafficLightFrame) container.getFrameStack().getItem())
				.getConfiguredApproachFacing(container.getFrameStack());
		facingAutoButton.setIsSelected(facing == null);
		facingNorthButton.setIsSelected(facing == EnumFacing.NORTH);
		facingSouthButton.setIsSelected(facing == EnumFacing.SOUTH);
		facingEastButton.setIsSelected(facing == EnumFacing.EAST);
		facingWestButton.setIsSelected(facing == EnumFacing.WEST);
	}

	private void setApproachFacing(EnumFacing facing) {
		BaseItemTrafficLightFrame frameItem = (BaseItemTrafficLightFrame) container.getFrameStack().getItem();
		frameItem.setConfiguredApproachFacing(container.getFrameStack(), facing);
		PacketHandler.INSTANCE.sendToServer(new PacketTrafficLightFrameFacingUpdate(facing));
		updateFacingButtonSelection();
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
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRenderer.drawString("Facing:", 4, -20, 0xFFFFFF);
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
			case ID_FACING_AUTO:
				setApproachFacing(null);
				return;
			case ID_FACING_NORTH:
				setApproachFacing(EnumFacing.NORTH);
				return;
			case ID_FACING_SOUTH:
				setApproachFacing(EnumFacing.SOUTH);
				return;
			case ID_FACING_EAST:
				setApproachFacing(EnumFacing.EAST);
				return;
			case ID_FACING_WEST:
				setApproachFacing(EnumFacing.WEST);
				return;
			default:
				break;
		}

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
