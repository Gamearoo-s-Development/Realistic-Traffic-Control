package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class GuiButtonToggle extends GuiButton {
    private boolean toggled;
    private final String label;

    public GuiButtonToggle(int id, int x, int y, int width, int height, boolean initialState) {
        this(id, x, y, width, height, initialState, "Night Flash");
    }

    public GuiButtonToggle(int id, int x, int y, int width, int height, boolean initialState, String label) {
        super(id, x, y, width, height, "");
        this.toggled = initialState;
        this.label = label;
    }

    public void toggle() {
        this.toggled = !this.toggled;
    }

    public boolean isToggled() {
        return this.toggled;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            FontRenderer fr = mc.fontRenderer;
            this.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, this.toggled ? 0xFF00FF00 : 0xFFFF0000);
            String stateLabel = toggled ? label + ": ON" : label + ": OFF";
            fr.drawString(stateLabel, this.x + this.width + 4, this.y + 6, 0xFFFFFF);
        }
    }
}
