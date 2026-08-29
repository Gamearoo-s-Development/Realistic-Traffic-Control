package com.gamearoosdevelopment.realistictrafficcontrol.client.gui;

import com.gamearoosdevelopment.realistictrafficcontrol.util.FyaMode;
import com.gamearoosdevelopment.realistictrafficcontrol.util.IdleBulbMode;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 1.21.1 widgets matching the 1.12.2 control-box GUI button family ({@code GuiButtonExtSelectable},
 * {@code GuiButtonToggle*}, etc.).
 */
public final class ControlBoxWidgets {

    private ControlBoxWidgets() {
    }

    public static Button selectable(int x, int y, int w, int h, String label, Runnable onPress) {
        return Button.builder(Component.literal(label), b -> onPress.run()).bounds(x, y, w, h).build();
    }

    public static class SelectableTab extends Button {
        private boolean selected;

        public SelectableTab(int x, int y, int w, int h, String label, Runnable onPress) {
            super(x, y, w, h, Component.literal(label), b -> onPress.run(), DEFAULT_NARRATION);
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public boolean isSelected() {
            return selected;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bg = selected ? 0xFF555555 : (isHovered() ? 0xFF777777 : 0xFF333333);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
            graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage().getString(),
                    getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFF);
        }
    }

    public static class LabeledToggle extends AbstractWidget {
        private boolean toggled;
        private final java.util.function.Function<Boolean, String> labelFn;
        private Runnable onToggle;

        public LabeledToggle(int x, int y, int w, int h, boolean initial, java.util.function.Function<Boolean, String> labelFn) {
            super(x, y, w, h, Component.empty());
            this.toggled = initial;
            this.labelFn = labelFn;
        }

        public void setOnToggle(Runnable onToggle) {
            this.onToggle = onToggle;
        }

        public void toggle() {
            toggled = !toggled;
            if (onToggle != null) {
                onToggle.run();
            }
        }

        public boolean isToggled() {
            return toggled;
        }

        public void setToggled(boolean toggled) {
            this.toggled = toggled;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = toggled ? 0xFF00FF00 : 0xFFFF0000;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, color);
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, labelFn.apply(toggled),
                    getX() + width + 4, getY() + 6, 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            toggle();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    public static class MovementToggle extends AbstractWidget {
        private final String movementLabel;
        private boolean toggled;
        private Runnable onToggle;

        public MovementToggle(int x, int y, int w, int h, String movementLabel, boolean initial) {
            super(x, y, w, h, Component.empty());
            this.movementLabel = movementLabel;
            this.toggled = initial;
        }

        public void setOnToggle(Runnable onToggle) {
            this.onToggle = onToggle;
        }

        public void toggle() {
            toggled = !toggled;
            if (onToggle != null) {
                onToggle.run();
            }
        }

        public boolean isToggled() {
            return toggled;
        }

        public void setToggled(boolean toggled) {
            this.toggled = toggled;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, toggled ? 0xFF00AA00 : 0xFFAA0000);
            String label = movementLabel + ": " + (toggled ? "ON" : "OFF");
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, label, getX() + 4, getY() + 6, 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            toggle();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    public static class IdleToggle extends AbstractWidget {
        private final String movementLabel;
        private final boolean straightOnly;
        private IdleBulbMode mode;
        private boolean movementEnabled;
        private Runnable onToggle;

        public IdleToggle(int x, int y, int w, int h, String movementLabel, IdleBulbMode initial, boolean straightOnly) {
            super(x, y, w, h, Component.empty());
            this.movementLabel = movementLabel;
            this.straightOnly = straightOnly;
            this.mode = straightOnly ? initial.normalizeForStraight() : initial;
        }

        public void setMovementEnabled(boolean movementEnabled) {
            this.movementEnabled = movementEnabled;
            this.active = !movementEnabled;
        }

        public void setOnToggle(Runnable onToggle) {
            this.onToggle = onToggle;
        }

        public void toggle() {
            if (movementEnabled) {
                return;
            }
            mode = straightOnly ? mode.nextForStraight() : mode.next();
            if (onToggle != null) {
                onToggle.run();
            }
        }

        public IdleBulbMode getState() {
            return mode;
        }

        public void setState(IdleBulbMode state) {
            this.mode = straightOnly ? state.normalizeForStraight() : state;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color;
            if (movementEnabled) {
                color = 0xFF555555;
            } else {
                color = switch (mode) {
                    case ARROW_GREEN, SOLID_GREEN -> 0xFF00AA00;
                    case ARROW_YELLOW, SOLID_YELLOW -> 0xFFAAAA00;
                    default -> 0xFFAA0000;
                };
            }
            graphics.fill(getX(), getY(), getX() + width, getY() + height, color);
            String label = movementEnabled
                    ? movementLabel + ": ON"
                    : movementLabel + ": " + mode.getShortLabel();
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, label, getX() + 4, getY() + 6,
                    movementEnabled ? 0x888888 : 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            toggle();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    public static class FyaToggle extends AbstractWidget {
        private final String movementLabel;
        private FyaMode mode;
        private Runnable onToggle;

        public FyaToggle(int x, int y, int w, int h, String movementLabel, FyaMode initial) {
            super(x, y, w, h, Component.empty());
            this.movementLabel = movementLabel;
            this.mode = initial;
        }

        public void setOnToggle(Runnable onToggle) {
            this.onToggle = onToggle;
        }

        public void toggle() {
            mode = mode.next();
            if (onToggle != null) {
                onToggle.run();
            }
        }

        public FyaMode getMode() {
            return mode;
        }

        public void setMode(FyaMode mode) {
            this.mode = mode;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = switch (mode) {
                case OFF -> 0xFFAA0000;
                case NIGHT_ONLY -> 0xFFAA8800;
                default -> 0xFF00AA00;
            };
            graphics.fill(getX(), getY(), getX() + width, getY() + height, color);
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                    movementLabel + ": " + mode.getShortLabel(), getX() + 4, getY() + 6, 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            toggle();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    public static class ManualBulbCheckbox extends AbstractWidget {
        private boolean selected;
        private final Runnable onPress;

        public ManualBulbCheckbox(int x, int y, boolean selected, Runnable onPress) {
            super(x, y, 12, 12, Component.empty());
            this.selected = selected;
            this.onPress = onPress;
        }

        public void setChecked(boolean checked) {
            this.selected = checked;
        }

        public boolean isChecked() {
            return selected;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int border = isHovered() ? 0xFFFFFFFF : 0xFFAAAAAA;
            graphics.fill(getX(), getY(), getX() + 12, getY() + 12, border);
            graphics.fill(getX() + 1, getY() + 1, getX() + 11, getY() + 11, selected ? 0xFF00AA00 : 0xFF333333);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            selected = !selected;
            onPress.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
