package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.SignImageListWidget;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.Type3BarrierMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignHorizontalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignVerticalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.Type3BarrierBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/** Port of 1.12.2 {@code GuiType3Barrier}. */
public class Type3BarrierScreen extends AbstractContainerScreen<Type3BarrierMenu> {

    private static final ResourceLocation ROAD_CLOSED_SIGN = ResourceLocation.fromNamespaceAndPath(
            ModRealisticTrafficControl.MODID, "textures/block/road_closed_sign.png");

    private Type3BarrierBlockEntity barrierEntity;
    private Checkbox renderSignBox;
    private Checkbox renderThisSignBox;
    private Button prevSignType;
    private Button nextSignType;
    private Button selectThisSign;
    private Button textEditorButton;
    private SignImageListWidget imageList;
    private EditBox imageListFilter;
    private boolean imageListVisible;
    private boolean textLineEditorActive;
    private int currentTextLine;

    public Type3BarrierScreen(Type3BarrierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 1;
        imageHeight = 1;
    }

    @Override
    protected void init() {
        super.init();
        barrierEntity = menu.getBarrier(minecraft.player);
        if (barrierEntity == null) {
            return;
        }

        renderSignBox = Checkbox.builder(Component.literal("Sign across barriers"), font)
                .pos(0, height / 2 - 48)
                .selected(barrierEntity.getRenderSign())
                .onValueChange((cb, checked) -> {
                    Type3BarrierBlockEntity owner = barrierEntity.findFurthestLeft();
                    owner.setRenderSign(checked);
                    owner.syncConnectedBarriers(true);
                    updateSignTypeVisibility();
                }).build();
        renderSignBox.setX(width / 2 - renderSignBox.getWidth() - 4);
        addRenderableWidget(renderSignBox);

        prevSignType = Button.builder(Component.literal("<"), b -> {
            Type3BarrierBlockEntity owner = barrierEntity.findFurthestLeft();
            owner.prevSignType();
            owner.syncConnectedBarriers(true);
        }).bounds(width / 2 + 4, height / 2 - 52, 20, 20).build();
        nextSignType = Button.builder(Component.literal(">"), b -> {
            Type3BarrierBlockEntity owner = barrierEntity.findFurthestLeft();
            owner.nextSignType();
            owner.syncConnectedBarriers(true);
        }).bounds(width / 2 + 92, height / 2 - 52, 20, 20).build();
        addRenderableWidget(prevSignType);
        addRenderableWidget(nextSignType);

        renderThisSignBox = Checkbox.builder(Component.literal("Sign on this barrier"), font)
                .pos(0, height / 2 + 8)
                .selected(barrierEntity.getRenderThisSign())
                .onValueChange((cb, checked) -> {
                    barrierEntity.setRenderThisSign(checked);
                    barrierEntity.performClientToServerSync();
                    textEditorButton.active = false;
                    updateThisSignVisibility();
                }).build();
        renderThisSignBox.setX(width / 2 - renderThisSignBox.getWidth() - 4);
        addRenderableWidget(renderThisSignBox);

        selectThisSign = Button.builder(Component.literal("Select Sign"), b -> {
            openImageList();
        }).bounds(width / 2 + 40, height / 2 - 6, 75, 20).build();
        addRenderableWidget(selectThisSign);

        textEditorButton = Button.builder(Component.literal("Text Editor"), b -> toggleTextEditor())
                .bounds(width / 2 + 40, height / 2 + 18, 75, 20).build();
        textEditorButton.active = barrierEntity.getThisSign() != null
                && !barrierEntity.getThisSign().getTextLines().isEmpty();
        addRenderableWidget(textEditorButton);

        imageList = addRenderableWidget(new SignImageListWidget(
                width / 2 - 96, height / 2 - 100, 200, 200, sign -> {
            barrierEntity.setThisSignTypeLegacy(SignBlockEntity.getSignTypeNumber(sign.getType()));
            barrierEntity.setThisSignVariantLegacy(sign.getVariant());
            barrierEntity.setThisSignID(sign.getID());
            barrierEntity.clearThisSignTextLines();
            barrierEntity.performClientToServerSync();
            textEditorButton.active = !sign.getTextLines().isEmpty();
            closeImageList();
        }));
        imageList.setVisible(false);

        imageListFilter = new EditBox(font, width / 2 - 96, height / 2 + 104, 200, 20, Component.literal("Filter"));
        imageListFilter.setHint(Component.literal("Filter..."));
        addRenderableWidget(imageListFilter);
        imageListFilter.setResponder(imageList::filter);
        imageListFilter.visible = false;

        updateSignTypeVisibility();
        updateThisSignVisibility();
    }

