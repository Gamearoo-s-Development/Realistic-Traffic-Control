package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.ControlBoxWidgets;
import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.ControlBoxWidgets.FyaToggle;
import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.ControlBoxWidgets.IdleToggle;
import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.ControlBoxWidgets.LabeledToggle;
import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.ControlBoxWidgets.MovementToggle;
import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.ControlBoxWidgets.SelectableTab;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.TrafficLightControlBoxMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.network.SetApproachMovementPayload;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ToggleApproachEnabledPayload;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ToggleHawkBeaconPayload;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ToggleMainPayload;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ToggleNightFlashPayload;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ToggleSplitAxisPayload;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ToggleSplitDirectionsPayload;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementSettings;
import com.gamearoosdevelopment.realistictrafficcontrol.util.FyaMode;
import com.gamearoosdevelopment.realistictrafficcontrol.util.IdleBulbMode;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Port of 1.12.2 {@code TrafficLightControlBoxAdvancedGui}: per-approach movement toggles and system options.
 */
public class TrafficLightControlBoxAdvancedScreen extends AbstractContainerScreen<TrafficLightControlBoxMenu> {

    private static final int ID_STRAIGHT = 20;
    private static final int ID_LEFT = 21;
    private static final int ID_RIGHT = 22;
    private static final int ID_SHARED_TURNS = 23;
    private static final int ID_STRAIGHT_IDLE = 30;
    private static final int ID_LEFT_IDLE = 31;
    private static final int ID_RIGHT_IDLE = 32;
    private static final int ID_FYA_LEFT = 40;
    private static final int ID_FYA_RIGHT = 41;
    private static final int ROW_HEIGHT = 22;

    private final TrafficLightControlBoxScreen parent;
    private Direction selectedApproach = Direction.NORTH;
    private SelectableTab approachNorth;
    private SelectableTab approachSouth;
    private SelectableTab approachEast;
    private SelectableTab approachWest;
    private MovementToggle straightToggle;
    private MovementToggle leftToggle;
    private MovementToggle rightToggle;
    private LabeledToggle sharedTurnsToggle;
    private IdleToggle straightIdleToggle;
    private IdleToggle leftIdleToggle;
    private IdleToggle rightIdleToggle;
    private FyaToggle leftFyaToggle;
    private FyaToggle rightFyaToggle;

    public TrafficLightControlBoxAdvancedScreen(TrafficLightControlBoxScreen parent, TrafficLightControlBoxMenu menu,
            Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.parent = parent;
        this.imageWidth = 1;
        this.imageHeight = 1;
    }

    private TrafficLightControlBoxBlockEntity getBox() {
        return menu.getControlBox(minecraft.player);
    }

