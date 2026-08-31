package com.gamearoosdevelopment.realistictrafficcontrol.client.gui;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Sixteen-pixel image grid matching the 1.12.2 {@code GuiImageList}. */
public class SignImageListWidget extends AbstractWidget {

    private final Consumer<Sign> onSelect;
    private final List<Sign> signs = new ArrayList<>();
    private int scrollRow;
    private boolean draggingScrollbar;

    public SignImageListWidget(int x, int y, int width, int height, Consumer<Sign> onSelect) {
        super(x + width % 16, y + height % 16, width - width % 16, height - height % 16,
                Component.literal("Signs"));
        this.onSelect = onSelect;
        filter("");
    }

    public void filter(String text) {
        String filterText = text == null ? "" : text.trim();
        String typeFilter = "";
        int typeStart = filterText.indexOf('@');
        if (typeStart >= 0) {
            String typePart = filterText.substring(typeStart + 1).trim();
            if (typePart.startsWith("\"") && typePart.indexOf('"', 1) > 1) {
                int end = typePart.indexOf('"', 1);
                typeFilter = typePart.substring(1, end);
                filterText = (filterText.substring(0, typeStart) + typePart.substring(end + 1)).trim();
            } else if (!typePart.isEmpty()) {
                int end = typePart.indexOf(' ');
                typeFilter = end < 0 ? typePart : typePart.substring(0, end);
                filterText = (filterText.substring(0, typeStart)
                        + (end < 0 ? "" : typePart.substring(end + 1))).trim();
            }
        }
        String nameNeedle = filterText.toLowerCase();
        String typeNeedle = typeFilter.toLowerCase();
        signs.clear();
        for (Sign sign : ModRealisticTrafficControl.signRepo.getAllSigns()) {
            String label = sign.getName() + " (" + sign.getVariant() + ")";
            String friendlyType = ModRealisticTrafficControl.signRepo.getFriendlyTypeName(sign.getType());
            boolean nameMatches = nameNeedle.isEmpty() || label.toLowerCase().contains(nameNeedle);
            boolean typeMatches = typeNeedle.isEmpty() || sign.getType().toLowerCase().contains(typeNeedle)
                    || friendlyType != null && friendlyType.toLowerCase().contains(typeNeedle);
            if (nameMatches && typeMatches) {
                signs.add(sign);
            }
        }
        scrollRow = 0;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    private int columns() {
        return Math.max(1, width / 16 - 1);
    }

    private int visibleRows() {
        return Math.max(1, height / 16);
    }

    private int totalRows() {
        return Math.max(1, (signs.size() + columns() - 1) / columns());
    }

    private int maxScrollRow() {
        return Math.max(0, totalRows() - visibleRows());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        graphics.setColor(1F, 1F, 1F, 1F);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFFFFFFFF);
        int hoveredIndex = -1;
        for (int row = 0; row < visibleRows(); row++) {
            for (int col = 0; col < columns(); col++) {
                int index = (row + scrollRow) * columns() + col;
                if (index >= signs.size()) {
                    break;
                }
                int imageX = getX() + col * 16;
                int imageY = getY() + row * 16;
                if (mouseX >= imageX && mouseX < imageX + 16 && mouseY >= imageY && mouseY < imageY + 16) {
                    graphics.fill(imageX, imageY, imageX + 16, imageY + 16, 0xFF0000FF);
                    hoveredIndex = index;
                }
                Sign sign = signs.get(index);
                graphics.blit(sign.getFrontImageResourceLocation(), imageX, imageY, 0, 0, 16, 16, 16, 16);
            }
        }

        int scrollbarLeft = getX() + width - 16;
        graphics.fill(scrollbarLeft, getY(), getX() + width, getY() + height, 0xFF202020);
        int thumbHeight = maxScrollRow() == 0 ? height : Math.max(8, height / (maxScrollRow() + 1));
        int thumbTop = getY() + (maxScrollRow() == 0 ? 0
                : (height - thumbHeight) * scrollRow / maxScrollRow());
        graphics.fill(scrollbarLeft, thumbTop, getX() + width, thumbTop + thumbHeight, 0xFF808080);

        if (hoveredIndex >= 0) {
            Sign sign = signs.get(hoveredIndex);
            List<Component> tooltip = new ArrayList<>();
            String friendly = ModRealisticTrafficControl.signRepo.getFriendlyTypeName(sign.getType());
            tooltip.add(Component.literal("\u00a7e" + sign.getName() + " ("
                    + (friendly == null ? sign.getType() : friendly) + ")"));
            if (sign.getToolTip() != null && !sign.getToolTip().isEmpty()) {
                tooltip.add(Component.literal(sign.getToolTip()));
            }
            graphics.renderComponentTooltip(net.minecraft.client.Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int localX = (int) mouseX - getX();
        if (localX >= width - 16) {
            draggingScrollbar = true;
            setScrollFromMouse(mouseY);
            return true;
        }
        int col = localX / 16;
        int row = ((int) mouseY - getY()) / 16;
        int index = (row + scrollRow) * columns() + col;
        if (index >= 0 && index < signs.size()) {
            onSelect.accept(signs.get(index));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            setScrollFromMouse(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = draggingScrollbar;
        draggingScrollbar = false;
        return wasDragging;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY)) {
            scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        return false;
    }

    private void setScrollFromMouse(double mouseY) {
        if (maxScrollRow() > 0) {
            double ratio = (mouseY - getY()) / Math.max(1.0, height);
            scrollRow = Math.max(0, Math.min(maxScrollRow(), (int) Math.round(ratio * maxScrollRow())));
        }
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }
}
