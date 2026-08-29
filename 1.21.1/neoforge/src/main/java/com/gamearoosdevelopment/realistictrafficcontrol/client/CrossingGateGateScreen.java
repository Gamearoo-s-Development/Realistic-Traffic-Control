package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.menu.CrossingGateGateMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingGateGateBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingGateGateBlockEntity.GateLightCount;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Port of 1.12.2 {@code CrossingGateGateGui} using the original field layout. */
public class CrossingGateGateScreen extends AbstractContainerScreen<CrossingGateGateMenu> {

    private EditBox length;
    private EditBox upperRotation;
    private EditBox lowerRotation;
    private EditBox delay;
    private EditBox lightStartOffset;
    private Checkbox threeLights;
    private Checkbox oneLight;

    public CrossingGateGateScreen(CrossingGateGateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        CrossingGateGateBlockEntity te = menu.getGate(minecraft.player);
        if (te == null) {
            return;
        }

        int horizontalCenter = width / 2;
        int verticalCenter = height / 2;

        length = new EditBox(font, horizontalCenter - 50, verticalCenter - 70, 100, 20,
                Component.literal("Gate Length"));
        length.setValue(String.valueOf(te.getCrossingGateLength()));

        upperRotation = new EditBox(font, horizontalCenter - 50, verticalCenter - 40, 100, 20,
                Component.literal("Upper Rotation"));
        upperRotation.setValue(String.valueOf(te.getUpperRotationLimit()));

        lowerRotation = new EditBox(font, horizontalCenter - 50, verticalCenter - 10, 100, 20,
                Component.literal("Lower Rotation"));
        lowerRotation.setValue(String.valueOf(te.getLowerRotationLimit()));

        delay = new EditBox(font, horizontalCenter - 50, verticalCenter + 20, 100, 20,
                Component.literal("Activation Delay"));
        delay.setValue(String.valueOf(te.getDelay()));

        lightStartOffset = new EditBox(font, horizontalCenter - 50, verticalCenter + 50, 100, 20,
                Component.literal("Light Start Offset"));
        lightStartOffset.setValue(String.valueOf(te.getLightStartOffset()));

        threeLights = Checkbox.builder(Component.literal("Three Gate Lights"), font)
                .pos(horizontalCenter - 50, verticalCenter + 80)
                .selected(te.getGateLightCount() == GateLightCount.ThreeLights)
                .build();

        oneLight = Checkbox.builder(Component.literal("One Gate Light"), font)
                .pos(horizontalCenter + 4, verticalCenter + 80)
                .selected(te.getGateLightCount() == GateLightCount.OneLight)
                .build();

        addRenderableWidget(length);
        addRenderableWidget(upperRotation);
        addRenderableWidget(lowerRotation);
        addRenderableWidget(delay);
        addRenderableWidget(lightStartOffset);
        addRenderableWidget(threeLights);
        addRenderableWidget(oneLight);

        setInitialFocus(length);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int horizontalCenter = width / 2;
        int verticalCenter = height / 2;

        drawLabel(graphics, "Gate Length:", length.getX(), length.getY());
        drawLabel(graphics, "Upper Rotation:", upperRotation.getX(), upperRotation.getY());
        drawLabel(graphics, "Lower Rotation:", lowerRotation.getX(), lowerRotation.getY());
        drawLabel(graphics, "Activation Delay:", delay.getX(), delay.getY());
        drawLabel(graphics, "Light Start Offset:", lightStartOffset.getX(), lightStartOffset.getY());
    }

    private void drawLabel(GuiGraphics graphics, String text, int fieldX, int fieldY) {
        int labelWidth = font.width(text);
        graphics.drawString(font, text, fieldX - labelWidth - 10, fieldY + 6, 0xFFFFFF, false);
    }

    @Override
    public void onClose() {
        CrossingGateGateBlockEntity te = menu.getGate(minecraft.player);
        if (te != null) {
            te.setCrossingGateLength(parseFloatOrDefault(length.getValue()));
            te.setUpperRotationLimit(parseFloatOrDefault(upperRotation.getValue()));
            te.setLowerRotationLimit(parseFloatOrDefault(lowerRotation.getValue()));
            te.setDelay(parseFloatOrDefault(delay.getValue()));
            te.setLightStartOffset(parseFloatOrDefault(lightStartOffset.getValue()));
            te.setGateLightCount(threeLights.selected() ? GateLightCount.ThreeLights : GateLightCount.OneLight);
            te.performClientToServerSync();
        }
        super.onClose();
    }

    private static float parseFloatOrDefault(String text) {
        try {
            return Float.parseFloat(text);
        } catch (Exception ex) {
            return 0F;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }
}
