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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiCheckBox;

public abstract class BaseTrafficLightFrameGui extends GuiContainer {

	private static final int MIN_GUI_WIDTH = 180;
	private static final int FACING_ROW_HEIGHT = 16;

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
	private int facingLabelY = 6;

	ItemStack frameStack;
	BaseTrafficLightFrameContainer container;
	public BaseTrafficLightFrameGui(BaseTrafficLightFrameContainer container)
	{
		super(container);
		this.container = container;
	}
	
	@Override
	public void initGui() {
		if (xSize < MIN_GUI_WIDTH) {
			xSize = MIN_GUI_WIDTH;
		}
		super.initGui();
		container.refreshFrameStackFromPlayer(Minecraft.getMinecraft().player);
		ItemStack frameStack = container.getFrameStack();
		BaseItemTrafficLightFrame frameItem = (BaseItemTrafficLightFrame)frameStack.getItem();
		int minFlashY = Integer.MAX_VALUE;
		
		for (int slotIndex = 0; slotIndex < container.getFrameSlotInfos().size(); slotIndex++)
		{
			FrameSlotInfo slotInfo = container.getFrameSlotInfos().get(slotIndex);
			SlotItemHandlerListenable primarySlot = slotInfo.getPrimarySlot();
			int x = guiLeft + primarySlot.xPos;
			int y = guiTop + primarySlot.yPos;
			GuiCheckBox allowFlash = new GuiCheckBox(slotIndex * 10, 0, 0, "Flash", true);
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
			minFlashY = Math.min(minFlashY, y);
			buttonList.add(allowFlash);
			updateCheckboxVisibility(slotIndex);
		}

		int minFrameSlotY = Integer.MAX_VALUE;
		for (FrameSlotInfo slotInfo : container.getFrameSlotInfos()) {
			minFrameSlotY = Math.min(minFrameSlotY, slotInfo.getPrimaryY());
		}
		if (minFrameSlotY == Integer.MAX_VALUE) {
			minFrameSlotY = 24;
		}
		if (minFlashY != Integer.MAX_VALUE) {
			minFrameSlotY = Math.min(minFrameSlotY, minFlashY);
		}
		int facingY = guiTop + Math.max(-10, minFrameSlotY - FACING_ROW_HEIGHT - 6);
		facingLabelY = facingY - guiTop + 4;
		facingAutoButton = new GuiButtonExtSelectable(ID_FACING_AUTO, guiLeft + 42, facingY, 32, FACING_ROW_HEIGHT, "Auto");
		facingNorthButton = new GuiButtonExtSelectable(ID_FACING_NORTH, guiLeft + 76, facingY, 18, FACING_ROW_HEIGHT, "N");
		facingSouthButton = new GuiButtonExtSelectable(ID_FACING_SOUTH, guiLeft + 96, facingY, 18, FACING_ROW_HEIGHT, "S");
		facingEastButton = new GuiButtonExtSelectable(ID_FACING_EAST, guiLeft + 116, facingY, 18, FACING_ROW_HEIGHT, "E");
		facingWestButton = new GuiButtonExtSelectable(ID_FACING_WEST, guiLeft + 136, facingY, 18, FACING_ROW_HEIGHT, "W");
		buttonList.add(facingAutoButton);
		buttonList.add(facingNorthButton);
		buttonList.add(facingSouthButton);
		buttonList.add(facingEastButton);
		buttonList.add(facingWestButton);
		updateFacingButtonSelection();
	}

	private void updateFacingButtonSelection() {
		container.refreshFrameStackFromPlayer(Minecraft.getMinecraft().player);
		EnumFacing facing = ((BaseItemTrafficLightFrame) container.getFrameStack().getItem())
				.getConfiguredApproachFacing(container.getFrameStack());
		facingAutoButton.setIsSelected(facing == null);
		facingNorthButton.setIsSelected(facing == EnumFacing.NORTH);
		facingSouthButton.setIsSelected(facing == EnumFacing.SOUTH);
		facingEastButton.setIsSelected(facing == EnumFacing.EAST);
		facingWestButton.setIsSelected(facing == EnumFacing.WEST);
	}

	private void setApproachFacing(EnumFacing facing) {
		container.refreshFrameStackFromPlayer(Minecraft.getMinecraft().player);
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
		fontRenderer.drawString("Facing:", 4, facingLabelY, 0xFFFFFF);
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
