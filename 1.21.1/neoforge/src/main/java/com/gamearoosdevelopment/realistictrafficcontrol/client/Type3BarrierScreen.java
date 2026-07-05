package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.SignImageListWidget;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.Type3BarrierMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.Type3BarrierBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Port of 1.12.2 {@code GuiType3Barrier}. */
public class Type3BarrierScreen extends AbstractContainerScreen<Type3BarrierMenu> {

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
    }

    @Override
    protected void init() {
        super.init();
        barrierEntity = menu.getBarrier(minecraft.player);
        if (barrierEntity == null) {
            return;
        }

        renderSignBox = Checkbox.builder(Component.literal("Sign across barriers"), font)
                .pos(width / 2 - 104, height / 2 - 48)
                .selected(barrierEntity.getRenderSign())
                .onValueChange((cb, checked) -> {
                    barrierEntity.setRenderSign(checked);
                    updateSignTypeVisibility();
                }).build();
        addRenderableWidget(renderSignBox);

        prevSignType = Button.builder(Component.literal("<"), b -> {
            barrierEntity.prevSignType();
        }).bounds(width / 2 + 4, height / 2 - 52, 20, 20).build();
        nextSignType = Button.builder(Component.literal(">"), b -> {
            barrierEntity.nextSignType();
        }).bounds(width / 2 + 92, height / 2 - 52, 20, 20).build();
        addRenderableWidget(prevSignType);
        addRenderableWidget(nextSignType);

        renderThisSignBox = Checkbox.builder(Component.literal("Sign on this barrier"), font)
                .pos(width / 2 - 104, height / 2 + 8)
                .selected(barrierEntity.getRenderThisSign())
                .onValueChange((cb, checked) -> {
                    barrierEntity.setRenderThisSign(checked);
                    updateThisSignVisibility();
                }).build();
        addRenderableWidget(renderThisSignBox);

        selectThisSign = Button.builder(Component.literal("Select Sign"), b -> {
            imageListVisible = !imageListVisible;
            imageList.setVisible(imageListVisible);
        }).bounds(width / 2 + 40, height / 2 - 6, 75, 20).build();
        addRenderableWidget(selectThisSign);

        textEditorButton = Button.builder(Component.literal("Text Editor"), b -> toggleTextEditor())
                .bounds(width / 2 + 40, height / 2 + 18, 75, 20).build();
        textEditorButton.active = barrierEntity.getThisSign() != null
                && !barrierEntity.getThisSign().getTextLines().isEmpty();
        addRenderableWidget(textEditorButton);

        imageList = new SignImageListWidget(width / 2 - 96, height / 2 - 100, 200, 200, sign -> {
            barrierEntity.setThisSignTypeLegacy(SignBlockEntity.getSignTypeNumber(sign.getType()));
            barrierEntity.setThisSignVariantLegacy(sign.getVariant());
            barrierEntity.setThisSignID(sign.getID());
            barrierEntity.clearThisSignTextLines();
            textEditorButton.active = !sign.getTextLines().isEmpty();
            imageListVisible = false;
            imageList.setVisible(false);
        });
        imageList.setVisible(false);
        addWidget(imageList);

        imageListFilter = new EditBox(font, width / 2 - 96, height / 2 + 104, 200, 20, Component.literal("Filter"));
        addRenderableWidget(imageListFilter);
        imageListFilter.setResponder(imageList::filter);

        updateSignTypeVisibility();
        updateThisSignVisibility();
    }

    private void updateSignTypeVisibility() {
        boolean visible = renderSignBox.selected();
        prevSignType.visible = visible;
        nextSignType.visible = visible;
    }

    private void updateThisSignVisibility() {
        boolean visible = renderThisSignBox.selected();
        selectThisSign.visible = visible;
        textEditorButton.visible = visible;
    }

    private void toggleTextEditor() {
        textLineEditorActive = !textLineEditorActive;
        textEditorButton.setMessage(Component.literal(textLineEditorActive ? "Finish Editing" : "Text Editor"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (barrierEntity != null && renderThisSignBox.selected()) {
            Sign sign = barrierEntity.getThisSign();
            if (sign != null) {
                int x = width / 2 - 32;
                int y = height / 2 - 32;
                graphics.blit(sign.getFrontImageResourceLocation(), x, y, 0, 0, 64, 64, 64, 64);
            }
        }
        if (imageListVisible) {
            imageList.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textLineEditorActive && barrierEntity != null && barrierEntity.getThisSign() != null) {
            String currentText = barrierEntity.getThisSignTextLine(currentTextLine);
            if (currentText == null) {
                currentText = "";
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !currentText.isEmpty()) {
                barrierEntity.setThisSignTextLine(currentTextLine, currentText.substring(0, currentText.length() - 1));
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (textLineEditorActive && barrierEntity != null && barrierEntity.getThisSign() != null) {
            Sign sign = barrierEntity.getThisSign();
            if (currentTextLine < sign.getTextLines().size()) {
                String currentText = barrierEntity.getThisSignTextLine(currentTextLine);
                if (currentText == null) {
                    currentText = "";
                }
                Sign.TextLine line = sign.getTextLines().get(currentTextLine);
                if (currentText.length() < line.getMaxLength() && !Character.isISOControl(codePoint)) {
                    barrierEntity.setThisSignTextLine(currentTextLine, currentText + codePoint);
                    return true;
                }
            }
        }
        return super.charTyped(codePoint, modifiers);
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
}
