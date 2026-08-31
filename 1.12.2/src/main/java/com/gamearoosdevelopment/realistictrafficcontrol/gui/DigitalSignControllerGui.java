package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;

import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignControllerTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class DigitalSignControllerGui extends GuiScreen {
	private final DigitalSignControllerTileEntity controller;
	private GuiImageList signs;
	private GuiButton addButton;
	private GuiButton previousPageButton;
	private GuiButton pageIndicator;
	private GuiButton nextPageButton;
	private GuiButton savePageButton;
	private GuiButton deletePageButton;
	private GuiButton scheduleButton;
	private GuiTextField intervalAmount;
	private GuiTextField signGameTime;
	private int editingPageIndex = -1;
	private java.util.UUID editingSign;

	public DigitalSignControllerGui(DigitalSignControllerTileEntity controller) {
		this.controller = controller;
	}

	@Override
	public void initGui() {
		signs = new GuiImageList(width - 128, 18, 112, height - 68, sign -> selectSign(sign.getID()));
		int center = (width - 128) / 2;
		int buttonWidth = Math.min(170, Math.max(90, width - 150));
		int pageCount = controller.getRotationSigns().size();
		editingPageIndex = pageCount == 0 ? -1
				: Math.max(0, Math.min(pageCount - 1, controller.getRotationIndex()));
		editingSign = editingPageIndex >= 0 ? controller.getRotationSigns().get(editingPageIndex)
				: controller.getSelectedSign();
		addButton = addButton(new GuiButton(30, center - buttonWidth / 2, 40, buttonWidth, 20, addLabel()));
		previousPageButton = addButton(new GuiButton(31, center - buttonWidth / 2, 64, 30, 20, "<"));
		pageIndicator = addButton(new GuiButton(32, center - 66, 64, 132, 20, pageLabel()));
		nextPageButton = addButton(new GuiButton(33, center + buttonWidth / 2 - 30, 64, 30, 20, ">"));
		savePageButton = addButton(new GuiButton(34, center - buttonWidth / 2, 88, buttonWidth / 2 - 2, 20, "Save page"));
		deletePageButton = addButton(new GuiButton(35, center + 2, 88, buttonWidth / 2 - 2, 20, "Delete page"));
		addButton(new GuiButton(36, center - buttonWidth / 2, 112, buttonWidth, 20, "Clear pages"));
		scheduleButton = addButton(new GuiButton(37, center - buttonWidth / 2, 136, buttonWidth, 20, scheduleLabel()));
		intervalAmount = new GuiTextField(40, fontRenderer, center - buttonWidth / 2, 170, buttonWidth, 18);
		intervalAmount.setMaxStringLength(7);
		intervalAmount.setText(String.valueOf(controller.getScheduleIntervalAmount()));
		signGameTime = new GuiTextField(41, fontRenderer, center - buttonWidth / 2, 200, buttonWidth, 18);
		signGameTime.setMaxStringLength(5);
		signGameTime.setText(controller.getRotationSignTimeText(editingSign));
		addButton(new GuiButton(38, center - buttonWidth / 2, 224, buttonWidth, 20, "Save selected time"));
		refreshPageButtons();
		refreshScheduleControls();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRenderer, "Digital Sign Controller", (width - 128) / 2, 8, 0xFFFF00);
		drawCenteredString(fontRenderer, "Linked signs: " + controller.getLinkedSigns().size(), (width - 128) / 2,
				height - 38, 0xFFFFFF);
		drawCenteredString(fontRenderer, "Use tuner on controller, then digital sign.", (width - 128) / 2,
				height - 25, 0xAAAAAA);
		drawCenteredString(fontRenderer, "Interval amount", (width - 128) / 2, 112, 0xAAAAAA);
		drawCenteredString(fontRenderer, "Selected sign time (HH:MM)", (width - 128) / 2, 190, 0xAAAAAA);
		if (intervalAmount != null) intervalAmount.drawTextBox();
		if (signGameTime != null) signGameTime.drawTextBox();
		if (editingSign != null) {
			Sign sign = com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl.instance.signRepo
					.getSignByID(editingSign);
			if (sign != null) {
				drawCenteredString(fontRenderer, "Selected: " + sign.getName(), (width - 128) / 2, 24, 0xFFFFA000);
			}
		}
		if (signs != null) {
			signs.draw(mouseX, mouseY, fontRenderer, text -> x -> y -> drawHoveringText(text, x, y));
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void onGuiClosed() {
		saveEditingPage();
		commitScheduleFields();
		controller.performClientToServerSync();
		super.onGuiClosed();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (intervalAmount != null) intervalAmount.mouseClicked(mouseX, mouseY, mouseButton);
		if (signGameTime != null) signGameTime.mouseClicked(mouseX, mouseY, mouseButton);
		if (signs != null) {
			signs.onMouseClick(mouseX, mouseY);
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (intervalAmount != null) intervalAmount.textboxKeyTyped(typedChar, keyCode);
		if (signGameTime != null) signGameTime.textboxKeyTyped(typedChar, keyCode);
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 30) {
			if (editingSign != null) {
				controller.addRotationSign(editingSign);
				editingPageIndex = controller.getRotationSigns().indexOf(editingSign);
				controller.selectRotationSign(editingPageIndex);
				commitSelectedSignTime();
			}
			addButton.displayString = addLabel();
			refreshPageButtons();
			controller.performClientToServerSync();
		} else if (button.id == 31) {
			saveEditingPage();
			if (selectEditingPage(editingPageIndex - 1)) loadPageFromController();
			refreshPageButtons();
			controller.performClientToServerSync();
		} else if (button.id == 33) {
			saveEditingPage();
			if (selectEditingPage(editingPageIndex + 1)) loadPageFromController();
			refreshPageButtons();
			controller.performClientToServerSync();
		} else if (button.id == 34) {
			saveEditingPage();
			refreshPageButtons();
			controller.performClientToServerSync();
		} else if (button.id == 35) {
			if (controller.removeRotationSign(editingPageIndex)) {
				editingPageIndex = controller.getRotationIndex();
				loadPageFromController();
			}
			refreshPageButtons();
			addButton.displayString = addLabel();
			controller.performClientToServerSync();
		} else if (button.id == 36) {
			controller.clearRotationSigns();
			editingPageIndex = -1;
			editingSign = controller.getSelectedSign();
			if (signGameTime != null) signGameTime.setText("");
			refreshPageButtons();
			addButton.displayString = addLabel();
			controller.performClientToServerSync();
		} else if (button.id == 37) {
			commitScheduleFields();
			DisplaySchedule.Mode next = controller.getScheduleMode().next();
			controller.setScheduleMode(next);
			refreshScheduleControls();
			controller.performClientToServerSync();
		} else if (button.id == 38) {
			commitScheduleFields();
			refreshScheduleControls();
			controller.performClientToServerSync();
		}
	}

	private String addLabel() {
		return "Add page (" + controller.getRotationSigns().size() + "/"
				+ DigitalSignControllerTileEntity.MAX_ROTATION_SIGNS + ")";
	}

	private String pageLabel() {
		int count = controller.getRotationSigns().size();
		return count == 0 ? "Page 0 / 0" : "Page " + (editingPageIndex + 1) + " / " + count;
	}

	private void refreshPageButtons() {
		int count = controller.getRotationSigns().size();
		boolean hasPages = count > 0;
		pageIndicator.displayString = pageLabel();
		pageIndicator.enabled = false;
		previousPageButton.enabled = hasPages;
		nextPageButton.enabled = hasPages;
		savePageButton.enabled = hasPages;
		deletePageButton.enabled = hasPages;
	}

	private boolean selectEditingPage(int index) {
		int count = controller.getRotationSigns().size();
		if (count == 0) return false;
		editingPageIndex = (index % count + count) % count;
		return controller.selectRotationSign(editingPageIndex);
	}

	private void loadPageFromController() {
		if (editingPageIndex < 0 || editingPageIndex >= controller.getRotationSigns().size()) {
			editingSign = controller.getSelectedSign();
		} else {
			editingSign = controller.getRotationSigns().get(editingPageIndex);
		}
		if (signGameTime != null) signGameTime.setText(controller.getRotationSignTimeText(editingSign));
	}

	private void saveEditingPage() {
		if (editingPageIndex >= 0 && editingSign != null) {
			if (controller.updateRotationSign(editingPageIndex, editingSign)) {
				commitSelectedSignTime();
			}
		}
	}

	private String scheduleLabel() {
		if (controller.getScheduleMode() == DisplaySchedule.Mode.GAME_TIMES) {
			return "Timing: Each sign's game time";
		}
		return "Timing: " + controller.getScheduleMode().getLabel(controller.getScheduleIntervalAmount());
	}

	private void refreshScheduleControls() {
		scheduleButton.displayString = scheduleLabel();
		intervalAmount.setEnabled(controller.getScheduleMode().isInterval());
		signGameTime.setEnabled(editingSign != null);
	}

	private void commitScheduleFields() {
		if (intervalAmount != null) {
			controller.setScheduleIntervalAmount(parsePositiveInt(intervalAmount.getText(), controller.getScheduleIntervalAmount()));
			intervalAmount.setText(String.valueOf(controller.getScheduleIntervalAmount()));
		}
		commitSelectedSignTime();
	}

	private void commitSelectedSignTime() {
		if (signGameTime == null || editingSign == null) return;
		if (controller.setRotationSignTime(editingSign, signGameTime.getText())) {
			signGameTime.setText(controller.getRotationSignTimeText(editingSign));
		}
	}

	private void selectSign(java.util.UUID id) {
		commitSelectedSignTime();
		editingSign = id;
		if (signGameTime != null) signGameTime.setText(controller.getRotationSignTimeText(id));
	}

	private int parsePositiveInt(String text, int fallback) {
		try {
			return Math.max(1, Integer.parseInt(text.trim()));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		if (signs != null) {
			signs.onMouseRelease();
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
