package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.SignImageListWidget;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.SignMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.network.UpdateSignPayload;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignHorizontalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignVerticalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;

/** Port of 1.12.2 {@code SignGui}. */
public class SignScreen extends AbstractContainerScreen<SignMenu> {

    private SignBlockEntity signEntity;
    private SignImageListWidget imageList;
    private EditBox searchBox;
    private Button textEditorButton;
    private boolean textEditMode;
    private int currentTextLine;

    public SignScreen(SignMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // This is a full-screen editor, not a vanilla inventory panel.
        imageWidth = 1;
        imageHeight = 1;
    }

    @Override
    protected void init() {
        super.init();
        signEntity = menu.getSign(minecraft.player);
        if (signEntity == null) {
            return;
        }

        int listX = width - 128;
        imageList = addRenderableWidget(
                new SignImageListWidget(listX, 18, 112, height - 68, this::onImageClicked));

        searchBox = new EditBox(font, listX, height - 40, 112, 20, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setResponder(imageList::filter);
        addRenderableWidget(searchBox);

        int leftPanelWidth = width - 134;
        int leftPanelHorizontalCenter = leftPanelWidth / 2;
        textEditorButton = Button.builder(Component.literal("Text Editor (T)"), b -> toggleTextEditor())
                .bounds(leftPanelHorizontalCenter - 100, 20, 200, 20).build();
        textEditorButton.active = signEntity.getSign() != null && !signEntity.getSign().getTextLines().isEmpty();
        addRenderableWidget(textEditorButton);
    }

    private void onImageClicked(Sign image) {
        signEntity.setTypeLegacy(SignBlockEntity.getSignTypeNumber(image.getType()));
        signEntity.setVariantLegacy(image.getVariant());
        signEntity.setID(image.getID());
        signEntity.clearTextLines();
        textEditorButton.active = !image.getTextLines().isEmpty();
        textEditMode = false;
    }

    private void toggleTextEditor() {
        if (signEntity.getSign() == null || signEntity.getSign().getTextLines().isEmpty()) {
            textEditMode = false;
            searchBox.setEditable(true);
            return;
        }
        textEditMode = !textEditMode;
        searchBox.setEditable(!textEditMode);
        textEditorButton.setMessage(Component.literal(textEditMode ? "Finish Editing" : "Text Editor (T)"));
        if (!textEditMode) {
            currentTextLine = 0;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (signEntity == null) {
            return;
        }
        Sign currentSign = signEntity.getSign();
        if (currentSign == null) {
            return;
        }

        int leftPanelWidth = width - 134;
        int leftPanelHorizontalCenter = leftPanelWidth / 2;
        graphics.drawCenteredString(font, "Name: " + currentSign.getName(), leftPanelHorizontalCenter, 43, 0xFFFF00);

        if (currentSign.getNote() != null && !currentSign.getNote().isEmpty()) {
            graphics.drawWordWrap(font, Component.literal("Note: " + currentSign.getNote()),
                    8, height - 35, Math.max(1, leftPanelWidth - 16), 0xFFFFFF);
        }

        int previewSize = Math.min(leftPanelWidth, height - 105);
        int previewX = leftPanelHorizontalCenter - previewSize / 2;
        // Sign-pack images are 16x16. Passing the scaled preview size as the
        // source texture size sampled only a corner of the image, producing the
        // mostly black/blank preview seen in the ported GUI.
        graphics.blit(currentSign.getFrontImageResourceLocation(), previewX, 53, previewSize, previewSize,
                0, 0, 16, 16, 16, 16);
        renderPreviewText(graphics, currentSign, previewX, 53, previewSize);
    }

    private void renderPreviewText(GuiGraphics graphics, Sign currentSign, int previewX, int previewY, int previewSize) {
        if (currentSign.getTextLines().isEmpty()) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(previewX, previewY, 100);
        double fullScale = ((double) previewSize / font.lineHeight) / 16;
        graphics.pose().scale((float) fullScale, (float) fullScale, 1);
        for (int i = 0; i < currentSign.getTextLines().size(); i++) {
            Sign.TextLine textLine = currentSign.getTextLines().get(i);
            graphics.pose().pushPose();
            graphics.pose().translate(textLine.getX() * font.lineHeight, textLine.getY() * font.lineHeight, 0);
            graphics.pose().scale((float) textLine.getXScale(), (float) textLine.getYScale(), 1);
            if (textLine.getvAlign() == SignVerticalAlignment.Center) {
                graphics.pose().translate(0, -font.lineHeight / 2F, 0);
            } else if (textLine.getvAlign() == SignVerticalAlignment.Bottom) {
                graphics.pose().translate(0, -font.lineHeight, 0);
            }
            double availableWidth = textLine.getScaleAdjustedWidth() * font.lineHeight;
            if (textLine.gethAlign() == SignHorizontalAlignment.Center) {
                graphics.pose().translate(-availableWidth / 2, 0, 0);
            } else if (textLine.gethAlign() == SignHorizontalAlignment.Right) {
                graphics.pose().translate(-availableWidth, 0, 0);
            }
            if (textEditMode) {
                int highlight = currentTextLine == i ? 0x6600FF00 : 0x66FF0000;
                graphics.fill(0, 0, (int) availableWidth, font.lineHeight, highlight);
                graphics.pose().pushPose();
                graphics.pose().scale(.5F, .5F, 1);
                graphics.drawString(font, textLine.getLabel(), 0, -font.lineHeight,
                        currentTextLine == i ? 0xFF00FF00 : 0xFFFF0000);
                graphics.pose().popPose();
            }
            String text = signEntity.getTextLine(i);
            if (text != null && !text.isEmpty()) {
                int textWidth = font.width(text);
                float widthScale = (float) Math.min(1, availableWidth / textWidth);
                graphics.pose().scale(widthScale, 1, 1);
                int textX = 0;
                if (textLine.gethAlign() == SignHorizontalAlignment.Center && widthScale == 1) {
                    textX = (int) (availableWidth / 2) - textWidth / 2;
                } else if (textLine.gethAlign() == SignHorizontalAlignment.Right) {
                    textX = (int) availableWidth - textWidth;
                }
                graphics.drawString(font, text, textX + 1, 1, textLine.getColor());
            }
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textEditMode && signEntity != null && signEntity.getSign() != null) {
            String currentText = signEntity.getTextLine(currentTextLine);
            if (currentText == null) {
                currentText = "";
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !currentText.isEmpty()) {
                signEntity.setTextLine(currentTextLine, currentText.substring(0, currentText.length() - 1));
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) {
                currentTextLine = (currentTextLine + 1) % signEntity.getSign().getTextLines().size();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UP) {
                int max = signEntity.getSign().getTextLines().size();
                currentTextLine = currentTextLine <= 0 ? max - 1 : currentTextLine - 1;
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                if (currentTextLine == signEntity.getSign().getTextLines().size() - 1) {
                    toggleTextEditor();
                } else {
                    currentTextLine++;
                }
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                toggleTextEditor();
                return true;
            }
        } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_T && !searchBox.isFocused()) {
            toggleTextEditor();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers) || searchBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (textEditMode && signEntity != null && signEntity.getSign() != null) {
            Sign.TextLine textLine = signEntity.getSign().getTextLines().get(currentTextLine);
            String currentText = signEntity.getTextLine(currentTextLine);
            if (currentText == null) {
                currentText = "";
            }
            if (currentText.length() < textLine.getMaxLength() && !Character.isISOControl(codePoint)) {
                signEntity.setTextLine(currentTextLine, currentText + codePoint);
                return true;
            }
        }
        return searchBox.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        if (signEntity != null) {
            ArrayList<String> lines = new ArrayList<>();
            Sign sign = signEntity.getSign();
            if (sign != null) {
                for (int i = 0; i < sign.getTextLines().size(); i++) {
                    lines.add(signEntity.getTextLine(i));
                }
            }
            PacketDistributor.sendToServer(new UpdateSignPayload(menu.getBlockPos(), signEntity.getTypeLegacy(),
                    signEntity.getVariantLegacy(), signEntity.getID(), lines));
        }
        super.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
    }
}
