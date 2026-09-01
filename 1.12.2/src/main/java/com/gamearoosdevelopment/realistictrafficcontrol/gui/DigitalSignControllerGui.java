package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignHorizontalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignVerticalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignControllerTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.client.config.GuiButtonExt;

public class DigitalSignControllerGui extends GuiScreen {
	private static final String SEARCH_PLACEHOLDER = "\u00a73\u00a7oSearch...";
	private static final int OPTIONS_WIDTH = 204;
	private static final int LIST_WIDTH = 128;

	private final DigitalSignControllerTileEntity controller;
	private GuiImageList signs;
	private GuiTextField search;
	private GuiButton addButton;
	private GuiButton previousPageButton;
	private GuiButton pageIndicator;
	private GuiButton nextPageButton;
	private GuiButton savePageButton;
	private GuiButton deletePageButton;
	private GuiButton scheduleButton;
	private GuiButton saveTimeButton;
	private GuiButtonExt textEditorButton;
	private GuiTextField intervalAmount;
	private GuiTextField signGameTime;
	private int editingPageIndex = -1;
	private UUID editingSign;
	private final ArrayList<String> editingTextLines = new ArrayList<>();
	private boolean textEditMode;
	private int currentTextLine;
	private int editPanelX;
	private int editPanelWidth;
	private int previewX;
	private int previewTop;
	private int previewSize;

	public DigitalSignControllerGui(DigitalSignControllerTileEntity controller) {
		this.controller = controller;
	}

	@Override
	public void initGui() {
		editPanelX = OPTIONS_WIDTH + 8;
		editPanelWidth = Math.max(160, width - OPTIONS_WIDTH - LIST_WIDTH - 16);
		int controlX = 8;
		int controlWidth = OPTIONS_WIDTH - 16;

		signs = new GuiImageList(width - LIST_WIDTH, 18, LIST_WIDTH - 16, height - 68, sign -> selectSign(sign.getID()));
		search = new GuiTextField(0, fontRenderer, width - LIST_WIDTH, height - 40, LIST_WIDTH - 16, 20);
		search.setMaxStringLength(64);
		search.setText(SEARCH_PLACEHOLDER);

		int pageCount = controller.getRotationPageCount();
		editingPageIndex = pageCount == 0 ? -1
				: Math.max(0, Math.min(pageCount - 1, controller.getRotationIndex()));
		editingSign = editingPageIndex >= 0 ? controller.getPageSignId(editingPageIndex)
				: controller.getSelectedSign();
		loadEditingTextLines();

		int y = 8;
		addButton = addButton(new GuiButton(30, controlX, y, controlWidth, 20, addLabel()));
		y += 22;
		previousPageButton = addButton(new GuiButton(31, controlX, y, 30, 20, "<"));
		pageIndicator = addButton(new GuiButton(32, controlX + 34, y, controlWidth - 68, 20, pageLabel()));
		nextPageButton = addButton(new GuiButton(33, controlX + controlWidth - 30, y, 30, 20, ">"));
		y += 22;
		savePageButton = addButton(new GuiButton(34, controlX, y, controlWidth / 2 - 2, 20, "Save page"));
		deletePageButton = addButton(new GuiButton(35, controlX + controlWidth / 2 + 2, y, controlWidth / 2 - 2, 20, "Delete"));
		y += 22;
		addButton(new GuiButton(36, controlX, y, controlWidth, 20, "Clear pages"));
		y += 22;
		scheduleButton = addButton(new GuiButton(37, controlX, y, controlWidth, 20, scheduleLabel()));
		y += 34;
		intervalAmount = new GuiTextField(40, fontRenderer, controlX, y, controlWidth, 18);
		intervalAmount.setMaxStringLength(7);
		intervalAmount.setText(String.valueOf(controller.getScheduleIntervalAmount()));
		y += 32;
		signGameTime = new GuiTextField(41, fontRenderer, controlX, y, controlWidth, 18);
		signGameTime.setMaxStringLength(5);
		signGameTime.setText(controller.getRotationPageTimeText(editingPageIndex));
		y += 22;
		saveTimeButton = addButton(new GuiButton(38, controlX, y, controlWidth, 20, "Save selected time"));

		textEditorButton = new GuiButtonExt(39, editPanelX + 8, 8, Math.min(180, editPanelWidth - 16), 20, "Text Editor (T)");
		buttonList.add(textEditorButton);
		refreshTextEditorButton();
		refreshPageButtons();
		refreshScheduleControls();
		updatePreviewBounds();
	}

