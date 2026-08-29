package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import com.gamearoosdevelopment.realistictrafficcontrol.util.FyaMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class GuiButtonFyaMode extends GuiButton {
	private final String movementLabel;
	private FyaMode mode;

	public GuiButtonFyaMode(int id, int x, int y, int width, int height, String movementLabel, FyaMode initialMode) {
		super(id, x, y, width, height, "");
		this.movementLabel = movementLabel;
		this.mode = initialMode;
	}

	public void cycle() {
		mode = mode.next();
	}

	public FyaMode getMode() {
		return mode;
	}

	public void setMode(FyaMode mode) {
		this.mode = mode;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}
		FontRenderer fr = mc.fontRenderer;
		int color;
		switch (mode) {
			case OFF:
				color = 0xFFAA0000;
				break;
			case NIGHT_ONLY:
				color = 0xFFAA8800;
				break;
			case ALWAYS:
			default:
				color = 0xFF00AA00;
				break;
		}
		drawRect(x, y, x + width, y + height, color);
		String label = movementLabel + ": " + mode.getShortLabel();
		fr.drawString(label, x + 4, y + 6, 0xFFFFFF);
	}
}
