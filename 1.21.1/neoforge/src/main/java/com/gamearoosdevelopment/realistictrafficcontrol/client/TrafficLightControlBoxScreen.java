package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.ControlBoxWidgets.ManualBulbCheckbox;
import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.ControlBoxWidgets.SelectableTab;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.TrafficLightControlBoxMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Port of 1.12.2 {@code TrafficLightControlBoxGui}: fullscreen control-box editor with manual bulb grid,
 * automatic timing fields, and an Advanced sub-screen.
 */
public class TrafficLightControlBoxScreen extends AbstractContainerScreen<TrafficLightControlBoxMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            ModRealisticTrafficControl.MODID, "textures/gui/control_box_gui.png");

    private enum Mode {
        MANUAL_NS,
        MANUAL_WE,
        AUTOMATIC
    }

    private Mode currentMode = Mode.MANUAL_NS;
    private boolean editingNorthSouth = true;

    private SelectableTab manualModeNorth;
    private SelectableTab manualModeSouth;
    private SelectableTab autoModeNorth;
    private SelectableTab autoModeSouth;
    private Button advancedOptionsButton;

    private EditBox greenMinimumNS;
    private EditBox greenMinimumEW;
    private EditBox greenMaxNS;
    private EditBox greenMaxEW;
    private EditBox yellowTimeNS;
    private EditBox yellowTimeEW;
    private EditBox redTimeNS;
    private EditBox redTimeEW;
    private EditBox arrowMinimumNS;
    private EditBox arrowMinimumEW;
    private EditBox arrowMaxNS;
    private EditBox arrowMaxEW;
    private EditBox crossTime;
    private EditBox crossWarningTime;
    private EditBox rightArrowMinimum;

    private final List<ManualCheckboxBinding> manualBindings = new ArrayList<>();

    private int tickCounter;
    private boolean isRightRed = true;
    private boolean isLeftYellow = true;
    private String leftTurn = "Red";
    private boolean isLeftGreen = true;
    private boolean isRed = true;
    private boolean isYellow = true;
    private boolean isGreen = true;

    public TrafficLightControlBoxScreen(TrafficLightControlBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 1;
        this.imageHeight = 1;
    }

    private TrafficLightControlBoxBlockEntity getBox() {
        return menu.getControlBox(minecraft.player);
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        manualBindings.clear();

        TrafficLightControlBoxBlockEntity box = getBox();
        if (box == null) {
            return;
        }

        currentMode = box.isAutoMode() ? Mode.AUTOMATIC : Mode.MANUAL_NS;
        int cx = width / 2;
        int cy = height / 2;

        manualModeNorth = addTab(cx - 107, cy - 100, "N/S", () -> switchManualMode(Mode.MANUAL_NS));
        manualModeSouth = addTab(cx - 107, cy - 78, "W/E", () -> switchManualMode(Mode.MANUAL_WE));
        manualModeNorth.setSelected(currentMode == Mode.MANUAL_NS);
        manualModeSouth.setSelected(currentMode == Mode.MANUAL_WE);

        int hx = cx - 27;
        int hxFlash = cx - 12;
        int hxOff = cx + 10;
        int hxOffFlash = cx + 25;
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy - 83,
                EnumTrafficLightBulbTypes.Red, EnumTrafficLightBulbTypes.RedX, EnumTrafficLightBulbTypes.StraightRed);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy - 63,
                EnumTrafficLightBulbTypes.Yellow, EnumTrafficLightBulbTypes.YellowX, EnumTrafficLightBulbTypes.StraightYellow);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy - 43,
                EnumTrafficLightBulbTypes.Green, EnumTrafficLightBulbTypes.GreenDownArrow, EnumTrafficLightBulbTypes.StraightGreen);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy - 23,
                EnumTrafficLightBulbTypes.RedArrowLeft, EnumTrafficLightBulbTypes.NoLeftTurn, EnumTrafficLightBulbTypes.RedArrowUTurn);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy - 3,
                EnumTrafficLightBulbTypes.YellowArrowLeft, EnumTrafficLightBulbTypes.YellowArrowUTurn,
                EnumTrafficLightBulbTypes.YellowArrowLeft2, EnumTrafficLightBulbTypes.YellowArrowUTurn2);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy + 17,
                EnumTrafficLightBulbTypes.GreenArrowLeft, EnumTrafficLightBulbTypes.GreenArrowUTurn);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy + 37, EnumTrafficLightBulbTypes.Cross);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy + 57, EnumTrafficLightBulbTypes.DontCross);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy + 77,
                EnumTrafficLightBulbTypes.RedArrowRight, EnumTrafficLightBulbTypes.NoRightTurn);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy + 97,
                EnumTrafficLightBulbTypes.YellowArrowRight, EnumTrafficLightBulbTypes.YellowArrowRight2);
        addManualRow(hx, hxFlash, hxOff, hxOffFlash, cy + 117, EnumTrafficLightBulbTypes.GreenArrowRight);

        if (currentMode == Mode.AUTOMATIC) {
            autoModeNorth = addTab(cx - 107, cy - 100, "N/S", () -> selectAutoAxis(true));
            autoModeSouth = addTab(cx - 107, cy - 78, "W/E", () -> selectAutoAxis(false));
            autoModeNorth.setSelected(true);
            advancedOptionsButton = addRenderableWidget(Button.builder(Component.literal("Advanced"),
                    b -> minecraft.setScreen(new TrafficLightControlBoxAdvancedScreen(this, menu,
                            minecraft.player.getInventory(), title)))
                    .bounds(cx - 107, cy - 56, 51, 20).build());
        }

        int xField = cx - 54;
        int yStart = cy - 90;
        int spacing = 30;
        greenMinimumNS = addAutoField(xField, yStart);
        greenMinimumEW = addAutoField(xField, yStart);
        yStart += spacing;
        greenMaxNS = addAutoField(xField, yStart);
        greenMaxEW = addAutoField(xField, yStart);
        yStart += spacing;
        yellowTimeNS = addAutoField(xField, yStart);
        yellowTimeEW = addAutoField(xField, yStart);
        yStart += spacing;
        redTimeNS = addAutoField(xField, yStart);
        redTimeEW = addAutoField(xField, yStart);
        yStart += spacing;
        arrowMinimumNS = addAutoField(xField, yStart);
        arrowMinimumEW = addAutoField(xField, yStart);
        yStart += spacing;
        arrowMaxNS = addAutoField(xField, yStart);
        arrowMaxEW = addAutoField(xField, yStart);
        yStart += spacing;
        crossTime = addAutoField(xField, yStart);
        yStart += spacing;
        crossWarningTime = addAutoField(xField, yStart);
        yStart += spacing;
        rightArrowMinimum = addAutoField(xField, yStart);

        fillAutoFields(box);
        syncManualCheckboxes();
        updateVisibility();
    }

    private SelectableTab addTab(int x, int y, String label, Runnable action) {
        return addRenderableWidget(new SelectableTab(x, y, 25, 20, label, action));
    }

    private EditBox addAutoField(int x, int y) {
        EditBox box = new EditBox(font, x, y, 105, 20, Component.empty());
        box.setFilter(s -> s.isEmpty() || s.matches("[0-9.]*"));
        addRenderableWidget(box);
        return box;
    }

    private void addManualRow(int xOn, int xOnFlash, int xOff, int xOffFlash, int y, EnumTrafficLightBulbTypes... types) {
        ManualCheckboxBinding binding = new ManualCheckboxBinding(types);
        binding.on = addManualCheckbox(xOn, y, false, true, binding);
        binding.onFlash = addManualCheckbox(xOnFlash, y, true, true, binding);
        binding.off = addManualCheckbox(xOff, y, false, false, binding);
        binding.offFlash = addManualCheckbox(xOffFlash, y, true, false, binding);
        manualBindings.add(binding);
    }

    private ManualBulbCheckbox addManualCheckbox(int x, int y, boolean flash, boolean forActive, ManualCheckboxBinding binding) {
        ManualBulbCheckbox[] ref = new ManualBulbCheckbox[1];
        ref[0] = new ManualBulbCheckbox(x, y, false,
                () -> applyManual(binding, flash, forActive, ref[0].isChecked()));
        return addRenderableWidget(ref[0]);
    }

    private void fillAutoFields(TrafficLightControlBoxBlockEntity box) {
        var auto = box.getAutomator();
        greenMinimumNS.setValue(Double.toString(auto.getGreenMinimumNS()));
        greenMinimumEW.setValue(Double.toString(auto.getGreenMinimumEW()));
        greenMaxNS.setValue(Double.toString(auto.getGreenMaxNS()));
        greenMaxEW.setValue(Double.toString(auto.getGreenMaxEW()));
        yellowTimeNS.setValue(Double.toString(auto.getYellowTimeNS()));
        yellowTimeEW.setValue(Double.toString(auto.getYellowTimeEW()));
        redTimeNS.setValue(Double.toString(auto.getRedTimeNS()));
        redTimeEW.setValue(Double.toString(auto.getRedTimeEW()));
        arrowMinimumNS.setValue(Double.toString(auto.getArrowMinimumNS()));
        arrowMinimumEW.setValue(Double.toString(auto.getArrowMinimumES()));
        arrowMaxNS.setValue(Double.toString(auto.getArrowMaxNS()));
        arrowMaxEW.setValue(Double.toString(auto.getArrowMaxEW()));
        crossTime.setValue(Double.toString(auto.getCrossTime()));
        crossWarningTime.setValue(Double.toString(auto.getCrossWarningTime()));
        rightArrowMinimum.setValue(Double.toString(auto.getRightArrowTime()));
    }

    private void switchManualMode(Mode mode) {
        currentMode = mode;
        manualModeNorth.setSelected(mode == Mode.MANUAL_NS);
        manualModeSouth.setSelected(mode == Mode.MANUAL_WE);
        syncManualCheckboxes();
        updateVisibility();
    }

    private void selectAutoAxis(boolean northSouth) {
        editingNorthSouth = northSouth;
        if (autoModeNorth != null) {
            autoModeNorth.setSelected(northSouth);
            autoModeSouth.setSelected(!northSouth);
        }
    }

    private void applyManual(ManualCheckboxBinding binding, boolean flash, boolean forActive, boolean checked) {
        TrafficLightControlBoxBlockEntity box = getBox();
        if (box == null) {
            return;
        }
        for (EnumTrafficLightBulbTypes type : binding.types) {
            if (currentMode == Mode.MANUAL_NS) {
                if (forActive) {
                    box.addRemoveNorthSouthActive(type, flash, checked);
                } else {
                    box.addRemoveNorthSouthInactive(type, flash, checked);
                }
            } else {
                if (forActive) {
                    box.addRemoveWestEastActive(type, flash, checked);
                } else {
                    box.addRemoveWestEastInactive(type, flash, checked);
                }
            }
        }
    }

    private void syncManualCheckboxes() {
        TrafficLightControlBoxBlockEntity box = getBox();
        if (box == null) {
            return;
        }
        for (ManualCheckboxBinding binding : manualBindings) {
            binding.on.setChecked(isChecked(box, binding.types, false, true));
            binding.onFlash.setChecked(isChecked(box, binding.types, true, true));
            binding.off.setChecked(isChecked(box, binding.types, false, false));
            binding.offFlash.setChecked(isChecked(box, binding.types, true, false));
        }
    }

    private boolean isChecked(TrafficLightControlBoxBlockEntity box, EnumTrafficLightBulbTypes[] types, boolean flash,
            boolean forActive) {
        for (EnumTrafficLightBulbTypes type : types) {
            boolean result = currentMode == Mode.MANUAL_NS
                    ? box.hasSpecificNorthSouthManualOption(type, flash, forActive)
                    : box.hasSpecificWestEastManualOption(type, flash, forActive);
            if (result) {
                return true;
            }
        }
        return false;
    }

    private void updateVisibility() {
        boolean manual = currentMode == Mode.MANUAL_NS || currentMode == Mode.MANUAL_WE;
        if (manualModeNorth != null) {
            manualModeNorth.visible = manual;
            manualModeSouth.visible = manual;
        }
        for (ManualCheckboxBinding binding : manualBindings) {
            binding.on.visible = manual;
            binding.onFlash.visible = manual;
            binding.off.visible = manual;
            binding.offFlash.visible = manual;
        }
        if (advancedOptionsButton != null) {
            advancedOptionsButton.visible = currentMode == Mode.AUTOMATIC;
        }
        if (autoModeNorth != null) {
            autoModeNorth.visible = currentMode == Mode.AUTOMATIC;
            autoModeSouth.visible = currentMode == Mode.AUTOMATIC;
        }
        setAutoFieldVisible(greenMinimumNS, currentMode == Mode.AUTOMATIC && editingNorthSouth);
        setAutoFieldVisible(greenMinimumEW, currentMode == Mode.AUTOMATIC && !editingNorthSouth);
        setAutoFieldVisible(greenMaxNS, currentMode == Mode.AUTOMATIC && editingNorthSouth);
        setAutoFieldVisible(greenMaxEW, currentMode == Mode.AUTOMATIC && !editingNorthSouth);
        setAutoFieldVisible(yellowTimeNS, currentMode == Mode.AUTOMATIC && editingNorthSouth);
        setAutoFieldVisible(yellowTimeEW, currentMode == Mode.AUTOMATIC && !editingNorthSouth);
        setAutoFieldVisible(redTimeNS, currentMode == Mode.AUTOMATIC && editingNorthSouth);
        setAutoFieldVisible(redTimeEW, currentMode == Mode.AUTOMATIC && !editingNorthSouth);
        setAutoFieldVisible(arrowMinimumNS, currentMode == Mode.AUTOMATIC && editingNorthSouth);
        setAutoFieldVisible(arrowMinimumEW, currentMode == Mode.AUTOMATIC && !editingNorthSouth);
        setAutoFieldVisible(arrowMaxNS, currentMode == Mode.AUTOMATIC && editingNorthSouth);
        setAutoFieldVisible(arrowMaxEW, currentMode == Mode.AUTOMATIC && !editingNorthSouth);
        setAutoFieldVisible(crossTime, currentMode == Mode.AUTOMATIC);
        setAutoFieldVisible(crossWarningTime, currentMode == Mode.AUTOMATIC);
        setAutoFieldVisible(rightArrowMinimum, currentMode == Mode.AUTOMATIC);
    }

    private static void setAutoFieldVisible(EditBox field, boolean visible) {
        if (field != null) {
            field.visible = visible;
            field.setFocused(visible && field.isFocused());
        }
    }

    public void renderBackgroundPanel(GuiGraphics graphics) {
        RTCGuiTextures.blitFullscreenGui(graphics, BACKGROUND, width, height);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderBackgroundPanel(graphics);
    }

  @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 1.12.2 used GuiScreen without container title/inventory labels.
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, net.minecraft.world.inventory.Slot slot) {
        // No player inventory on this fullscreen editor.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;
        if (currentMode == Mode.MANUAL_NS || currentMode == Mode.MANUAL_WE) {
            renderManualOverlay(graphics, cx, cy, mouseX, mouseY);
        } else {
            renderAutomaticOverlay(graphics, cx, cy);
        }
    }

    private void renderManualOverlay(GuiGraphics graphics, int cx, int cy, int mouseX, int mouseY) {
        graphics.drawString(font, "Manual Mode", cx - 54, cy - 110, 0xFFFF00);
        graphics.drawString(font, "Direction", cx - 115, cy - 110, 0xFFFFFF);
        graphics.drawString(font, "Bulb", cx - 54, cy - 100, 0xFFFFFF);
        graphics.drawString(font, "F", cx - 11, cy - 100, 0xFFFFFF);
        graphics.drawString(font, "F", cx + 26, cy - 100, 0xFFFFFF);

        blitBlock(graphics, "redstone_torch", cx - 30, cy - 106);
        blitBlock(graphics, "redstone_torch_off", cx + 7, cy - 106);
        blitBlock(graphics, "cross", cx - 54, cy + 35);
        blitBlock(graphics, "dontcross", cx - 54, cy + 55);
        animateSpecialBulbs(graphics, cx, cy);
        blitBlock(graphics, "yellow_arrow_right", cx - 54, cy + 95);
        blitBlock(graphics, "green_arrow_right", cx - 54, cy + 115);

        renderHover(graphics, mouseX, mouseY, cx, cy);
    }

    private void renderAutomaticOverlay(GuiGraphics graphics, int cx, int cy) {
        int left = cx - 54;
        graphics.drawString(font, "Automatic Mode", left, cy - 115, 0xFFFF00);
        graphics.drawString(font, "Direction", cx - 115, cy - 110, 0xFFFFFF);

        drawAutoLabel(graphics, left, greenMinimumNS, greenMinimumEW,
                "Green Minimum (0 = always use Max; otherwise used if no sensors trip)");
        drawAutoLabel(graphics, left, greenMaxNS, greenMaxEW, "Green Max");
        drawAutoLabel(graphics, left, yellowTimeNS, yellowTimeEW, "Yellow Time");
        drawAutoLabel(graphics, left, redTimeNS, redTimeEW, "Red Time");
        drawAutoLabel(graphics, left, arrowMinimumNS, arrowMinimumEW,
                "Left Arrow Min (0 = always use Max; otherwise used if no sensors trip)");
        drawAutoLabel(graphics, left, arrowMaxNS, arrowMaxEW, "Left Arrow Max");
        drawAutoLabel(graphics, left, crossTime, null, "Cross Time");
        drawAutoLabel(graphics, left, crossWarningTime, null, "Cross Warning Time");
        drawAutoLabel(graphics, left, rightArrowMinimum, null, "Right Arrow");
    }

    private void drawAutoLabel(GuiGraphics graphics, int left, EditBox primary, EditBox alternate, String text) {
        EditBox field = primary != null && primary.visible ? primary
                : alternate != null && alternate.visible ? alternate : primary;
        if (field == null) {
            return;
        }
        graphics.drawString(font, text, left, field.getY() - 12, 0xFFFFFF);
    }

    private void animateSpecialBulbs(GuiGraphics graphics, int cx, int cy) {
        tickCounter++;
        if (tickCounter >= 180) {
            tickCounter = 0;
            isRightRed = !isRightRed;
            isLeftYellow = !isLeftYellow;
            isLeftGreen = !isLeftGreen;
            isRed = !isRed;
            isYellow = !isYellow;
            isGreen = !isGreen;
            leftTurn = switch (leftTurn) {
                case "Red" -> "No";
                case "No" -> "UTurn";
                default -> "Red";
            };
        }
        blitBlock(graphics, isRightRed ? "red_arrow_right" : "no_right_turn", cx - 54, cy + 75);
        blitBlock(graphics, switch (leftTurn) {
            case "No" -> "no_left_turn";
            case "UTurn" -> "red_arrow_uturn";
            default -> "red_arrow_left";
        }, cx - 54, cy - 25);
        blitBlock(graphics, isLeftYellow ? "yellow_arrow_left" : "yellow_arrow_uturn", cx - 54, cy - 5);
        blitBlock(graphics, isLeftGreen ? "green_arrow_left" : "green_arrow_uturn", cx - 54, cy + 15);
        blitBlock(graphics, isRed ? "red_solid" : "straight_red", cx - 54, cy - 85);
        blitBlock(graphics, isYellow ? "yellow_solid" : "straight_yellow", cx - 54, cy - 65);
        blitBlock(graphics, isGreen ? "green" : "straight_green", cx - 54, cy - 45);
    }

    private void blitBlock(GuiGraphics graphics, String texture, int x, int y) {
        RTCGuiTextures.blitBlock(graphics, texture, x, y);
    }

    private void renderHover(GuiGraphics graphics, int mouseX, int mouseY, int cx, int cy) {
        if (hover(mouseX, mouseY, cx - 54, cy + 75)) {
            graphics.renderTooltip(font, Component.literal("Right Arrow Red And No Right Turn"), mouseX, mouseY);
        } else if (hover(mouseX, mouseY, cx - 54, cy - 25)) {
            graphics.renderTooltip(font, Component.literal("Left Arrow Red, No Left Turn, And U-Turn Arrow Red"), mouseX, mouseY);
        } else if (hover(mouseX, mouseY, cx - 54, cy - 5)) {
            graphics.renderTooltip(font, Component.literal("Left Arrow Yellow and U-Turn Arrow Yellow"), mouseX, mouseY);
        } else if (hover(mouseX, mouseY, cx - 54, cy + 15)) {
            graphics.renderTooltip(font, Component.literal("Left Arrow Green and U-Turn Arrow Green"), mouseX, mouseY);
        } else if (hover(mouseX, mouseY, cx - 54, cy - 85)) {
            graphics.renderTooltip(font, Component.literal("Solid Red and Striaght Arrow Red"), mouseX, mouseY);
        } else if (hover(mouseX, mouseY, cx - 54, cy - 65)) {
            graphics.renderTooltip(font, Component.literal("Solid Yellow and Striaght Arrow Yellow"), mouseX, mouseY);
        } else if (hover(mouseX, mouseY, cx - 54, cy - 45)) {
            graphics.renderTooltip(font, Component.literal("Solid Green and Striaght Arrow Green"), mouseX, mouseY);
        }
    }

    private static boolean hover(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 16;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentMode == Mode.AUTOMATIC) {
            TrafficLightControlBoxBlockEntity box = getBox();
            if (box != null) {
                var auto = box.getAutomator();
                if (editingNorthSouth) {
                    bindAutoField(greenMinimumNS, auto::setGreenMinimumNS);
                    bindAutoField(greenMaxNS, auto::setGreenMaxNS);
                    bindAutoField(arrowMinimumNS, auto::setArrowMinimumNS);
                    bindAutoField(arrowMaxNS, auto::setArrowMaxNS);
                    bindAutoField(yellowTimeNS, auto::setYellowTimeNS);
                    bindAutoField(redTimeNS, auto::setRedTimeNS);
                } else {
                    bindAutoField(greenMinimumEW, auto::setGreenMinimumEW);
                    bindAutoField(greenMaxEW, auto::setGreenMaxEW);
                    bindAutoField(arrowMinimumEW, auto::setArrowMinimumEW);
                    bindAutoField(arrowMaxEW, auto::setArrowMaxEW);
                    bindAutoField(yellowTimeEW, auto::setYellowTimeEW);
                    bindAutoField(redTimeEW, auto::setRedTimeEW);
                }
                bindAutoField(crossTime, auto::setCrossTime);
                bindAutoField(crossWarningTime, auto::setCrossWarningTime);
                bindAutoField(rightArrowMinimum, auto::setRightArrowTime);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void bindAutoField(EditBox field, Consumer<Double> setter) {
        if (field != null && field.isFocused()) {
            try {
                setter.accept(field.getValue().isEmpty() ? 0.0 : Double.parseDouble(field.getValue()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public void onClose() {
        TrafficLightControlBoxBlockEntity box = getBox();
        if (box != null) {
            box.performClientToServerSync();
            if (minecraft.player != null) {
                BlockPos pos = menu.getBlockPos();
                minecraft.player.displayClientMessage(Component.literal(
                        "[Realistic Traffic Control] Control Box " + pos.getX() + "," + pos.getY() + "," + pos.getZ()
                                + " Was Saved."), false);
            }
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class ManualCheckboxBinding {
        private final EnumTrafficLightBulbTypes[] types;
        private ManualBulbCheckbox on;
        private ManualBulbCheckbox onFlash;
        private ManualBulbCheckbox off;
        private ManualBulbCheckbox offFlash;

        private ManualCheckboxBinding(EnumTrafficLightBulbTypes[] types) {
            this.types = types;
        }
    }
}
