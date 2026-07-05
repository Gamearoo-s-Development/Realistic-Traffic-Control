package com.gamearoosdevelopment.realistictrafficcontrol.client.gui;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Simplified port of 1.12.2 {@code GuiImageList} as a scrollable sign picker. */
public class SignImageListWidget extends ObjectSelectionList<SignImageListWidget.SignEntry> {

    private final Consumer<Sign> onSelect;
    private boolean visible = true;

    public SignImageListWidget(int x, int y, int width, int height, Consumer<Sign> onSelect) {
        super(Minecraft.getInstance(), width, height, y, y + height);
        this.onSelect = onSelect;
        this.setX(x);
        reload("");
    }

    public void filter(String text) {
        reload(text);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (visible) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void reload(String filter) {
        clearEntries();
        String needle = filter == null ? "" : filter.toLowerCase();
        List<Sign> filtered = new ArrayList<>();
        for (Sign sign : ModRealisticTrafficControl.signRepo.getAllSigns()) {
            String label = sign.getName() + " (" + sign.getVariant() + ")";
            if (needle.isEmpty() || label.toLowerCase().contains(needle)) {
                filtered.add(sign);
            }
        }
        filtered.sort(Comparator.comparing(Sign::getName));
        for (Sign sign : filtered) {
            addEntry(new SignEntry(sign));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            SignEntry entry = getSelected();
            if (entry != null) {
                onSelect.accept(entry.sign);
            }
            return true;
        }
        return false;
    }

    public class SignEntry extends Entry<SignEntry> {
        private final Sign sign;

        SignEntry(Sign sign) {
            this.sign = sign;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.drawString(Minecraft.getInstance().font,
                    sign.getName() + " (" + sign.getVariant() + ")", left + 2, top + 6, 0xFFFFFF);
        }

        @Override
        public Component getNarration() {
            return Component.literal(sign.getName());
        }
    }
}
