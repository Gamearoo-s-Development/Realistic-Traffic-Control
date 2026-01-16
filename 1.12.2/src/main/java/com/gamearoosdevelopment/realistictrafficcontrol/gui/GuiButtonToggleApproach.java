package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.EnumFacing;

public class GuiButtonToggleApproach extends GuiButton {
	private final EnumFacing facing;
	private boolean toggled;

	public GuiButtonToggleApproach(int id, int x, int y, int width, int height, EnumFacing facing, boolean initialState) {
		super(id, x, y, width, height, "");
		this.facing = facing;
		this.toggled = initialState;
	}

	public void toggle() {
		this.toggled = !this.toggled;
	}

	public boolean isToggled() {
		return this.toggled;
	}

	public void setToggled(boolean toggled) {
		this.toggled = toggled;
	}

	public EnumFacing getFacing() {
		return facing;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!this.visible) {
			return;
		}

		FontRenderer fr = mc.fontRenderer;
		this.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, this.toggled ? 0xFF00AA00 : 0xFFAA0000);

		String shortDir;
		switch (facing) {
			case NORTH:
				shortDir = "N";
				break;
			case SOUTH:
				shortDir = "S";
				break;
			case EAST:
				shortDir = "E";
				break;
			case WEST:
				shortDir = "W";
				break;
			default:
				shortDir = "?";
				break;
		}

		String label = shortDir + ": " + (toggled ? "ON" : "OFF");
		fr.drawString(label, this.x + this.width + 4, this.y + 6, 0xFFFFFF);
	}
}
