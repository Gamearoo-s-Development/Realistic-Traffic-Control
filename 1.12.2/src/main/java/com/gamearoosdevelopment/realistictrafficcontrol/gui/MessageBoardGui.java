package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardTileEntity;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class MessageBoardGui extends GuiScreen {
	private final MessageBoardTileEntity board;
	private final GuiTextField[] fields = new GuiTextField[MessageBoardTileEntity.MAX_LINES];

	public MessageBoardGui(MessageBoardTileEntity board) {
		this.board = board;
	}

	@Override
	public void initGui() {
		for (int i = 0; i < fields.length; i++) {
			fields[i] = new GuiTextField(i, fontRenderer, width / 2 - 100, height / 2 - 40 + i * 24, 200, 20);
			fields[i].setMaxStringLength(MessageBoardTileEntity.MAX_LINE_LENGTH);
			fields[i].setText(board.getLine(i));
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRenderer, "Message Board", width / 2, height / 2 - 78, 0xFFFFA000);
		drawCenteredString(fontRenderer, "Use the controller or OpenComputers for remote control.", width / 2,
				height / 2 + 42, 0xAAAAAA);
		for (GuiTextField field : fields) {
			field.drawTextBox();
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		for (GuiTextField field : fields) {
			field.mouseClicked(mouseX, mouseY, mouseButton);
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		for (GuiTextField field : fields) {
			field.textboxKeyTyped(typedChar, keyCode);
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void onGuiClosed() {
		for (int i = 0; i < fields.length; i++) {
			board.setLine(i, fields[i].getText());
		}
		board.performClientToServerSync();
		super.onGuiClosed();
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