    private BlockPos pos() {
        return menu.getBlockPos();
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        TrafficLightControlBoxBlockEntity box = getBox();
        if (box == null) {
            return;
        }

        int cx = width / 2;
        int cy = height / 2;
        int approachX = cx - 214;
        int enabledX = cx - 182;
        int idleX = cx - 96;
        int fyaX = cx - 18;
        int rowY = cy - 78;
        int systemCol1X = cx + 8;
        int systemCol2X = cx + 150;

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.setScreen(parent))
                .bounds(cx - 40, cy + 112, 80, 20).build());

        approachNorth = addTab(approachX, rowY, "N", Direction.NORTH);
        approachSouth = addTab(approachX, rowY + ROW_HEIGHT, "S", Direction.SOUTH);
        approachEast = addTab(approachX, rowY + ROW_HEIGHT * 2, "E", Direction.EAST);
        approachWest = addTab(approachX, rowY + ROW_HEIGHT * 3, "W", Direction.WEST);
        updateApproachSelection();

        ApproachMovementSettings settings = box.getMovementSettings(selectedApproach);
        straightToggle = addMovement(ID_STRAIGHT, enabledX, rowY, "Straight", settings.straightEnabled);
        leftToggle = addMovement(ID_LEFT, enabledX, rowY + ROW_HEIGHT, "Left", settings.leftEnabled);
        rightToggle = addMovement(ID_RIGHT, enabledX, rowY + ROW_HEIGHT * 2, "Right", settings.rightEnabled);
        sharedTurnsToggle = addSharedTurns(enabledX, rowY + ROW_HEIGHT * 3, settings.sharedTurns);
        straightIdleToggle = addIdle(ID_STRAIGHT_IDLE, idleX, rowY, "Straight", settings.straightIdle, true);
        leftIdleToggle = addIdle(ID_LEFT_IDLE, idleX, rowY + ROW_HEIGHT, "Left", settings.leftIdle, false);
        rightIdleToggle = addIdle(ID_RIGHT_IDLE, idleX, rowY + ROW_HEIGHT * 2, "Right", settings.rightIdle, false);
        leftFyaToggle = addFya(ID_FYA_LEFT, fyaX, rowY + ROW_HEIGHT, "Left", settings.leftFya);
        rightFyaToggle = addFya(ID_FYA_RIGHT, fyaX, rowY + ROW_HEIGHT * 2, "Right", settings.rightFya);
        straightIdleToggle.setMovementEnabled(settings.straightEnabled);
        leftIdleToggle.setMovementEnabled(settings.leftEnabled);
        rightIdleToggle.setMovementEnabled(settings.rightEnabled);
        refreshMovementButtons();

        addSystemToggle(systemCol1X, rowY, box.isNightFlashEnabled(),
                on -> new ToggleNightFlashPayload(pos(), on), box::setNightFlashEnabled,
                s -> s ? "Night Flash: ON" : "Night Flash: OFF");
        addSystemToggle(systemCol1X, rowY + ROW_HEIGHT, box.isNorthMainEnabled(),
                on -> new ToggleMainPayload(pos(), on), box::setNorthMainEnabled,
                s -> s ? "Main Street: North" : "Main Street: West");
        addSystemToggle(systemCol1X, rowY + ROW_HEIGHT * 2, box.isHawkBeaconEnabled(),
                on -> new ToggleHawkBeaconPayload(pos(), on), box::setHawkBeaconEnabled,
                s -> s ? "HAWK: ON" : "HAWK: OFF");
        addSystemToggle(systemCol1X, rowY + ROW_HEIGHT * 3, box.isSplitDirectionsEnabled(),
                on -> new ToggleSplitDirectionsPayload(pos(), on), box::setSplitDirectionsEnabled,
                s -> s ? "Split Dir: ON" : "Split Dir: OFF");
        addSystemToggle(systemCol1X, rowY + ROW_HEIGHT * 4, box.isSplitNorthSouthEnabled(),
                on -> new ToggleSplitAxisPayload(pos(), ToggleSplitAxisPayload.AXIS_NS, on),
                box::setSplitNorthSouthEnabled, s -> s ? "Split NS: ON" : "Split NS: OFF");
        addSystemToggle(systemCol1X, rowY + ROW_HEIGHT * 5, box.isSplitWestEastEnabled(),
                on -> new ToggleSplitAxisPayload(pos(), ToggleSplitAxisPayload.AXIS_EW, on),
                box::setSplitWestEastEnabled, s -> s ? "Split EW: ON" : "Split EW: OFF");

        addApproachToggle(systemCol2X, rowY, Direction.NORTH, box.hasNorth);
        addApproachToggle(systemCol2X, rowY + ROW_HEIGHT, Direction.SOUTH, box.hasSouth);
        addApproachToggle(systemCol2X, rowY + ROW_HEIGHT * 2, Direction.EAST, box.hasEast);
        addApproachToggle(systemCol2X, rowY + ROW_HEIGHT * 3, Direction.WEST, box.hasWest);
    }

    private SelectableTab addTab(int x, int y, String label, Direction facing) {
        SelectableTab tab = new SelectableTab(x, y, 25, 18, label, () -> selectApproach(facing));
        return addRenderableWidget(tab);
    }

    private void selectApproach(Direction facing) {
        selectedApproach = facing;
        updateApproachSelection();
        refreshMovementButtons();
    }

    private MovementToggle addMovement(int id, int x, int y, String label, boolean initial) {
        MovementToggle toggle = new MovementToggle(x, y, 80, 18, label, initial);
        toggle.setOnToggle(() -> {
            TrafficLightControlBoxBlockEntity box = getBox();
            if (box == null) {
                return;
            }
            ApproachMovementSettings settings = box.getMovementSettings(selectedApproach);
            if (id == ID_STRAIGHT) {
                settings.straightEnabled = straightToggle.isToggled();
                straightIdleToggle.setMovementEnabled(settings.straightEnabled);
            } else if (id == ID_LEFT) {
                settings.leftEnabled = leftToggle.isToggled();
                leftIdleToggle.setMovementEnabled(settings.leftEnabled);
            } else {
                settings.rightEnabled = rightToggle.isToggled();
                rightIdleToggle.setMovementEnabled(settings.rightEnabled);
            }
            box.setMovementSettings(selectedApproach, settings);
            syncMovement(settings);
        });
        return addRenderableWidget(toggle);
    }

    private LabeledToggle addSharedTurns(int x, int y, boolean initial) {
        LabeledToggle toggle = new LabeledToggle(x, y, 25, 18, initial,
                on -> on ? "Shared: ON" : "Shared: OFF");
        toggle.setOnToggle(() -> {
            TrafficLightControlBoxBlockEntity box = getBox();
            if (box == null) {
                return;
            }
            ApproachMovementSettings settings = box.getMovementSettings(selectedApproach);
            settings.sharedTurns = toggle.isToggled();
            box.setMovementSettings(selectedApproach, settings);
            syncMovement(settings);
        });
        return addRenderableWidget(toggle);
    }

    private IdleToggle addIdle(int id, int x, int y, String label, IdleBulbMode initial, boolean straightOnly) {
        IdleToggle toggle = new IdleToggle(x, y, 74, 18, label, initial, straightOnly);
        toggle.setOnToggle(() -> {
            TrafficLightControlBoxBlockEntity box = getBox();
            if (box == null) {
                return;
            }
            ApproachMovementSettings settings = box.getMovementSettings(selectedApproach);
            if (id == ID_STRAIGHT_IDLE) {
                settings.straightIdle = straightIdleToggle.getState();
            } else if (id == ID_LEFT_IDLE) {
                settings.leftIdle = leftIdleToggle.getState();
            } else {
                settings.rightIdle = rightIdleToggle.getState();
            }
            box.setMovementSettings(selectedApproach, settings);
            syncMovement(settings);
        });
        return addRenderableWidget(toggle);
    }

    private FyaToggle addFya(int id, int x, int y, String label, FyaMode initial) {
        FyaToggle toggle = new FyaToggle(x, y, 74, 18, label, initial);
        toggle.setOnToggle(() -> {
            TrafficLightControlBoxBlockEntity box = getBox();
            if (box == null) {
                return;
            }
            ApproachMovementSettings settings = box.getMovementSettings(selectedApproach);
            if (id == ID_FYA_LEFT) {
                settings.leftFya = toggle.getMode();
            } else {
                settings.rightFya = toggle.getMode();
            }
            box.setMovementSettings(selectedApproach, settings);
            syncMovement(settings);
        });
        return addRenderableWidget(toggle);
    }

    private void addSystemToggle(int x, int y, boolean initial,
            java.util.function.Function<Boolean, net.minecraft.network.protocol.common.custom.CustomPacketPayload> packetFn,
            java.util.function.Consumer<Boolean> clientUpdate,
            java.util.function.Function<Boolean, String> labelFn) {
        LabeledToggle toggle = new LabeledToggle(x, y, 25, 18, initial, labelFn);
        toggle.setOnToggle(() -> {
            boolean enabled = toggle.isToggled();
            clientUpdate.accept(enabled);
            PacketDistributor.sendToServer(packetFn.apply(enabled));
        });
        addRenderableWidget(toggle);
    }

    private void addApproachToggle(int x, int y, Direction facing, boolean initial) {
        String shortDir = switch (facing) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST -> "E";
            case WEST -> "W";
            default -> "?";
        };
        LabeledToggle toggle = new LabeledToggle(x, y, 25, 18, initial,
                on -> shortDir + ": " + (on ? "ON" : "OFF"));
        toggle.setOnToggle(() -> {
            boolean enabled = toggle.isToggled();
            TrafficLightControlBoxBlockEntity box = getBox();
            if (box == null) {
                return;
            }
            switch (facing) {
                case NORTH -> box.setNorth(enabled);
                case SOUTH -> box.setSouth(enabled);
                case EAST -> box.setEast(enabled);
                case WEST -> box.setWest(enabled);
                default -> {
                }
            }
            PacketDistributor.sendToServer(new ToggleApproachEnabledPayload(pos(), facing, enabled));
        });
        addRenderableWidget(toggle);
    }

    private void syncMovement(ApproachMovementSettings settings) {
        PacketDistributor.sendToServer(new SetApproachMovementPayload(pos(), selectedApproach, settings));
    }

    private void updateApproachSelection() {
        if (approachNorth != null) {
            approachNorth.setSelected(selectedApproach == Direction.NORTH);
            approachSouth.setSelected(selectedApproach == Direction.SOUTH);
            approachEast.setSelected(selectedApproach == Direction.EAST);
            approachWest.setSelected(selectedApproach == Direction.WEST);
        }
    }

    private void refreshMovementButtons() {
        TrafficLightControlBoxBlockEntity box = getBox();
        if (box == null || straightToggle == null) {
            return;
        }
        ApproachMovementSettings settings = box.getMovementSettings(selectedApproach);
        straightToggle.setToggled(settings.straightEnabled);
        leftToggle.setToggled(settings.leftEnabled);
        rightToggle.setToggled(settings.rightEnabled);
        sharedTurnsToggle.setToggled(settings.sharedTurns);
        straightIdleToggle.setState(settings.straightIdle);
        leftIdleToggle.setState(settings.leftIdle);
        rightIdleToggle.setState(settings.rightIdle);
        leftFyaToggle.setMode(settings.leftFya);
        rightFyaToggle.setMode(settings.rightFya);
        straightIdleToggle.setMovementEnabled(settings.straightEnabled);
        leftIdleToggle.setMovementEnabled(settings.leftEnabled);
        rightIdleToggle.setMovementEnabled(settings.rightEnabled);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        parent.renderBackgroundPanel(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, net.minecraft.world.inventory.Slot slot) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;
        graphics.drawCenteredString(font, "Advanced Options", cx, cy - 108, 0xFFFF00);
        graphics.drawString(font, "Movement (per approach)", cx - 214, cy - 100, 0xFFFF55);
        graphics.drawString(font, "Dir", cx - 214, cy - 88, 0xAAAAAA);
        graphics.drawString(font, "Enabled", cx - 182, cy - 88, 0xAAAAAA);
        graphics.drawString(font, "Idle if OFF", cx - 96, cy - 88, 0xAAAAAA);
        graphics.drawString(font, "FYA", cx - 18, cy - 88, 0xAAAAAA);
        graphics.drawString(font, "System Options", cx + 8, cy - 100, 0xFFFF55);
        graphics.drawString(font, "Modes", cx + 8, cy - 88, 0xAAAAAA);
        graphics.drawString(font, "Approach", cx + 150, cy - 88, 0xAAAAAA);
        graphics.drawCenteredString(font, "Idle if OFF: only applies when movement is disabled (grey = ON)",
                cx, cy + 74, 0x888888);
        graphics.drawCenteredString(font, "Shared Turns: left + U-turn + right arrows with straight (this approach only)",
                cx, cy + 86, 0x888888);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
