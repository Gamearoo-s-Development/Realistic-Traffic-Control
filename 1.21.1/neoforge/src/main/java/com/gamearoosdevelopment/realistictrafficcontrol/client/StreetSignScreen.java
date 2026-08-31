package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.StreetSignMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSignBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Port of 1.12.2 {@code StreetSignGui}. */
public class StreetSignScreen extends AbstractContainerScreen<StreetSignMenu> {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(
            ModRealisticTrafficControl.MODID, "textures/block/street_sign.png");

    private StreetSignBlockEntity streetSignEntity;

    public StreetSignScreen(StreetSignMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 1;
        imageHeight = 1;
    }

    @Override
    protected void init() {
        super.init();
        streetSignEntity = menu.getStreetSignEntity(minecraft.player);
        if (streetSignEntity == null) {
            return;
        }
        int horizontalCenter = width / 2;
        int verticalCenter = height / 2;
        for (int i = 0; i < StreetSignBlockEntity.MAX_STREET_SIGNS; i++) {
            StreetSign sign = streetSignEntity.getStreetSign(i);
            if (sign == null) {
                continue;
            }
            int baseId = 10 * i;
            int yOffset = (1 - i) * 80;
            int signIndex = i;
            addRenderableWidget(Button.builder(Component.literal(getDirectionText(sign)), b -> {
                incrementRotation(streetSignEntity.getStreetSign(signIndex));
                b.setMessage(Component.literal(getDirectionText(streetSignEntity.getStreetSign(signIndex))));
            }).bounds(horizontalCenter + 132, verticalCenter + yOffset, 70, 20).build());

            addColorButton(baseId + 1, signIndex, StreetSign.StreetSignColors.Green,
                    horizontalCenter + 206, verticalCenter + yOffset);
            addColorButton(baseId + 2, signIndex, StreetSign.StreetSignColors.Red,
                    horizontalCenter + 206, verticalCenter + yOffset + 24);
            addColorButton(baseId + 3, signIndex, StreetSign.StreetSignColors.Blue,
                    horizontalCenter + 250, verticalCenter + yOffset);
            addColorButton(baseId + 4, signIndex, StreetSign.StreetSignColors.Yellow,
                    horizontalCenter + 250, verticalCenter + yOffset + 24);
        }
    }

    private void addColorButton(int id, int signIndex, StreetSign.StreetSignColors color, int x, int y) {
        addRenderableWidget(Button.builder(Component.literal(color.name()), b -> {
            StreetSign sign = streetSignEntity.getStreetSign(signIndex);
            if (sign != null) {
                sign.setColor(color);
            }
        }).bounds(x, y, 40, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        if (streetSignEntity != null) {
            int horizontalCenter = width / 2;
            int verticalCenter = height / 2;
            for (int i = 0; i < StreetSignBlockEntity.MAX_STREET_SIGNS; i++) {
                StreetSign sign = streetSignEntity.getStreetSign(i);
                if (sign == null) {
                    continue;
                }
                int yTexOffset = (sign.getColor().getRow() - 1) * 64;
                int yOffset = (1 - i) * 80;
                graphics.blit(ATLAS, horizontalCenter - 128, verticalCenter + yOffset, 0, yTexOffset, 256, 64, 256, 256);
                int textWidth = font.width(sign.getText());
                float scale = Math.min(4F, 240F / Math.max(1, textWidth));
                graphics.pose().pushPose();
                graphics.pose().translate(horizontalCenter, verticalCenter + yOffset + 18, 0);
                graphics.pose().scale(scale, 4, 1);
                graphics.drawCenteredString(font, sign.getText() + (sign.getIsNew() ? "_" : ""), 0, 0,
                        sign.getTextColor());
                graphics.pose().popPose();
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (streetSignEntity != null) {
            for (int i = 0; i < StreetSignBlockEntity.MAX_STREET_SIGNS; i++) {
                StreetSign sign = streetSignEntity.getStreetSign(i);
                if (sign != null && sign.getIsNew()) {
                    if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !sign.getText().isEmpty()) {
                        sign.setText(sign.getText().substring(0, sign.getText().length() - 1));
                        return true;
                    }
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (streetSignEntity != null) {
            for (int i = 0; i < StreetSignBlockEntity.MAX_STREET_SIGNS; i++) {
                StreetSign sign = streetSignEntity.getStreetSign(i);
                if (sign != null && sign.getIsNew()) {
                    if (Character.isLetterOrDigit(codePoint) || Character.isSpaceChar(codePoint)
                            || ".!,@#$%^&*()-_=+/`~".indexOf(codePoint) >= 0) {
                        sign.setText(sign.getText() + codePoint);
                        return true;
                    }
                }
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        if (streetSignEntity != null) {
            for (int i = 0; i < StreetSignBlockEntity.MAX_STREET_SIGNS; i++) {
                StreetSign sign = streetSignEntity.getStreetSign(i);
                if (sign != null) {
                    sign.setIsNew(false);
                }
            }
            streetSignEntity.performClientToServerSync();
        }
        super.onClose();
    }

    private static String getDirectionText(StreetSign sign) {
        return switch (sign.getRotation()) {
            case 0, 8 -> "N/S";
            case 1, 9 -> "NNE/SSW";
            case 2, 10 -> "NW/SE";
            case 3, 11 -> "WNW/ESE";
            case 4, 12 -> "W/E";
            case 5, 13 -> "WSW/ENE";
            case 6, 14 -> "SE/NW";
            case 7, 15 -> "SSE/NNW";
            default -> "";
        };
    }

    private static void incrementRotation(StreetSign sign) {
        int current = sign.getRotation();
        if (current == 15) {
            current = -1;
        }
        sign.setRotation(current + 1);
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
