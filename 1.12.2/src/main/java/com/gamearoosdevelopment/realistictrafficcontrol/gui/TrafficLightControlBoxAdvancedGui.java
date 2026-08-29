package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ModNetworkHandler;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketSetApproachMovement;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleApproachEnabled;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleHawkBeacon;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleMain;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleNightFlash;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleSplitAxis;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleSplitDirections;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementSettings;
import com.gamearoosdevelopment.realistictrafficcontrol.util.FyaMode;
import com.gamearoosdevelopment.realistictrafficcontrol.util.IdleBulbMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class TrafficLightControlBoxAdvancedGui extends GuiScreen {
	private static final int ID_BACK = 1;
	private static final int ID_APPROACH_N = 10;
	private static final int ID_APPROACH_S = 11;
	private static final int ID_APPROACH_E = 12;
	private static final int ID_APPROACH_W = 13;
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
	private static final int BTN_HEIGHT = 18;
	private static final int COL_APPROACH = -214;
	private static final int COL_ENABLED = -182;
	private static final int COL_IDLE = -96;
	private static final int COL_FYA = -18;
	private static final int COL_FYA_WIDTH = 74;
	private static final int COL_SYSTEM = COL_FYA + COL_FYA_WIDTH + 10;
	private static final int COL_APPROACH_TOGGLE = COL_SYSTEM + 146;

	private final TrafficLightControlBoxGui parent;
	private TrafficLightControlBoxTileEntity te;
	private final EntityPlayer player;
	private final ResourceLocation background = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/gui/control_box_gui.png");

	private EnumFacing selectedApproach = EnumFacing.NORTH;
	private GuiButtonExtSelectable approachNorth;
	private GuiButtonExtSelectable approachSouth;
	private GuiButtonExtSelectable approachEast;
	private GuiButtonExtSelectable approachWest;

	public TrafficLightControlBoxAdvancedGui(TrafficLightControlBoxGui parent, TrafficLightControlBoxTileEntity te, EntityPlayer player) {
		this.parent = parent;
		this.te = te;
		this.player = player;
	}

	@Override
	public void initGui() {
		super.initGui();
		buttonList.clear();
		te = refreshTileEntity();
		if (te == null) {
			return;
		}

		int cx = width / 2;
		int cy = height / 2;

		int approachX = cx + COL_APPROACH;
		int enabledX = cx + COL_ENABLED;
		int idleX = cx + COL_IDLE;
		int fyaX = cx + COL_FYA;
		int rowY = cy - 78;

		int systemCol1X = cx + COL_SYSTEM;
		int systemCol2X = cx + COL_APPROACH_TOGGLE;
		int systemStep = ROW_HEIGHT;

		buttonList.add(new GuiButton(ID_BACK, cx - 40, cy + 124, 80, 20, "Back"));

		approachNorth = new GuiButtonExtSelectable(ID_APPROACH_N, approachX, rowY, 25, BTN_HEIGHT, "N");
		approachSouth = new GuiButtonExtSelectable(ID_APPROACH_S, approachX, rowY + ROW_HEIGHT, 25, BTN_HEIGHT, "S");
		approachEast = new GuiButtonExtSelectable(ID_APPROACH_E, approachX, rowY + ROW_HEIGHT * 2, 25, BTN_HEIGHT, "E");
		approachWest = new GuiButtonExtSelectable(ID_APPROACH_W, approachX, rowY + ROW_HEIGHT * 3, 25, BTN_HEIGHT, "W");
		buttonList.add(approachNorth);
		buttonList.add(approachSouth);
		buttonList.add(approachEast);
		buttonList.add(approachWest);
		updateApproachSelection();

		ApproachMovementSettings settings = te.getMovementSettings(selectedApproach);
		buttonList.add(new GuiButtonToggleMovement(ID_STRAIGHT, enabledX, rowY, 80, BTN_HEIGHT, "Straight", settings.straightEnabled));
		buttonList.add(new GuiButtonToggleMovement(ID_LEFT, enabledX, rowY + ROW_HEIGHT, 80, BTN_HEIGHT, "Left", settings.leftEnabled));
		buttonList.add(new GuiButtonToggleMovement(ID_RIGHT, enabledX, rowY + ROW_HEIGHT * 2, 80, BTN_HEIGHT, "Right", settings.rightEnabled));
		buttonList.add(new GuiButtonToggleMovement(ID_SHARED_TURNS, enabledX, rowY + ROW_HEIGHT * 3, 80, BTN_HEIGHT,
				sharedTurnsLabel(selectedApproach), settings.sharedTurns));
		buttonList.add(new GuiButtonIdleMode(ID_STRAIGHT_IDLE, idleX, rowY, 90, BTN_HEIGHT, "Straight", settings.straightIdle, true));
		buttonList.add(new GuiButtonIdleMode(ID_LEFT_IDLE, idleX, rowY + ROW_HEIGHT, 90, BTN_HEIGHT, "Left", settings.leftIdle, false));
		buttonList.add(new GuiButtonIdleMode(ID_RIGHT_IDLE, idleX, rowY + ROW_HEIGHT * 2, 90, BTN_HEIGHT, "Right", settings.rightIdle, false));

		buttonList.add(new GuiButtonFyaMode(ID_FYA_LEFT, fyaX, rowY + ROW_HEIGHT, 74, BTN_HEIGHT, "Left", settings.leftFya));
		buttonList.add(new GuiButtonFyaMode(ID_FYA_RIGHT, fyaX, rowY + ROW_HEIGHT * 2, 74, BTN_HEIGHT, "Right", settings.rightFya));

		buttonList.add(new GuiButtonToggle(9001, systemCol1X, rowY, 25, BTN_HEIGHT, te.isNightFlashEnabled()));
		buttonList.add(new GuiButtonToggle2(9002, systemCol1X, rowY + systemStep, 25, BTN_HEIGHT, te.isNorthMainEnabled()));
		buttonList.add(new GuiButtonToggleHawk(9003, systemCol1X, rowY + systemStep * 2, 25, BTN_HEIGHT, te.isHawkBeaconEnabled()));
		buttonList.add(new GuiButtonToggleSplitDirections(9004, systemCol1X, rowY + systemStep * 3, 25, BTN_HEIGHT, te.isSplitDirectionsEnabled()));
		buttonList.add(new GuiButtonToggleSplitNS(9005, systemCol1X, rowY + systemStep * 4, 25, BTN_HEIGHT, te.isSplitNorthSouthEnabled()));
		buttonList.add(new GuiButtonToggleSplitEW(9006, systemCol1X, rowY + systemStep * 5, 25, BTN_HEIGHT, te.isSplitWestEastEnabled()));

		buttonList.add(new GuiButtonToggleApproach(9010, systemCol2X, rowY, 25, BTN_HEIGHT, EnumFacing.NORTH, te.hasNorth));
		buttonList.add(new GuiButtonToggleApproach(9011, systemCol2X, rowY + systemStep, 25, BTN_HEIGHT, EnumFacing.SOUTH, te.hasSouth));
		buttonList.add(new GuiButtonToggleApproach(9012, systemCol2X, rowY + systemStep * 2, 25, BTN_HEIGHT, EnumFacing.EAST, te.hasEast));
		buttonList.add(new GuiButtonToggleApproach(9013, systemCol2X, rowY + systemStep * 3, 25, BTN_HEIGHT, EnumFacing.WEST, te.hasWest));
		refreshMovementButtons();
	}

	private static String sharedTurnsLabel(EnumFacing approach) {
		if (approach == null) {
			return "Shared";
		}
		return "Shared " + approach.getName().substring(0, 1).toUpperCase();
	}

	private TrafficLightControlBoxTileEntity refreshTileEntity() {
		if (te == null) {
			return null;
		}
		return (TrafficLightControlBoxTileEntity) Minecraft.getMinecraft().world.getTileEntity(te.getPos());
	}

	private void updateApproachSelection() {
		approachNorth.setIsSelected(selectedApproach == EnumFacing.NORTH);
		approachSouth.setIsSelected(selectedApproach == EnumFacing.SOUTH);
		approachEast.setIsSelected(selectedApproach == EnumFacing.EAST);
		approachWest.setIsSelected(selectedApproach == EnumFacing.WEST);
	}

	private void refreshMovementButtons() {
		ApproachMovementSettings settings = te.getMovementSettings(selectedApproach);
		for (GuiButton button : buttonList) {
			if (button.id == ID_STRAIGHT && button instanceof GuiButtonToggleMovement) {
				((GuiButtonToggleMovement) button).setToggled(settings.straightEnabled);
			} else if (button.id == ID_LEFT && button instanceof GuiButtonToggleMovement) {
				((GuiButtonToggleMovement) button).setToggled(settings.leftEnabled);
			} else if (button.id == ID_RIGHT && button instanceof GuiButtonToggleMovement) {
				((GuiButtonToggleMovement) button).setToggled(settings.rightEnabled);
			} else if (button.id == ID_SHARED_TURNS && button instanceof GuiButtonToggleMovement) {
				GuiButtonToggleMovement shared = (GuiButtonToggleMovement) button;
				shared.setToggled(settings.sharedTurns);
				shared.setMovementLabel(sharedTurnsLabel(selectedApproach));
			} else if (button.id == ID_STRAIGHT_IDLE && button instanceof GuiButtonIdleMode) {
				GuiButtonIdleMode idleButton = (GuiButtonIdleMode) button;
				idleButton.setMode(settings.straightIdle);
				idleButton.setMovementEnabled(settings.straightEnabled);
			} else if (button.id == ID_LEFT_IDLE && button instanceof GuiButtonIdleMode) {
				GuiButtonIdleMode idleButton = (GuiButtonIdleMode) button;
				idleButton.setMode(settings.leftIdle);
				idleButton.setMovementEnabled(settings.leftEnabled);
			} else if (button.id == ID_RIGHT_IDLE && button instanceof GuiButtonIdleMode) {
				GuiButtonIdleMode idleButton = (GuiButtonIdleMode) button;
				idleButton.setMode(settings.rightIdle);
				idleButton.setMovementEnabled(settings.rightEnabled);
			} else if (button.id == ID_FYA_LEFT && button instanceof GuiButtonFyaMode) {
				((GuiButtonFyaMode) button).setMode(settings.leftFya);
			} else if (button.id == ID_FYA_RIGHT && button instanceof GuiButtonFyaMode) {
				((GuiButtonFyaMode) button).setMode(settings.rightFya);
			}
		}
	}

	private void syncSettingsToServer() {
		ApproachMovementSettings settings = te.getMovementSettings(selectedApproach);
		te.setMovementSettings(selectedApproach, settings);
		ModNetworkHandler.INSTANCE.sendToServer(new PacketSetApproachMovement(te.getPos(), selectedApproach, settings));
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == ID_BACK) {
			Minecraft.getMinecraft().displayGuiScreen(parent);
			return;
		}

		if (handleSystemToggle(button)) {
			return;
		}

		switch (button.id) {
			case ID_APPROACH_N:
				selectedApproach = EnumFacing.NORTH;
				updateApproachSelection();
				refreshMovementButtons();
				return;
			case ID_APPROACH_S:
				selectedApproach = EnumFacing.SOUTH;
				updateApproachSelection();
				refreshMovementButtons();
				return;
			case ID_APPROACH_E:
				selectedApproach = EnumFacing.EAST;
				updateApproachSelection();
				refreshMovementButtons();
				return;
			case ID_APPROACH_W:
				selectedApproach = EnumFacing.WEST;
				updateApproachSelection();
				refreshMovementButtons();
				return;
			case ID_STRAIGHT:
			case ID_LEFT:
			case ID_RIGHT:
			case ID_SHARED_TURNS:
				if (button instanceof GuiButtonToggleMovement) {
					GuiButtonToggleMovement toggle = (GuiButtonToggleMovement) button;
					toggle.toggle();
					ApproachMovementSettings settings = te.getMovementSettings(selectedApproach);
					if (button.id == ID_STRAIGHT) {
						settings.straightEnabled = toggle.isToggled();
					} else if (button.id == ID_LEFT) {
						settings.leftEnabled = toggle.isToggled();
					} else if (button.id == ID_RIGHT) {
						settings.rightEnabled = toggle.isToggled();
					} else {
						settings.sharedTurns = toggle.isToggled();
					}
					te.setMovementSettings(selectedApproach, settings);
					syncSettingsToServer();
					refreshMovementButtons();
				}
				return;
			case ID_STRAIGHT_IDLE:
			case ID_LEFT_IDLE:
			case ID_RIGHT_IDLE:
				if (button instanceof GuiButtonIdleMode) {
					GuiButtonIdleMode toggle = (GuiButtonIdleMode) button;
					if (toggle.isMovementEnabled()) {
						return;
					}
					toggle.cycle();
					ApproachMovementSettings idleSettings = te.getMovementSettings(selectedApproach);
					if (button.id == ID_STRAIGHT_IDLE) {
						idleSettings.straightIdle = toggle.getMode();
					} else if (button.id == ID_LEFT_IDLE) {
						idleSettings.leftIdle = toggle.getMode();
					} else {
						idleSettings.rightIdle = toggle.getMode();
					}
					te.setMovementSettings(selectedApproach, idleSettings);
					syncSettingsToServer();
				}
				return;
			case ID_FYA_LEFT:
			case ID_FYA_RIGHT:
				if (button instanceof GuiButtonFyaMode) {
					GuiButtonFyaMode toggle = (GuiButtonFyaMode) button;
					toggle.cycle();
					ApproachMovementSettings fyaSettings = te.getMovementSettings(selectedApproach);
					if (button.id == ID_FYA_LEFT) {
						fyaSettings.leftFya = toggle.getMode();
					} else {
						fyaSettings.rightFya = toggle.getMode();
					}
					te.setMovementSettings(selectedApproach, fyaSettings);
					syncSettingsToServer();
				}
				return;
			default:
				break;
		}
	}

	private boolean handleSystemToggle(GuiButton button) {
		if (button.id == 9001 && button instanceof GuiButtonToggle) {
			GuiButtonToggle toggle = (GuiButtonToggle) button;
			toggle.toggle();
			boolean enabled = toggle.isToggled();
			te.setNightFlashEnabled(enabled);
			ModNetworkHandler.INSTANCE.sendToServer(new PacketToggleNightFlash(te.getPos(), enabled));
			return true;
		}
		if (button.id == 9002 && button instanceof GuiButtonToggle2) {
			GuiButtonToggle2 toggle = (GuiButtonToggle2) button;
			toggle.toggle();
			boolean enabled = toggle.isToggled();
			te.setNorthMainEnabled(enabled);
			ModNetworkHandler.INSTANCE.sendToServer(new PacketToggleMain(te.getPos(), enabled));
			return true;
		}
		if (button.id == 9003 && button instanceof GuiButtonToggleHawk) {
			GuiButtonToggleHawk toggle = (GuiButtonToggleHawk) button;
			toggle.toggle();
			boolean enabled = toggle.isToggled();
			te.setHawkBeaconEnabled(enabled);
			ModNetworkHandler.INSTANCE.sendToServer(new PacketToggleHawkBeacon(te.getPos(), enabled));
			return true;
		}
		if (button.id == 9004 && button instanceof GuiButtonToggleSplitDirections) {
			GuiButtonToggleSplitDirections toggle = (GuiButtonToggleSplitDirections) button;
			toggle.toggle();
			boolean enabled = toggle.isToggled();
			te.setSplitDirectionsEnabled(enabled);
			ModNetworkHandler.INSTANCE.sendToServer(new PacketToggleSplitDirections(te.getPos(), enabled));
			return true;
		}
		if (button.id == 9005 && button instanceof GuiButtonToggleSplitNS) {
			GuiButtonToggleSplitNS toggle = (GuiButtonToggleSplitNS) button;
			toggle.toggle();
			boolean enabled = toggle.isToggled();
			te.setSplitNorthSouthEnabled(enabled);
			ModNetworkHandler.INSTANCE.sendToServer(new PacketToggleSplitAxis(te.getPos(), PacketToggleSplitAxis.AXIS_NS, enabled));
			return true;
		}
		if (button.id == 9006 && button instanceof GuiButtonToggleSplitEW) {
			GuiButtonToggleSplitEW toggle = (GuiButtonToggleSplitEW) button;
			toggle.toggle();
			boolean enabled = toggle.isToggled();
			te.setSplitWestEastEnabled(enabled);
			ModNetworkHandler.INSTANCE.sendToServer(new PacketToggleSplitAxis(te.getPos(), PacketToggleSplitAxis.AXIS_EW, enabled));
			return true;
		}
		if (button.id >= 9010 && button.id <= 9013 && button instanceof GuiButtonToggleApproach) {
			GuiButtonToggleApproach toggle = (GuiButtonToggleApproach) button;
			toggle.toggle();
			boolean enabled = toggle.isToggled();
			EnumFacing facing = toggle.getFacing();
			switch (facing) {
				case NORTH:
					te.setNorth(enabled);
					break;
				case SOUTH:
					te.setSouth(enabled);
					break;
				case EAST:
					te.setEast(enabled);
					break;
				case WEST:
					te.setWest(enabled);
					break;
				default:
					break;
			}
			ModNetworkHandler.INSTANCE.sendToServer(new PacketToggleApproachEnabled(te.getPos(), facing, enabled));
			return true;
		}
		return false;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();

		int cx = width / 2;
		int cy = height / 2;

		drawCenteredString(fontRenderer, "Advanced Options", cx, cy - 108, 0xFFFF00);

		drawString(fontRenderer, "Movement (per approach)", cx + COL_APPROACH, cy - 100, 0xFFFF55);
		drawString(fontRenderer, "Dir", cx + COL_APPROACH, cy - 88, 0xAAAAAA);
		drawString(fontRenderer, "Enabled", cx + COL_ENABLED, cy - 88, 0xAAAAAA);
		drawString(fontRenderer, "Idle if OFF", cx + COL_IDLE, cy - 88, 0xAAAAAA);
		drawString(fontRenderer, "FYA", cx + COL_FYA, cy - 88, 0xAAAAAA);

		drawString(fontRenderer, "System Options", cx + COL_SYSTEM, cy - 100, 0xFFFF55);
		drawString(fontRenderer, "Modes", cx + COL_SYSTEM, cy - 88, 0xAAAAAA);
		drawString(fontRenderer, "Approach", cx + COL_APPROACH_TOGGLE, cy - 88, 0xAAAAAA);

		drawCenteredString(fontRenderer, "Idle if OFF: only applies when movement is disabled (grey = ON)", cx, cy + 74, 0x888888);
		drawCenteredString(fontRenderer, "Shared Turns: left + U-turn + right arrows with straight (this Dir only)", cx, cy + 86, 0x888888);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