    private boolean hasModalOpen() {
        return imageListVisible || textLineEditorActive;
    }

    private void openImageList() {
        imageListVisible = true;
        imageList.setVisible(true);
        imageListFilter.visible = true;
        imageListFilter.setFocused(true);
        updateModalVisibility();
    }

    private void closeImageList() {
        imageListVisible = false;
        imageList.setVisible(false);
        imageList.filter(null);
        imageListFilter.visible = false;
        imageListFilter.setFocused(false);
        updateModalVisibility();
    }

    private void updateSignTypeVisibility() {
        boolean visible = !hasModalOpen() && renderSignBox.selected();
        prevSignType.visible = visible;
        nextSignType.visible = visible;
    }

    private void updateThisSignVisibility() {
        boolean visible = !hasModalOpen() && renderThisSignBox.selected();
        selectThisSign.visible = visible;
        textEditorButton.visible = visible;
    }

    private void updateModalVisibility() {
        boolean showBase = !hasModalOpen();
        renderSignBox.visible = showBase;
        renderThisSignBox.visible = showBase;
        imageList.setVisible(imageListVisible);
        imageListFilter.visible = imageListVisible;
        updateSignTypeVisibility();
        updateThisSignVisibility();
    }

    private void toggleTextEditor() {
        Sign sign = barrierEntity.getThisSign();
        if (sign == null || sign.getTextLines().isEmpty()) {
            textLineEditorActive = false;
            return;
        }
        textLineEditorActive = !textLineEditorActive;
        if (!textLineEditorActive) {
            currentTextLine = 0;
            barrierEntity.performClientToServerSync();
        } else if (currentTextLine >= sign.getTextLines().size()) {
            currentTextLine = 0;
        }
        updateModalVisibility();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        if (barrierEntity == null) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        if (!hasModalOpen()) {
            if (renderSignBox.selected()) {
                renderSpanningSignPreview(graphics, width / 2 + 28, height / 2 - 58);
            }
            if (renderThisSignBox.selected()) {
                renderThisSignPreview(graphics, width / 2 + 4, height / 2, 32, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (imageListVisible) {
            graphics.drawCenteredString(font, "Select Sign", width / 2 + 4, height / 2 - 114, 0xFFFFFFFF);
        } else if (textLineEditorActive) {
            Sign sign = barrierEntity.getThisSign();
            if (sign != null) {
                int previewSize = Math.max(16, Math.min(width - 100, height - 100));
                renderThisSignPreview(graphics, width / 2 - previewSize / 2,
                        height / 2 - previewSize / 2, previewSize, true);
            }
        }
    }

    private void renderSpanningSignPreview(GuiGraphics graphics, int left, int top) {
        int sourceY = 0;
        int sourceHeight = 32;
        if (barrierEntity.getSignType() == Type3BarrierBlockEntity.SignType.LaneClosed) {
            sourceY = 32;
        } else if (barrierEntity.getSignType() == Type3BarrierBlockEntity.SignType.RoadClosedThruTraffic) {
            sourceY = 64;
            sourceHeight = 64;
        }
        graphics.blit(ROAD_CLOSED_SIGN, left, top, 64, 32,
                0, sourceY, 64, sourceHeight, 128, 128);
    }

    private void renderThisSignPreview(GuiGraphics graphics, int left, int top, int size, boolean editor) {
        Sign sign = barrierEntity.getThisSign();
        if (sign == null) {
            return;
        }
        // Sign-pack images are 16x16; destination dimensions must not be used
        // as source dimensions or the image repeats/only samples one corner.
        graphics.blit(sign.getFrontImageResourceLocation(), left, top, size, size,
                0, 0, 16, 16, 16, 16);
        renderSignText(graphics, sign, left, top, size, editor);
    }

    private void renderSignText(GuiGraphics graphics, Sign sign, int left, int top, int size, boolean editor) {
        if (sign.getTextLines().isEmpty()) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 100);
        float fullScale = (float) (((double) size / font.lineHeight) / 16D);
        graphics.pose().scale(fullScale, fullScale, 1);
        for (int i = 0; i < sign.getTextLines().size(); i++) {
            Sign.TextLine textLine = sign.getTextLines().get(i);
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
                graphics.pose().translate(-availableWidth / 2D, 0, 0);
            } else if (textLine.gethAlign() == SignHorizontalAlignment.Right) {
                graphics.pose().translate(-availableWidth, 0, 0);
            }

            if (editor) {
                graphics.fill(0, 0, (int) availableWidth, font.lineHeight,
                        currentTextLine == i ? 0xAA00FF00 : 0xAAFF0000);
                graphics.pose().pushPose();
                graphics.pose().scale(.5F, .5F, 1);
                graphics.drawString(font, textLine.getLabel(), 0, -font.lineHeight,
                        currentTextLine == i ? 0xFF00FF00 : 0xFFFF0000);
                graphics.pose().popPose();
            }

            String text = barrierEntity.getThisSignTextLine(i);
            int textWidth = text == null ? 0 : font.width(text);
            if (textWidth > 0) {
                float widthScale = (float) Math.min(1D, availableWidth / textWidth);
                graphics.pose().scale(widthScale, 1, 1);
                int textX = 0;
                if (textLine.gethAlign() == SignHorizontalAlignment.Center && widthScale == 1F) {
                    textX = (int) (availableWidth / 2D) - textWidth / 2;
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
        if (imageListVisible) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeImageList();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (textLineEditorActive && barrierEntity != null && barrierEntity.getThisSign() != null
                && !barrierEntity.getThisSign().getTextLines().isEmpty()) {
            Sign sign = barrierEntity.getThisSign();
            if (currentTextLine < 0 || currentTextLine >= sign.getTextLines().size()) {
                currentTextLine = 0;
            }
            String currentText = barrierEntity.getThisSignTextLine(currentTextLine);
            if (currentText == null) {
                currentText = "";
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                toggleTextEditor();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!currentText.isEmpty()) {
                    barrierEntity.setThisSignTextLine(currentTextLine,
                            currentText.substring(0, currentText.length() - 1));
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                currentTextLine = (currentTextLine + 1) % sign.getTextLines().size();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                currentTextLine = currentTextLine <= 0 ? sign.getTextLines().size() - 1 : currentTextLine - 1;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (currentTextLine == sign.getTextLines().size() - 1) {
                    toggleTextEditor();
                } else {
                    currentTextLine++;
                }
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (imageListVisible) {
            return super.charTyped(codePoint, modifiers);
        }
        if (textLineEditorActive && barrierEntity != null && barrierEntity.getThisSign() != null) {
            Sign sign = barrierEntity.getThisSign();
            if (currentTextLine >= 0 && currentTextLine < sign.getTextLines().size()) {
                String currentText = barrierEntity.getThisSignTextLine(currentTextLine);
                if (currentText == null) {
                    currentText = "";
                }
                Sign.TextLine line = sign.getTextLines().get(currentTextLine);
                if (currentText.length() < line.getMaxLength() && !Character.isISOControl(codePoint)) {
                    barrierEntity.setThisSignTextLine(currentTextLine, currentText + codePoint);
                }
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (textLineEditorActive) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (textLineEditorActive) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (textLineEditorActive) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (textLineEditorActive) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (barrierEntity != null) {
            barrierEntity.syncConnectedBarriers(true);
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
