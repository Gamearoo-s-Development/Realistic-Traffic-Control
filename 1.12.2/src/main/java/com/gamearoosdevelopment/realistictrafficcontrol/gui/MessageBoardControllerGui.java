package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardControllerTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import org.lwjgl.opengl.GL11;

public class MessageBoardControllerGui extends GuiScreen {
	private final MessageBoardControllerTileEntity controller;
	private final GuiTextField[] fields = new GuiTextField[MessageBoardTileEntity.MAX_LINES];
	private GuiButton modeButton;
	private GuiButton addPageButton;
	private GuiButton previousPageButton;
	private GuiButton pageIndicator;
	private GuiButton nextPageButton;
	private GuiButton savePageButton;
	private GuiButton deletePageButton;
	private GuiButton scheduleButton;
	private GuiButton fontButton;
	private GuiButton textSizeIndicator;
	private GuiTextField intervalAmount;
	private GuiTextField gameTimes;
	private int startY;
	private int controlX;
	private int previewX;
	private int previewWidth;
	private int editingPageIndex = -1;
	private MessageBoardTileEntity.DisplayMode editingMode;
	private MessageBoardTileEntity.FontStyle editingFontStyle;
	private float editingTextScale;
	private int editingColor;
	private float editingBrightness;

	public MessageBoardControllerGui(MessageBoardControllerTileEntity controller) {
		this.controller = controller;
	}