	private void updatePreviewBounds() {
		previewTop = 34;
		previewSize = Math.min(editPanelWidth - 16, Math.max(96, height - 100));
		previewX = editPanelX + (editPanelWidth - previewSize) / 2;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		updatePreviewBounds();

		int footerY = height - 48;
		drawCenteredString(fontRenderer, "Digital Sign Controller", editPanelX + editPanelWidth / 2, footerY, 0xFFFF00);
		drawCenteredString(fontRenderer,
				"Signs: " + controller.getLinkedSigns().size()
						+ "  |  Synced: " + controller.getSyncedControllers().size()
						+ (controller.isSyncFollower() ? "  |  Following master" : ""),
				editPanelX + editPanelWidth / 2, footerY + 12, 0xFFFFFF);
		if (controller.isSyncFollower()) {
			drawCenteredString(fontRenderer, "Edit timing on controller 1 (the master).",
					editPanelX + editPanelWidth / 2, footerY + 24, 0xFF5555);
		} else {
			drawCenteredString(fontRenderer, "Tuner: controller->sign, or controller->controller to sync timing.",
					editPanelX + editPanelWidth / 2, footerY + 24, 0xAAAAAA);
		}

		if (controller.isSyncFollower()) {
			drawCenteredString(fontRenderer, "Timing locked to master", OPTIONS_WIDTH / 2, intervalAmount.y - 11, 0xFF5555);
			drawCenteredString(fontRenderer, "Edit timing on controller 1", OPTIONS_WIDTH / 2, signGameTime.y - 11, 0xFF5555);
		} else {
			drawCenteredString(fontRenderer, "Interval amount", OPTIONS_WIDTH / 2, intervalAmount.y - 11, 0xAAAAAA);
			drawCenteredString(fontRenderer, "Page time (HH:MM)", OPTIONS_WIDTH / 2, signGameTime.y - 11, 0xAAAAAA);
		}

		Sign preview = getEditingSignDefinition();
		if (preview != null) {
			drawCenteredString(fontRenderer, preview.getName(), editPanelX + editPanelWidth / 2, 30, 0xFFFFA000);
			drawSignPreview(preview);
		}

		if (intervalAmount != null) intervalAmount.drawTextBox();
		if (signGameTime != null) signGameTime.drawTextBox();
		if (search != null) search.drawTextBox();
		if (signs != null) {
			signs.draw(mouseX, mouseY, fontRenderer, text -> x -> y -> {
				drawHoveringText(text, x, y);
				GlStateManager.color(1F, 1F, 1F, 1F);
				GlStateManager.disableLighting();
			});
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private Sign getEditingSignDefinition() {
		return editingSign == null ? null
				: ModRealisticTrafficControl.instance.signRepo.getSignByID(editingSign);
	}

	private void drawSignPreview(Sign sign) {
		mc.renderEngine.bindTexture(sign.getFrontImageResourceLocation());
		GlStateManager.color(1F, 1F, 1F, 1F);
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		builder.pos(previewX, previewTop, 1).tex(0, 0).endVertex();
		builder.pos(previewX, previewTop + previewSize, 1).tex(0, 1).endVertex();
		builder.pos(previewX + previewSize, previewTop + previewSize, 1).tex(1, 1).endVertex();
		builder.pos(previewX + previewSize, previewTop, 1).tex(1, 0).endVertex();
		tess.draw();

		if (sign.getTextLines().isEmpty()) return;

		double fullScale = ((double) previewSize / fontRenderer.FONT_HEIGHT) / 16.0;
		double downScale = 1 / fullScale;
		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();
		GlStateManager.translate(previewX, previewTop, 2);
		GlStateManager.scale(fullScale, fullScale, 1);

		for (int i = 0; i < sign.getTextLines().size(); i++) {
			Sign.TextLine textLine = sign.getTextLines().get(i);
			String lineText = getEditingTextLine(i);

			GlStateManager.translate(textLine.getX() * fontRenderer.FONT_HEIGHT, textLine.getY() * fontRenderer.FONT_HEIGHT, 0);
			GlStateManager.scale(textLine.getXScale(), textLine.getYScale(), 1);
			if (textLine.getvAlign() == SignVerticalAlignment.Center) {
				GlStateManager.translate(0, -fontRenderer.FONT_HEIGHT / 2.0, 0);
			} else if (textLine.getvAlign() == SignVerticalAlignment.Bottom) {
				GlStateManager.translate(0, -fontRenderer.FONT_HEIGHT, 0);
			}
			if (textLine.gethAlign() == SignHorizontalAlignment.Center) {
				GlStateManager.translate(-(textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) / 2.0, 0, 0);
			} else if (textLine.gethAlign() == SignHorizontalAlignment.Right) {
				GlStateManager.translate(-textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT, 0, 0);
			}

			if (textEditMode) {
				int labelColor;
				if (currentTextLine == i) {
					GlStateManager.color(0F, 1F, 0F, 2F / 3F);
					labelColor = 0x00FF00;
				} else {
					GlStateManager.color(1F, 0F, 0F, 2F / 3F);
					labelColor = 0xFF0000;
				}
				GlStateManager.disableTexture2D();
				builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
				builder.pos(0, 0, 0).endVertex();
				builder.pos(0, fontRenderer.FONT_HEIGHT, 0).endVertex();
				builder.pos(textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT, fontRenderer.FONT_HEIGHT, 0).endVertex();
				builder.pos(textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT, 0, 0).endVertex();
				tess.draw();
				GlStateManager.enableTexture2D();
				GlStateManager.scale(0.5, 0.5, 1);
				fontRenderer.drawString(textLine.getLabel(), 0, -fontRenderer.FONT_HEIGHT, labelColor);
				GlStateManager.scale(2, 2, 1);
			}

			int textWidth = fontRenderer.getStringWidth(lineText);
			if (textWidth > 0) {
				double widthScaling = (textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) / textWidth;
				if (widthScaling > 1) widthScaling = 1;
				GlStateManager.scale(widthScaling, 1, 1);
				int textX = 0;
				if (textLine.gethAlign() == SignHorizontalAlignment.Center && widthScaling == 1) {
					textX = (int) ((textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) / 2) - (textWidth / 2);
				} else if (textLine.gethAlign() == SignHorizontalAlignment.Right) {
					textX = (int) (textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) - textWidth;
				}
				fontRenderer.drawString(lineText, textX + 1, 1, textLine.getColor());
				GlStateManager.scale(1 / widthScaling, 1, 1);
			}

			if (textLine.gethAlign() == SignHorizontalAlignment.Center) {
				GlStateManager.translate((textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) / 2.0, 0, 0);
			} else if (textLine.gethAlign() == SignHorizontalAlignment.Right) {
				GlStateManager.translate(textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT, 0, 0);
			}
			if (textLine.getvAlign() == SignVerticalAlignment.Center) {
				GlStateManager.translate(0, fontRenderer.FONT_HEIGHT / 2.0, 0);
			} else if (textLine.getvAlign() == SignVerticalAlignment.Bottom) {
				GlStateManager.translate(0, fontRenderer.FONT_HEIGHT, 0);
			}
			GlStateManager.scale(1 / textLine.getXScale(), 1 / textLine.getYScale(), 1);
			GlStateManager.translate(-textLine.getX() * fontRenderer.FONT_HEIGHT, -textLine.getY() * fontRenderer.FONT_HEIGHT, 0);
		}

		GlStateManager.scale(downScale, downScale, 1);
		GlStateManager.translate(-previewX, -previewTop, -2);
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private String getEditingTextLine(int index) {
		if (index < 0 || index >= editingTextLines.size()) return "";
		String value = editingTextLines.get(index);
		return value == null ? "" : value;
	}

	private void setEditingTextLine(int index, String text) {
		while (editingTextLines.size() <= index) editingTextLines.add("");
		editingTextLines.set(index, text == null ? "" : text);
	}

	private void loadEditingTextLines() {
		editingTextLines.clear();
		Sign sign = getEditingSignDefinition();
		if (sign == null) return;
		for (int i = 0; i < sign.getTextLines().size(); i++) {
			String value = editingPageIndex >= 0 ? controller.getPageTextLine(editingPageIndex, i) : "";
			editingTextLines.add(value == null ? "" : value);
		}
		if (currentTextLine >= editingTextLines.size()) currentTextLine = 0;
	}

	private void refreshTextEditorButton() {
		Sign sign = getEditingSignDefinition();
		boolean hasLines = sign != null && !sign.getTextLines().isEmpty();
		textEditorButton.enabled = hasLines;
		if (!hasLines) textEditMode = false;
		textEditorButton.displayString = textEditMode ? "Finish Editing" : "Text Editor (T)";
		if (search != null) search.setEnabled(!textEditMode);
	}

	private void toggleTextEditMode() {
		Sign sign = getEditingSignDefinition();
		if (sign == null || sign.getTextLines().isEmpty()) {
			textEditMode = false;
			refreshTextEditorButton();
			return;
		}
		textEditMode = !textEditMode;
		if (!textEditMode) currentTextLine = 0;
		refreshTextEditorButton();
	}

	@Override
	public void onGuiClosed() {
		saveEditingPage();
		if (!controller.isSyncFollower()) commitScheduleFields();
		controller.performClientToServerSync();
		super.onGuiClosed();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (!textEditMode) {
			if (!controller.isSyncFollower()) {
				if (intervalAmount != null) intervalAmount.mouseClicked(mouseX, mouseY, mouseButton);
				if (signGameTime != null) signGameTime.mouseClicked(mouseX, mouseY, mouseButton);
			}
			if (search != null) {
				if (search.mouseClicked(mouseX, mouseY, mouseButton) && SEARCH_PLACEHOLDER.equals(search.getText())) {
					search.setText("");
				} else if (search.getText().isEmpty()) {
					search.setText(SEARCH_PLACEHOLDER);
				}
			}
			if (signs != null) signs.onMouseClick(mouseX, mouseY);
		} else if (trySelectTextLineAt(mouseX, mouseY)) {
			// Selected a text region on the preview.
		}
	}

	/** Click a highlighted text region on the preview to select that line. */
	private boolean trySelectTextLineAt(int mouseX, int mouseY) {
		Sign sign = getEditingSignDefinition();
		if (sign == null || sign.getTextLines().isEmpty()) return false;
		if (mouseX < previewX || mouseX > previewX + previewSize
				|| mouseY < previewTop || mouseY > previewTop + previewSize) {
			return false;
		}

		double fullScale = ((double) previewSize / fontRenderer.FONT_HEIGHT) / 16.0;
		double localX = (mouseX - previewX) / fullScale;
		double localY = (mouseY - previewTop) / fullScale;

		for (int i = 0; i < sign.getTextLines().size(); i++) {
			Sign.TextLine textLine = sign.getTextLines().get(i);
			double lineX = textLine.getX() * fontRenderer.FONT_HEIGHT;
			double lineY = textLine.getY() * fontRenderer.FONT_HEIGHT;
			double width = textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT * textLine.getXScale();
			double height = fontRenderer.FONT_HEIGHT * textLine.getYScale();

			if (textLine.getvAlign() == SignVerticalAlignment.Center) lineY -= height / 2.0;
			else if (textLine.getvAlign() == SignVerticalAlignment.Bottom) lineY -= height;
			if (textLine.gethAlign() == SignHorizontalAlignment.Center) lineX -= width / 2.0;
			else if (textLine.gethAlign() == SignHorizontalAlignment.Right) lineX -= width;

			if (localX >= lineX && localX <= lineX + width && localY >= lineY && localY <= lineY + height) {
				currentTextLine = i;
				return true;
			}
		}
		return false;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (textEditMode) {
			handleTextEditKey(typedChar, keyCode);
			return;
		}
		if (keyCode == Keyboard.KEY_T && !search.isFocused()
				&& (intervalAmount == null || !intervalAmount.isFocused())
				&& (signGameTime == null || !signGameTime.isFocused())) {
			textEditorButton.playPressSound(Minecraft.getMinecraft().getSoundHandler());
			toggleTextEditMode();
			return;
		}
		if (!controller.isSyncFollower()) {
			if (intervalAmount != null) intervalAmount.textboxKeyTyped(typedChar, keyCode);
			if (signGameTime != null) signGameTime.textboxKeyTyped(typedChar, keyCode);
		}
		if (search != null && search.textboxKeyTyped(typedChar, keyCode)) {
			String filter = SEARCH_PLACEHOLDER.equals(search.getText()) ? null : search.getText();
			if (signs != null) signs.filter(filter);
		}
		super.keyTyped(typedChar, keyCode);
	}

	private void handleTextEditKey(char typedChar, int keyCode) {
		Sign sign = getEditingSignDefinition();
		if (sign == null || sign.getTextLines().isEmpty()) {
			textEditMode = false;
			refreshTextEditorButton();
			return;
		}
		if (currentTextLine < 0 || currentTextLine >= sign.getTextLines().size()) currentTextLine = 0;

		String currentText = getEditingTextLine(currentTextLine);
		if (keyCode == Keyboard.KEY_BACK && !currentText.isEmpty()) {
			setEditingTextLine(currentTextLine, currentText.substring(0, currentText.length() - 1));
		} else if (keyCode == Keyboard.KEY_DOWN) {
			currentTextLine = (currentTextLine + 1) % sign.getTextLines().size();
		} else if (keyCode == Keyboard.KEY_UP) {
			currentTextLine = currentTextLine <= 0 ? sign.getTextLines().size() - 1 : currentTextLine - 1;
		} else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
			if (currentTextLine == sign.getTextLines().size() - 1) toggleTextEditMode();
			else currentTextLine++;
		} else if (keyCode == Keyboard.KEY_ESCAPE) {
			toggleTextEditMode();
		} else if (keyCode != Keyboard.KEY_BACK) {
			Sign.TextLine textLine = sign.getTextLines().get(currentTextLine);
			if (textLine.getMaxLength() == currentText.length() || typedChar == 0
					|| (Character.isWhitespace(typedChar) && keyCode != Keyboard.KEY_SPACE)) {
				return;
			}
			setEditingTextLine(currentTextLine, currentText + typedChar);
		}
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		if (!textEditMode && signs != null) signs.scroll(Integer.signum(Mouse.getDWheel()));
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == textEditorButton) {
			toggleTextEditMode();
			return;
		}
		if (textEditMode) return;

		if (button.id == 30) {
			if (editingSign != null) {
				saveEditingPage();
				controller.addRotationSign(editingSign);
				editingPageIndex = controller.getRotationPageCount() - 1;
				saveEditingPage();
			}
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
			controller.performClientToServerSync();
		} else if (button.id == 36) {
			controller.clearRotationSigns();
			editingPageIndex = -1;
			editingSign = controller.getSelectedSign();
			if (signGameTime != null) signGameTime.setText("");
			loadEditingTextLines();
			refreshTextEditorButton();
			refreshPageButtons();
			controller.performClientToServerSync();
		} else if (button.id == 37) {
			if (controller.isSyncFollower()) return;
			commitScheduleFields();
			controller.setScheduleMode(controller.getScheduleMode().next());
			refreshScheduleControls();
			controller.performClientToServerSync();
		} else if (button.id == 38) {
			if (controller.isSyncFollower()) return;
			commitScheduleFields();
			refreshScheduleControls();
			controller.performClientToServerSync();
		}
	}

	private String addLabel() {
		return "Add page (" + controller.getRotationPageCount() + "/"
				+ DigitalSignControllerTileEntity.MAX_ROTATION_SIGNS + ")";
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
		previousPageButton.enabled = hasPages && !textEditMode;
		nextPageButton.enabled = hasPages && !textEditMode;
		savePageButton.enabled = hasPages && !textEditMode;
		deletePageButton.enabled = hasPages && !textEditMode;
		addButton.displayString = addLabel();
		addButton.enabled = !textEditMode;
	}

	private boolean selectEditingPage(int index) {
		int count = controller.getRotationPageCount();
		if (count == 0) return false;
		editingPageIndex = (index % count + count) % count;
		return controller.selectRotationSign(editingPageIndex);
	}

	private void loadPageFromController() {
		if (editingPageIndex < 0 || editingPageIndex >= controller.getRotationPageCount()) {
			editingSign = controller.getSelectedSign();
		} else {
			editingSign = controller.getPageSignId(editingPageIndex);
		}
		if (signGameTime != null) signGameTime.setText(controller.getRotationPageTimeText(editingPageIndex));
		loadEditingTextLines();
		refreshTextEditorButton();
	}

	private void saveEditingPage() {
		if (editingSign == null) return;
		if (editingPageIndex >= 0 && editingPageIndex < controller.getRotationPageCount()) {
			controller.saveRotationPage(editingPageIndex, editingSign, new ArrayList<>(editingTextLines));
			if (!controller.isSyncFollower() && signGameTime != null) {
				controller.setRotationPageTime(editingPageIndex, signGameTime.getText());
			}
		}
	}

	private String scheduleLabel() {
		if (controller.isSyncFollower()) {
			return "Timing: Locked to master";
		}
		if (controller.getScheduleMode() == DisplaySchedule.Mode.GAME_TIMES) {
			return "Timing: Each sign's game time";
		}
		return "Timing: " + controller.getScheduleMode().getLabel(controller.getScheduleIntervalAmount());
	}

	private void refreshScheduleControls() {
		boolean follower = controller.isSyncFollower();
		scheduleButton.displayString = scheduleLabel();
		scheduleButton.enabled = !follower && !textEditMode;
		intervalAmount.setEnabled(!follower && controller.getScheduleMode().isInterval() && !textEditMode);
		signGameTime.setEnabled(!follower && editingPageIndex >= 0 && !textEditMode);
		if (saveTimeButton != null) saveTimeButton.enabled = !follower && !textEditMode;
	}

	private void commitScheduleFields() {
		if (controller.isSyncFollower()) return;
		if (intervalAmount != null) {
			controller.setScheduleIntervalAmount(parsePositiveInt(intervalAmount.getText(),
					controller.getScheduleIntervalAmount()));
			intervalAmount.setText(String.valueOf(controller.getScheduleIntervalAmount()));
		}
		if (signGameTime != null && editingPageIndex >= 0) {
			controller.setRotationPageTime(editingPageIndex, signGameTime.getText());
			signGameTime.setText(controller.getRotationPageTimeText(editingPageIndex));
		}
	}

	private void selectSign(UUID id) {
		saveEditingPage();
		editingSign = id;
		textEditMode = false;
		currentTextLine = 0;
		if (editingPageIndex >= 0) {
			controller.updateRotationSign(editingPageIndex, id);
			loadEditingTextLines();
		} else {
			editingTextLines.clear();
			Sign sign = getEditingSignDefinition();
			if (sign != null) {
				for (int i = 0; i < sign.getTextLines().size(); i++) editingTextLines.add("");
			}
		}
		if (signGameTime != null) signGameTime.setText(controller.getRotationPageTimeText(editingPageIndex));
		refreshTextEditorButton();
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
		if (signs != null) signs.onMouseRelease();
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
