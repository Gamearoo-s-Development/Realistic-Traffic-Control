package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ModNetworkHandler;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketSetApproachMovement;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleApproachEnabled;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleFyaNightOnly;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleHawkBeacon;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleMain;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleNightFlash;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleSplitAxis;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleSplitDirections;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementSettings;
import com.gamearoosdevelopment.realistictrafficcontrol.util.IdleBulbState;

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
	private static final int ID_STRAIGHT_IDLE = 30;
	private static final int ID_LEFT_IDLE = 31;
	private static final int ID_RIGHT_IDLE = 32;

	private static final int ROW_HEIGHT = 22;
	private static final int BTN_HEIGHT = 18;

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

		int approachX = cx - 214;
		int enabledX = cx - 182;
		int idleX = cx - 96;
		int rowY = cy - 78;

		int systemCol1X = cx + 8;
		int systemCol2X = cx + 150;
		int systemStep = ROW_HEIGHT;

		buttonList.add(new GuiButton(ID_BACK, cx - 40, cy + 112, 80, 20, "Back"));

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

		buttonList.add(new GuiButtonToggleIdle(ID_STRAIGHT_IDLE, idleX, rowY, 74, BTN_HEIGHT, "Straight", settings.straightIdle));
		buttonList.add(new GuiButtonToggleIdle(ID_LEFT_IDLE, idleX, rowY + ROW_HEIGHT, 74, BTN_HEIGHT, "Left", settings.leftIdle));
		buttonList.add(new GuiButtonToggleIdle(ID_RIGHT_IDLE, idleX, rowY + ROW_HEIGHT * 2, 74, BTN_HEIGHT, "Right", settings.rightIdle));

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
		buttonList.add(new GuiButtonToggleFyaNightOnly(9007, systemCol2X, rowY + systemStep * 4, 25, BTN_HEIGHT, te.isFyaNightOnlyEnabled()));
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
			} else if (button.id == ID_STRAIGHT_IDLE && button instanceof GuiButtonToggleIdle) {
				((GuiButtonToggleIdle) button).setState(settings.straightIdle);
			} else if (button.id == ID_LEFT_IDLE && button instanceof GuiButtonToggleIdle) {
				((GuiButtonToggleIdle) button).setState(settings.leftIdle);
			} else if (button.id == ID_RIGHT_IDLE && button instanceof GuiButtonToggleIdle) {
				((GuiButtonToggleIdle) button).setState(settings.rightIdle);
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
				if (button instanceof GuiButtonToggleMovement) {
					GuiButtonToggleMovement toggle = (GuiButtonToggleMovement) button;
					toggle.toggle();
					ApproachMovementSettings settings = te.getMovementSettings(selectedApproach);
					if (button.id == ID_STRAIGHT) {
						settings.straightEnabled = toggle.isToggled();
					} else if (button.id == ID_LEFT) {
						settings.leftEnabled = toggle.isToggled();
					} else {
						settings.rightEnabled = toggle.isToggled();
					}
					te.setMovementSettings(selectedApproach, settings);
					syncSettingsToServer();
				}
				return;
			case ID_STRAIGHT_IDLE:
			case ID_LEFT_IDLE:
			case ID_RIGHT_IDLE:
				if (button instanceof GuiButtonToggleIdle) {
					GuiButtonToggleIdle toggle = (GuiButtonToggleIdle) button;
					toggle.toggle();
					ApproachMovementSettings idleSettings = te.getMovementSettings(selectedApproach);
					if (button.id == ID_STRAIGHT_IDLE) {
						idleSettings.straightIdle = toggle.getState();
					} else if (button.id == ID_LEFT_IDLE) {
						idleSettings.leftIdle = toggle.getState();
					} else {
						idleSettings.rightIdle = toggle.getState();
					}
					te.setMovementSettings(selectedApproach, idleSettings);
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
		if (button.id == 9007 && button instanceof GuiButtonToggleFyaNightOnly) {
			GuiButtonToggleFyaNightOnly toggle = (GuiButtonToggleFyaNightOnly) button;
			toggle.toggle();
			boolean enabled = toggle.isToggled();
			te.setFyaNightOnlyEnabled(enabled);
			ModNetworkHandler.INSTANCE.sendToServer(new PacketToggleFyaNightOnly(te.getPos(), enabled));
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

		drawString(fontRenderer, "Movement (per approach)", cx - 214, cy - 100, 0xFFFF55);
		drawString(fontRenderer, "Dir", cx - 214, cy - 88, 0xAAAAAA);
		drawString(fontRenderer, "Enabled", cx - 182, cy - 88, 0xAAAAAA);
		drawString(fontRenderer, "Idle if OFF", cx - 96, cy - 88, 0xAAAAAA);

		drawString(fontRenderer, "System Options", cx + 8, cy - 100, 0xFFFF55);
		drawString(fontRenderer, "Modes", cx + 8, cy - 88, 0xAAAAAA);
		drawString(fontRenderer, "Approach", cx + 150, cy - 88, 0xAAAAAA);

		drawCenteredString(fontRenderer, "Idle Red = stay red when movement OFF  |  Idle Green = stay green", cx, cy + 92, 0x888888);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