	@Override
	public void initGui() {
		startY = Math.max(16, height / 2 - 120);
		previewWidth = Math.min(200, Math.max(96, width - 216));
		int totalWidth = 208 + previewWidth;
		controlX = Math.max(4, (width - totalWidth) / 2);
		previewX = controlX + 208;
		for (int i = 0; i < fields.length; i++) {
			fields[i] = new GuiTextField(i, fontRenderer, controlX, startY + i * 20, 200, 18);
			fields[i].setMaxStringLength(MessageBoardTileEntity.MAX_LINE_LENGTH);
			fields[i].setText(controller.getLine(i));
		}
		editingPageIndex = controller.getRotationPageCount() == 0 ? -1 : controller.getRotationIndex();
		capturePreviewState();
		modeButton = addButton(new GuiButton(20, controlX, startY + 60, 200, 20, modeLabel()));
		previousPageButton = addButton(new GuiButton(21, controlX, startY + 82, 30, 20, "<"));
		pageIndicator = addButton(new GuiButton(22, controlX + 34, startY + 82, 132, 20, pageLabel()));
		nextPageButton = addButton(new GuiButton(23, controlX + 170, startY + 82, 30, 20, ">"));
		addPageButton = addButton(new GuiButton(24, controlX, startY + 104, 64, 20, "Add"));
		savePageButton = addButton(new GuiButton(25, controlX + 68, startY + 104, 64, 20, "Save page"));
		deletePageButton = addButton(new GuiButton(26, controlX + 136, startY + 104, 64, 20, "Delete"));
		scheduleButton = addButton(new GuiButton(27, controlX, startY + 126, 200, 20, scheduleLabel()));
		fontButton = addButton(new GuiButton(28, previewX, startY + 90, previewWidth, 20, fontLabel()));
		addButton(new GuiButton(29, previewX, startY + 112, 28, 20, "-"));
		textSizeIndicator = addButton(new GuiButton(30, previewX + 32, startY + 112,
				Math.max(32, previewWidth - 64), 20, textSizeLabel()));
		addButton(new GuiButton(31, previewX + previewWidth - 28, startY + 112, 28, 20, "+"));
		textSizeIndicator.enabled = false;
		intervalAmount = new GuiTextField(40, fontRenderer, controlX, startY + 159, 200, 18);
		intervalAmount.setMaxStringLength(7);
		intervalAmount.setText(String.valueOf(controller.getScheduleIntervalAmount()));
		gameTimes = new GuiTextField(41, fontRenderer, controlX, startY + 190, 200, 18);
		gameTimes.setMaxStringLength(128);
		gameTimes.setText(controller.getScheduleTimesText());
		refreshPageButtons();
		refreshScheduleControls();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRenderer, "Message / Arrow Board Controller", width / 2, 5, 0xFFFFA000);
		drawCenteredString(fontRenderer, "Linked boards: " + controller.getLinkedBoards().size(), width / 2,
				startY + 211, 0xFFFFFF);
		drawCenteredString(fontRenderer, "Use tuner on controller, then board. OpenComputers can also control it.", width / 2,
				startY + 223, 0xAAAAAA);
		drawCenteredString(fontRenderer, "Interval amount", controlX + 100, startY + 149, 0xAAAAAA);
		drawCenteredString(fontRenderer, "Game times (HH:MM, comma-separated)", controlX + 100, startY + 180, 0xAAAAAA);
		drawPreview();
		for (GuiTextField field : fields) field.drawTextBox();
		if (intervalAmount != null) intervalAmount.drawTextBox();
		if (gameTimes != null) gameTimes.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private String modeLabel() { return "Display: " + editingMode.name().replace('_', ' '); }
	private String fontLabel() { return "Font: " + editingFontStyle.getLabel(); }
	private String textSizeLabel() { return "Text size: " + Math.round(editingTextScale * 100.0F) + "%"; }

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 20) {
			MessageBoardTileEntity.DisplayMode[] modes = MessageBoardTileEntity.DisplayMode.values();
			editingMode = modes[(editingMode.ordinal() + 1) % modes.length];
			controller.setMode(editingMode);
			modeButton.displayString = modeLabel();
		} else if (button.id == 21) {
			saveSelectedPage();
			if (selectEditingPage(editingPageIndex - 1)) loadPageFromController();
			refreshPageButtons();
			controller.performClientToServerSync();
		} else if (button.id == 23) {
			saveSelectedPage();
			if (selectEditingPage(editingPageIndex + 1)) loadPageFromController();
			refreshPageButtons();
			controller.performClientToServerSync();
		} else if (button.id == 24) {
			saveFieldsToController();
			controller.addCurrentPage();
			editingPageIndex = controller.getRotationIndex();
			capturePreviewState();
			refreshPageButtons();
			controller.performClientToServerSync();
		} else if (button.id == 25) {
			saveSelectedPage();
			commitScheduleFields();
			refreshPageButtons();
			refreshScheduleControls();
			controller.performClientToServerSync();
		} else if (button.id == 26) {
			if (controller.removeCurrentPage()) {
				editingPageIndex = controller.getRotationIndex();
				loadPageFromController();
			}
			refreshPageButtons();
			controller.performClientToServerSync();
		} else if (button.id == 27) {
			commitScheduleFields();
			DisplaySchedule.Mode next = controller.getScheduleMode().next();
			controller.setScheduleMode(next);
			refreshScheduleControls();
			controller.performClientToServerSync();
		} else if (button.id == 28) {
			editingFontStyle = editingFontStyle.next();
			controller.setFontStyle(editingFontStyle);
			refreshTextControls();
		} else if (button.id == 29) {
			setTextScale(editingTextScale - 0.1F);
		} else if (button.id == 31) {
			setTextScale(editingTextScale + 0.1F);
		}
	}

	private String pageLabel() {
		int count = controller.getRotationPageCount();
		return count == 0 ? "Page 0 / 0" : "Page " + (editingPageIndex + 1) + " / " + count;
	}

	private void refreshPageButtons() {
		int count = controller.getRotationPageCount();
		boolean hasPages = count > 0;
		pageIndicator.displayString = pageLabel();
		pageIndicator.enabled = false;
		previousPageButton.enabled = hasPages;
		nextPageButton.enabled = hasPages;
		savePageButton.enabled = hasPages;
		deletePageButton.enabled = hasPages;
		addPageButton.enabled = count < MessageBoardControllerTileEntity.MAX_ROTATION_PAGES;
	}

	private void loadPageFromController() {
		for (int i = 0; i < fields.length; i++) fields[i].setText(controller.getLine(i));
		capturePreviewState();
		modeButton.displayString = modeLabel();
		refreshTextControls();
	}

	private void setTextScale(float value) {
		editingTextScale = Math.max(0.5F, Math.min(1.5F, Math.round(value * 10.0F) / 10.0F));
		controller.setTextScale(editingTextScale);
		refreshTextControls();
	}

	private void refreshTextControls() {
		fontButton.displayString = fontLabel();
		textSizeIndicator.displayString = textSizeLabel();
	}

	private String scheduleLabel() {
		return "Timing: " + controller.getScheduleMode().getLabel(controller.getScheduleIntervalAmount());
	}

	private void refreshScheduleControls() {
		scheduleButton.displayString = scheduleLabel();
		intervalAmount.setEnabled(controller.getScheduleMode().isInterval());
		gameTimes.setEnabled(controller.getScheduleMode() == DisplaySchedule.Mode.GAME_TIMES);
	}

	private void drawPreview() {
		int areaTop = startY;
		int areaBottom = startY + 86;
		drawRect(previewX, areaTop, previewX + previewWidth, areaBottom, 0xFF181A1C);

		double pixelsPerWorld = Math.min((previewWidth - 10) / 2.30, (areaBottom - areaTop - 10) / 1.06);
		int screenWidth = Math.max(20, (int) Math.round(2.30 * pixelsPerWorld));
		int screenHeight = Math.max(12, (int) Math.round(1.06 * pixelsPerWorld));
		int left = previewX + (previewWidth - screenWidth) / 2;
		int top = areaTop + (areaBottom - areaTop - screenHeight) / 2;
		int right = left + screenWidth;
		int bottom = top + screenHeight;
		drawRect(left - 3, top - 3, right + 3, bottom + 3, 0xFF303336);
		drawRect(left, top, right, bottom, 0xFF050606);

		int color = previewColor();
		MessageBoardTileEntity.DisplayMode mode = editingMode;
		if (mode == MessageBoardTileEntity.DisplayMode.TEXT) {
			drawPreviewText(left, top, right, bottom, pixelsPerWorld, color);
		} else if (mode == MessageBoardTileEntity.DisplayMode.ARROW_LEFT
				|| mode == MessageBoardTileEntity.DisplayMode.ARROW_RIGHT) {
			drawPreviewArrow(left, top, right, bottom, mode == MessageBoardTileEntity.DisplayMode.ARROW_LEFT, color);
		} else if (mode == MessageBoardTileEntity.DisplayMode.CAUTION) {
			drawPreviewCaution(left, top, right, bottom, color);
		}
	}

	private void drawPreviewText(int left, int top, int right, int bottom, double pixelsPerWorld, int color) {
		int scaleX = (int) Math.round(left * mc.displayWidth / (double) width);
		int scaleY = (int) Math.round((height - bottom) * mc.displayHeight / (double) height);
		int scissorWidth = Math.max(1, (int) Math.round((right - left) * mc.displayWidth / (double) width));
		int scissorHeight = Math.max(1, (int) Math.round((bottom - top) * mc.displayHeight / (double) height));
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(scaleX, scaleY, scissorWidth, scissorHeight);

		float scale = (float) (pixelsPerWorld * editingTextScale / 92.0);
		float baseline = (float) (top + 0.14 * pixelsPerWorld);
		GlStateManager.pushMatrix();
		GlStateManager.translate((left + right) / 2.0F, baseline, 0);
		GlStateManager.scale(scale, scale, 1);
		for (int i = 0; i < fields.length; i++) {
			String line = editingFontStyle.apply(fields[i].getText());
			fontRenderer.drawString(line, -fontRenderer.getStringWidth(line) / 2, i * 22, color);
		}
		GlStateManager.popMatrix();
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}

	private void drawPreviewArrow(int left, int top, int right, int bottom, boolean pointsLeft, int color) {
		int centerY = (top + bottom) / 2;
		int headBase = pointsLeft ? left + (right - left) * 2 / 5 : right - (right - left) * 2 / 5;
		int tip = pointsLeft ? left + 5 : right - 5;
		int shaftStart = pointsLeft ? headBase : left + 6;
		int shaftEnd = pointsLeft ? right - 6 : headBase;
		drawRect(Math.min(shaftStart, shaftEnd), centerY - 3, Math.max(shaftStart, shaftEnd), centerY + 4, 0xFF000000 | color);
		drawGuiTriangle(tip, centerY, headBase, top + 5, headBase, bottom - 5, color);
	}

	private void drawPreviewCaution(int left, int top, int right, int bottom, int color) {
		String caution = "CAUTION";
		float scale = Math.min(1.0F, (right - left - 30.0F) / Math.max(1, fontRenderer.getStringWidth(caution)));
		GlStateManager.pushMatrix();
		GlStateManager.translate((left + right) / 2.0F, (top + bottom) / 2.0F - fontRenderer.FONT_HEIGHT * scale / 2.0F, 0);
		GlStateManager.scale(scale, scale, 1);
		fontRenderer.drawString(caution, -fontRenderer.getStringWidth(caution) / 2, 0, color);
		GlStateManager.popMatrix();
		drawGuiDiamond(left + 7, top + 7, color);
		drawGuiDiamond(right - 7, top + 7, color);
		drawGuiDiamond(left + 7, bottom - 7, color);
		drawGuiDiamond(right - 7, bottom - 7, color);
	}

	private void drawGuiDiamond(int cx, int cy, int color) {
		drawGuiTriangle(cx, cy - 4, cx + 4, cy, cx - 4, cy, color);
		drawGuiTriangle(cx, cy + 4, cx - 4, cy, cx + 4, cy, color);
	}

	private void drawGuiTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int color) {
		GlStateManager.disableTexture2D();
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.getBuffer();
		builder.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
		builder.pos(x1, y1, 0).color((color >> 16) & 255, (color >> 8) & 255, color & 255, 255).endVertex();
		builder.pos(x2, y2, 0).color((color >> 16) & 255, (color >> 8) & 255, color & 255, 255).endVertex();
		builder.pos(x3, y3, 0).color((color >> 16) & 255, (color >> 8) & 255, color & 255, 255).endVertex();
		tess.draw();
		GlStateManager.enableTexture2D();
	}

	private int previewColor() {
		int color = editingColor & 0xFFFFFF;
		float brightness = editingBrightness;
		int red = (int) (((color >> 16) & 255) * brightness);
		int green = (int) (((color >> 8) & 255) * brightness);
		int blue = (int) ((color & 255) * brightness);
		return (red << 16) | (green << 8) | blue;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		for (GuiTextField field : fields) field.mouseClicked(mouseX, mouseY, mouseButton);
		if (intervalAmount != null) intervalAmount.mouseClicked(mouseX, mouseY, mouseButton);
		if (gameTimes != null) gameTimes.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		for (GuiTextField field : fields) field.textboxKeyTyped(typedChar, keyCode);
		if (intervalAmount != null) intervalAmount.textboxKeyTyped(typedChar, keyCode);
		if (gameTimes != null) gameTimes.textboxKeyTyped(typedChar, keyCode);
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void onGuiClosed() {
		saveSelectedPage();
		commitScheduleFields();
		controller.performClientToServerSync();
		super.onGuiClosed();
	}

	private void saveFieldsToController() {
		for (int i = 0; i < fields.length; i++) controller.setText(i, fields[i].getText());
	}

	private void saveSelectedPage() {
		saveFieldsToController();
		controller.setMode(editingMode);
		controller.setBrightness(editingBrightness);
		controller.setTextScale(editingTextScale);
		controller.setFontStyle(editingFontStyle);
		controller.setColor(editingColor);
		if (editingPageIndex >= 0) controller.updateRotationPage(editingPageIndex);
	}

	private boolean selectEditingPage(int index) {
		int count = controller.getRotationPageCount();
		if (count == 0) return false;
		editingPageIndex = (index % count + count) % count;
		return controller.selectRotationPage(editingPageIndex);
	}

	private void capturePreviewState() {
		editingMode = controller.getMode();
		editingFontStyle = controller.getFontStyle();
		editingTextScale = controller.getTextScale();
		editingColor = controller.getColor();
		editingBrightness = controller.getBrightness();
	}

	private void commitScheduleFields() {
		if (intervalAmount != null) {
			controller.setScheduleIntervalAmount(parsePositiveInt(intervalAmount.getText(), controller.getScheduleIntervalAmount()));
			intervalAmount.setText(String.valueOf(controller.getScheduleIntervalAmount()));
		}
		if (gameTimes != null) controller.setScheduleTimes(gameTimes.getText());
	}

	private int parsePositiveInt(String text, int fallback) {
		try {
			return Math.max(1, Integer.parseInt(text.trim()));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
