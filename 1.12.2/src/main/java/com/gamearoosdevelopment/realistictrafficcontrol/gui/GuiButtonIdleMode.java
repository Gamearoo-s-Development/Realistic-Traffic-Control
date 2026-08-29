package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import com.gamearoosdevelopment.realistictrafficcontrol.util.IdleBulbMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class GuiButtonIdleMode extends GuiButton {
	private final String movementLabel;
	private final boolean straightOnly;
	private IdleBulbMode mode;

	private boolean movementEnabled;
	private boolean dimmed;

	public GuiButtonIdleMode(int id, int x, int y, int width, int height, String movementLabel, IdleBulbMode initialMode, boolean straightOnly) {
		super(id, x, y, width, height, "");
		this.movementLabel = movementLabel;
		this.straightOnly = straightOnly;
		this.mode = straightOnly ? initialMode.normalizeForStraight() : initialMode;
	}

	public void setMovementEnabled(boolean movementEnabled) {
		this.movementEnabled = movementEnabled;
		this.enabled = !movementEnabled;
		this.dimmed = movementEnabled;
	}

	public boolean isMovementEnabled() {
		return movementEnabled;
	}

	public void cycle() {
		if (movementEnabled) {
			return;
		}
		mode = straightOnly ? mode.nextForStraight() : mode.next();
	}

	public IdleBulbMode getMode() {
		return mode;
	}

	public void setMode(IdleBulbMode mode) {
		this.mode = straightOnly ? mode.normalizeForStraight() : mode;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}
		FontRenderer fr = mc.fontRenderer;
		int color;
		if (dimmed) {
			color = 0xFF555555;
		} else {
			switch (mode) {
				case ARROW_GREEN:
				case SOLID_GREEN:
					color = 0xFF00AA00;
					break;
				case ARROW_YELLOW:
				case SOLID_YELLOW:
					color = 0xFFAAAA00;
					break;
				case ARROW_RED:
				case SOLID_RED:
				default:
					color = 0xFFAA0000;
					break;
			}
		}
		drawRect(x, y, x + width, y + height, color);
		String label = movementEnabled
				? movementLabel + ": ON"
				: movementLabel + ": " + mode.getShortLabel();
		fr.drawString(label, x + 4, y + 6, dimmed ? 0x888888 : 0xFFFFFF);
	}
}
