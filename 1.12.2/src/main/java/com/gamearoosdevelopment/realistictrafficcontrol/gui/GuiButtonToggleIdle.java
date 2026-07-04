package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import com.gamearoosdevelopment.realistictrafficcontrol.util.IdleBulbState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class GuiButtonToggleIdle extends GuiButton {
	private final String movementLabel;
	private IdleBulbState state;

	public GuiButtonToggleIdle(int id, int x, int y, int width, int height, String movementLabel, IdleBulbState initialState) {
		super(id, x, y, width, height, "");
		this.movementLabel = movementLabel;
		this.state = initialState;
	}

	public void toggle() {
		state = state == IdleBulbState.RED ? IdleBulbState.GREEN : IdleBulbState.RED;
	}

	public IdleBulbState getState() {
		return state;
	}

	public void setState(IdleBulbState state) {
		this.state = state;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}
		FontRenderer fr = mc.fontRenderer;
		boolean isGreen = state == IdleBulbState.GREEN;
		drawRect(x, y, x + width, y + height, isGreen ? 0xFF00AA00 : 0xFFAA0000);
		String label = movementLabel + ": " + (isGreen ? "Green" : "Red");
		fr.drawString(label, x + 4, y + 6, 0xFFFFFF);
	}
}
