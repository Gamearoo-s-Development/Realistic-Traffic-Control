package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class GuiButtonToggleMovement extends GuiButton {
	private final String movementLabel;
	private boolean toggled;

	public GuiButtonToggleMovement(int id, int x, int y, int width, int height, String movementLabel, boolean initialState) {
		super(id, x, y, width, height, "");
		this.movementLabel = movementLabel;
		this.toggled = initialState;
	}

	public void toggle() {
		this.toggled = !this.toggled;
	}

	public boolean isToggled() {
		return toggled;
	}

	public void setToggled(boolean toggled) {
		this.toggled = toggled;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}
		FontRenderer fr = mc.fontRenderer;
		drawRect(x, y, x + width, y + height, toggled ? 0xFF00AA00 : 0xFFAA0000);
		String label = movementLabel + ": " + (toggled ? "ON" : "OFF");
		fr.drawString(label, x + 4, y + 6, 0xFFFFFF);
	}
}
