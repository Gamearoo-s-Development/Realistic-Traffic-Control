package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.RelayTileEntity;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CrossingRelaySettingsGui extends GuiScreen {

	private final World world;
	private final BlockPos clickedRelayPos;
	private GuiTextField bellStopSeconds;

	private static final int ID_DONE = 1;

	public CrossingRelaySettingsGui(World world, BlockPos clickedRelayPos) {
		this.world = world;
		this.clickedRelayPos = clickedRelayPos;
	}

	/** Master if multiblock is intact; otherwise the clicked relay tile (server handler still resolves master). */
	private RelayTileEntity relayForSettings() {
		if (world == null) {
			return null;
		}
		TileEntity t = world.getTileEntity(clickedRelayPos);
		if (!(t instanceof RelayTileEntity)) {
			return null;
		}
		RelayTileEntity atClick = (RelayTileEntity) t;
		RelayTileEntity master = atClick.getMaster(world);
		return master != null ? master : atClick;
	}

	@Override
	public void initGui() {
		super.initGui();
		int cx = width / 2;
		int cy = height / 2;
		bellStopSeconds = new GuiTextField(0, fontRenderer, cx - 40, cy, 80, 20);
		RelayTileEntity relay = relayForSettings();
		int initial = relay != null ? relay.getRelayBellStopAfterSeconds() : 0;
		bellStopSeconds.setText(String.valueOf(initial));
		buttonList.clear();
		buttonList.add(new GuiButton(ID_DONE, cx - 50, cy + 28, 100, 20, I18n.format("gui.done")));
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (bellStopSeconds != null) {
			bellStopSeconds.updateCursorCounter();
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == ID_DONE) {
			mc.displayGuiScreen(null);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		int cx = width / 2;
		int cy = height / 2;
		String title = I18n.format("trafficcontrol.gui.relay.title");
		fontRenderer.drawString(title, cx - fontRenderer.getStringWidth(title) / 2, cy - 36, 0xFFFFFF);
		String label = I18n.format("trafficcontrol.gui.relay.bellstop");
		fontRenderer.drawString(label, cx - fontRenderer.getStringWidth(label) / 2, cy - 18, 0xA0A0A0);
		bellStopSeconds.drawTextBox();
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);
		if (Character.isDigit(typedChar) || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK
				|| keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_HOME
				|| keyCode == Keyboard.KEY_END) {
			bellStopSeconds.textboxKeyTyped(typedChar, keyCode);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		bellStopSeconds.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void onGuiClosed() {
		int sec = 0;
		try {
			sec = Integer.parseInt(bellStopSeconds.getText().trim());
		} catch (NumberFormatException ex) {
			sec = 0;
		}
		if (sec < 0) {
			sec = 0;
		}
		if (sec > 3600) {
			sec = 3600;
		}
		RelayTileEntity relay = relayForSettings();
		if (relay != null) {
			relay.setRelayBellStopAfterSeconds(sec);
			relay.performClientToServerSync();
		}
	}
}