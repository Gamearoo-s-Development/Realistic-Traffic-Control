package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.io.IOException;
import java.util.function.Consumer;

import org.lwjgl.input.Keyboard;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.GuiButtonToggle;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ModNetworkHandler;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketToggleNightFlash;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiCheckBox;


public class TrafficLightControlBoxGui extends GuiScreen {
	private ResourceLocation background = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/gui/control_box_gui.png");
	private Modes _currentMode = Modes.ManualNorthSouth;
	private final EntityPlayer player;
	private GuiCheckBox greenOn;
	private GuiCheckBox yellowOn;
	private GuiCheckBox redOn;
	private GuiCheckBox greenArrowLeftOn;
	private GuiCheckBox yellowArrowLeftOn;
	private GuiCheckBox redArrowLeftOn;
	private GuiCheckBox greenOff;
	private GuiCheckBox yellowOff;
	private GuiCheckBox redOff;
	private GuiCheckBox greenArrowLeftOff;
	private GuiCheckBox yellowArrowLeftOff;
	private GuiCheckBox redArrowLeftOff;
	private GuiCheckBox crossOn;
	private GuiCheckBox crossOff;
	private GuiCheckBox dontCrossOn;
	private GuiCheckBox dontCrossOff;
	private GuiCheckBox greenArrowRightOn;
	private GuiCheckBox yellowArrowRightOn;
	private GuiCheckBox redArrowRightOn;
	private GuiCheckBox greenArrowRightOff;
	private GuiCheckBox yellowArrowRightOff;
	private GuiCheckBox redArrowRightOff;
	
	private GuiCheckBox greenOnFlash;
	private GuiCheckBox yellowOnFlash;
	private GuiCheckBox redOnFlash;
	private GuiCheckBox greenArrowLeftOnFlash;
	private GuiCheckBox yellowArrowLeftOnFlash;
	private GuiCheckBox redArrowLeftOnFlash;
	private GuiCheckBox greenOffFlash;
	private GuiCheckBox yellowOffFlash;
	private GuiCheckBox redOffFlash;
	private GuiCheckBox greenArrowLeftOffFlash;
	private GuiCheckBox yellowArrowLeftOffFlash;
	private GuiCheckBox redArrowLeftOffFlash;
	private GuiCheckBox crossOnFlash;
	private GuiCheckBox crossOffFlash;
	private GuiCheckBox dontCrossOnFlash;
	private GuiCheckBox dontCrossOffFlash;
	private GuiCheckBox greenArrowRightOnFlash;
	private GuiCheckBox yellowArrowRightOnFlash;
	private GuiCheckBox redArrowRightOnFlash;
	private GuiCheckBox greenArrowRightOffFlash;
	private GuiCheckBox yellowArrowRightOffFlash;
	private GuiCheckBox redArrowRightOffFlash;
	
	
	private GuiButtonExtSelectable manualModeNorth;
	private GuiButtonExtSelectable manualModeSouth;
	
	private GuiButtonExtSelectable autoModeNorth;
	private GuiButtonExtSelectable autoModeSouth;
	
	private GuiTextField rightArrowMinimum;
	private GuiButton nightFlashToggle;
	private GuiTextField crossTime;
	private GuiTextField crossWarningTime;
	
	private GuiTextField greenMinimumNS, greenMaxNS, arrowMinimumNS, arrowMaxNS, redTimeNS, yellowTimeNS;
	private GuiTextField greenMinimumEW, greenMaxEW, arrowMinimumEW, arrowMaxEW, redTimeEW, yellowTimeEW;

	private boolean editingNorthSouth = true; // default to N/S
	
	private GuiButtonExt toggleAutoDirectionButton;
	
	private TrafficLightControlBoxTileEntity _te;
	public TrafficLightControlBoxGui(TrafficLightControlBoxTileEntity te, EntityPlayer player)
	{
		_te = te;
		this.player = player;
		_currentMode = _te.isAutoMode() ? Modes.Automatic : Modes.ManualNorthSouth; 
	}
	
	@Override
	public void initGui() {
		super.initGui();
		
		_te = (TrafficLightControlBoxTileEntity) Minecraft.getMinecraft().world.getTileEntity(_te.getPos());
	    if (!(_te instanceof TrafficLightControlBoxTileEntity)) return;
		
		int horizontalCenter = width / 2;
		int verticalCenter = height / 2;
		manualModeNorth = new GuiButtonExtSelectable(ELEMENT_IDS.manualModeNS, horizontalCenter - 107, verticalCenter - 100, 25, 20, "N/S");
		manualModeSouth = new GuiButtonExtSelectable(ELEMENT_IDS.manualModeWE, horizontalCenter - 107, verticalCenter - 78, 25, 20, "W/E");
		
		manualModeNorth.setIsSelected(true);
		
		buttonList.add(manualModeNorth);
		buttonList.add(manualModeSouth);
		
		redOn = new GuiCheckBox(ELEMENT_IDS.redOn, horizontalCenter - 27, verticalCenter - 83, "", false);
		redOnFlash = new GuiCheckBox(ELEMENT_IDS.redOnFlash, horizontalCenter - 12, verticalCenter - 83, "", false);
		redOff = new GuiCheckBox(ELEMENT_IDS.redOff, horizontalCenter + 10, verticalCenter - 83, "", false);
		redOffFlash = new GuiCheckBox(ELEMENT_IDS.redOffFlash, horizontalCenter + 25, verticalCenter - 83, "", false);
		yellowOn = new GuiCheckBox(ELEMENT_IDS.yellowOn, horizontalCenter - 27, verticalCenter - 63, "", false);
		yellowOnFlash = new GuiCheckBox(ELEMENT_IDS.yellowOnFlash, horizontalCenter - 12, verticalCenter - 63, "", false);
		yellowOff = new GuiCheckBox(ELEMENT_IDS.yellowOff, horizontalCenter + 10, verticalCenter - 63, "", false);
		yellowOffFlash = new GuiCheckBox(ELEMENT_IDS.yellowOffFlash, horizontalCenter + 25, verticalCenter - 63, "", false);
		greenOn = new GuiCheckBox(ELEMENT_IDS.greenOn, horizontalCenter - 27, verticalCenter - 43, "", false);
		greenOnFlash = new GuiCheckBox(ELEMENT_IDS.greenOnFlash, horizontalCenter - 12, verticalCenter - 43, "", false);
		greenOff = new GuiCheckBox(ELEMENT_IDS.greenOff, horizontalCenter + 10, verticalCenter - 43, "", false);
		greenOffFlash = new GuiCheckBox(ELEMENT_IDS.greenOffFlash, horizontalCenter + 25, verticalCenter - 43, "", false);
		redArrowLeftOn = new GuiCheckBox(ELEMENT_IDS.redArrowLeftOn, horizontalCenter - 27, verticalCenter - 23, "", false);
		redArrowLeftOnFlash = new GuiCheckBox(ELEMENT_IDS.redArrowLeftOnFlash, horizontalCenter - 12, verticalCenter - 23, "", false);
		redArrowLeftOff = new GuiCheckBox(ELEMENT_IDS.redArrowLeftOff, horizontalCenter + 10, verticalCenter - 23, "", false);
		redArrowLeftOffFlash = new GuiCheckBox(ELEMENT_IDS.redArrowLeftOffFlash, horizontalCenter + 25, verticalCenter - 23, "", false);
		yellowArrowLeftOn = new GuiCheckBox(ELEMENT_IDS.yellowArrowLeftOn, horizontalCenter - 27, verticalCenter - 3, "", false);
		yellowArrowLeftOnFlash = new GuiCheckBox(ELEMENT_IDS.yellowArrowLeftOnFlash, horizontalCenter - 12, verticalCenter - 3, "", false);
		yellowArrowLeftOff = new GuiCheckBox(ELEMENT_IDS.yellowArrowLeftOff, horizontalCenter + 10, verticalCenter - 3, "", false);
		yellowArrowLeftOffFlash = new GuiCheckBox(ELEMENT_IDS.yellowArrowLeftOffFlash, horizontalCenter + 25, verticalCenter - 3, "", false);
		greenArrowLeftOn = new GuiCheckBox(ELEMENT_IDS.greenArrowLeftOn, horizontalCenter - 27, verticalCenter + 17, "", false);
		greenArrowLeftOnFlash = new GuiCheckBox(ELEMENT_IDS.greenArrowLeftOnFlash, horizontalCenter - 12, verticalCenter + 17, "", false);
		greenArrowLeftOff = new GuiCheckBox(ELEMENT_IDS.greenArrowLeftOff, horizontalCenter + 10, verticalCenter + 17, "", false);
		greenArrowLeftOffFlash = new GuiCheckBox(ELEMENT_IDS.greenArrowLeftOffFlash, horizontalCenter + 25, verticalCenter + 17, "", false);
		crossOn = new GuiCheckBox(ELEMENT_IDS.crossOn, horizontalCenter - 27, verticalCenter + 37, "", false);
		crossOnFlash = new GuiCheckBox(ELEMENT_IDS.crossOnFlash, horizontalCenter - 12, verticalCenter + 37, "", false);
		crossOff = new GuiCheckBox(ELEMENT_IDS.crossOff, horizontalCenter + 10, verticalCenter + 37, "", false);
		crossOffFlash = new GuiCheckBox(ELEMENT_IDS.crossOffFlash, horizontalCenter + 25, verticalCenter + 37, "", false);
		dontCrossOn = new GuiCheckBox(ELEMENT_IDS.dontCrossOn, horizontalCenter - 27, verticalCenter + 57, "", false);
		dontCrossOnFlash = new GuiCheckBox(ELEMENT_IDS.dontCrossOnFlash, horizontalCenter - 12, verticalCenter + 57, "", false);
		dontCrossOff = new GuiCheckBox(ELEMENT_IDS.dontCrossOff, horizontalCenter + 10, verticalCenter + 57, "", false);
		dontCrossOffFlash = new GuiCheckBox(ELEMENT_IDS.dontCrossOffFlash, horizontalCenter + 25, verticalCenter + 57, "", false);
		redArrowRightOn = new GuiCheckBox(ELEMENT_IDS.redArrowRightOn, horizontalCenter - 27, verticalCenter + 77, "", false);
		redArrowRightOnFlash = new GuiCheckBox(ELEMENT_IDS.redArrowRightOnFlash, horizontalCenter - 12, verticalCenter + 77, "", false);
		redArrowRightOff = new GuiCheckBox(ELEMENT_IDS.redArrowRightOff, horizontalCenter + 10, verticalCenter + 77, "", false);
		redArrowRightOffFlash = new GuiCheckBox(ELEMENT_IDS.redArrowRightOffFlash, horizontalCenter + 25, verticalCenter + 77, "", false);
		yellowArrowRightOn = new GuiCheckBox(ELEMENT_IDS.yellowArrowRightOn, horizontalCenter - 27, verticalCenter + 97, "", false);
		yellowArrowRightOnFlash = new GuiCheckBox(ELEMENT_IDS.yellowArrowRightOnFlash, horizontalCenter - 12, verticalCenter + 97, "", false);
		yellowArrowRightOff = new GuiCheckBox(ELEMENT_IDS.yellowArrowRightOff, horizontalCenter + 10, verticalCenter + 97, "", false);
		yellowArrowRightOffFlash = new GuiCheckBox(ELEMENT_IDS.yellowArrowRightOffFlash, horizontalCenter + 25, verticalCenter + 97, "", false);
		greenArrowRightOn = new GuiCheckBox(ELEMENT_IDS.greenArrowRightOn, horizontalCenter - 27, verticalCenter + 117, "", false);
		greenArrowRightOnFlash = new GuiCheckBox(ELEMENT_IDS.greenArrowRightOnFlash, horizontalCenter - 12, verticalCenter + 117, "", false);
		greenArrowRightOff = new GuiCheckBox(ELEMENT_IDS.greenArrowRightOff, horizontalCenter + 10, verticalCenter + 117, "", false);
		greenArrowRightOffFlash = new GuiCheckBox(ELEMENT_IDS.greenArrowRightOffFlash, horizontalCenter + 25, verticalCenter + 117, "", false);
		
		
		
		buttonList.add(redOn);
		buttonList.add(redOnFlash);
		buttonList.add(redOff);
		buttonList.add(redOffFlash);
		buttonList.add(yellowOn);
		buttonList.add(yellowOnFlash);
		buttonList.add(yellowOff);
		buttonList.add(yellowOffFlash);
		buttonList.add(greenOn);
		buttonList.add(greenOnFlash);
		buttonList.add(greenOff);
		buttonList.add(greenOffFlash);
		buttonList.add(redArrowLeftOn);
		buttonList.add(redArrowLeftOnFlash);
		buttonList.add(redArrowLeftOff);
		buttonList.add(redArrowLeftOffFlash);
		buttonList.add(yellowArrowLeftOn);
		buttonList.add(yellowArrowLeftOnFlash);
		buttonList.add(yellowArrowLeftOff);
		buttonList.add(yellowArrowLeftOffFlash);
		buttonList.add(greenArrowLeftOn);
		buttonList.add(greenArrowLeftOnFlash);
		buttonList.add(greenArrowLeftOff);
		buttonList.add(greenArrowLeftOffFlash);
		buttonList.add(crossOn);
		buttonList.add(crossOnFlash);
		buttonList.add(crossOff);
		buttonList.add(crossOffFlash);
		buttonList.add(dontCrossOn);
		buttonList.add(dontCrossOnFlash);
		buttonList.add(dontCrossOff);
		buttonList.add(dontCrossOffFlash);
		buttonList.add(redArrowRightOn);
		buttonList.add(redArrowRightOnFlash);
		buttonList.add(redArrowRightOff);
		buttonList.add(redArrowRightOffFlash);
		buttonList.add(yellowArrowRightOn);
		buttonList.add(yellowArrowRightOnFlash);
		buttonList.add(yellowArrowRightOff);
		buttonList.add(yellowArrowRightOffFlash);
		buttonList.add(greenArrowRightOn);
		buttonList.add(greenArrowRightOnFlash);
		buttonList.add(greenArrowRightOff);
		buttonList.add(greenArrowRightOffFlash);
		
		setManualChecked();
		
		int xNS = horizontalCenter - 54;
		int xEW = horizontalCenter - 54;
		int yStart = verticalCenter - 90;
		int spacing = 30;
		
		if(_currentMode == Modes.Automatic) {
			autoModeNorth = new GuiButtonExtSelectable(600, horizontalCenter - 107, verticalCenter - 100, 25, 20, "N/S");
			autoModeSouth = new GuiButtonExtSelectable(700, horizontalCenter - 107, verticalCenter - 78, 25, 20, "W/E");
			this.buttonList.add(autoModeNorth);
		    this.buttonList.add(autoModeSouth);
		    
			this.nightFlashToggle = new GuiButtonToggle(9001, horizontalCenter + 107, verticalCenter - 78, 25, 20, _te.isNightFlashEnabled());
		    this.buttonList.add(nightFlashToggle);
		
		}

		greenMinimumNS = new GuiTextField(ELEMENT_IDS.greenMinimum, fontRenderer, xNS, yStart, 105, 20);
		greenMinimumEW = new GuiTextField(ELEMENT_IDS.greenMinimum + 100, fontRenderer, xEW, yStart, 105, 20);
		yStart += spacing;

		greenMaxNS = new GuiTextField(ELEMENT_IDS.greenMax, fontRenderer, xNS, yStart, 105, 20);
		greenMaxEW = new GuiTextField(ELEMENT_IDS.greenMax + 100, fontRenderer, xEW, yStart, 105, 20);
		yStart += spacing;

		yellowTimeNS = new GuiTextField(ELEMENT_IDS.yellowTime + 10, fontRenderer, xNS, yStart, 105, 20);
		yellowTimeEW = new GuiTextField(ELEMENT_IDS.yellowTime + 110, fontRenderer, xEW, yStart, 105, 20);
		yStart += spacing;

		redTimeNS = new GuiTextField(ELEMENT_IDS.redTime + 10, fontRenderer, xNS, yStart, 105, 20);
		redTimeEW = new GuiTextField(ELEMENT_IDS.redTime + 110, fontRenderer, xEW, yStart, 105, 20);
		yStart += spacing;

		arrowMinimumNS = new GuiTextField(ELEMENT_IDS.arrowMinimum, fontRenderer, xNS, yStart, 105, 20);
		arrowMinimumEW = new GuiTextField(ELEMENT_IDS.arrowMinimum + 100, fontRenderer, xEW, yStart, 105, 20);
		yStart += spacing;

		arrowMaxNS = new GuiTextField(ELEMENT_IDS.arrowMax, fontRenderer, xNS, yStart, 105, 20);
		arrowMaxEW = new GuiTextField(ELEMENT_IDS.arrowMax + 100, fontRenderer, xEW, yStart, 105, 20);
		yStart += spacing;

		crossTime = new GuiTextField(ELEMENT_IDS.crossTime, fontRenderer, xNS, yStart, 105, 20);
		
		yStart += spacing;
		crossWarningTime = new GuiTextField(ELEMENT_IDS.crossWarningTime, fontRenderer, xNS, yStart, 105, 20);
		yStart += spacing;
		rightArrowMinimum = new GuiTextField(ELEMENT_IDS.rightArrowMinimum, fontRenderer, xNS, yStart, 105, 20);

		// Fill with values
		greenMinimumNS.setText(Double.toString(_te.getAutomator().getGreenMinimumNS()));
		greenMinimumEW.setText(Double.toString(_te.getAutomator().getGreenMinimumEW()));
		greenMaxNS.setText(Double.toString(_te.getAutomator().getGreenMaxNS()));
		greenMaxEW.setText(Double.toString(_te.getAutomator().getGreenMaxEW()));
		yellowTimeNS.setText(Double.toString(_te.getAutomator().getYellowTimeNS()));
		yellowTimeEW.setText(Double.toString(_te.getAutomator().getYellowTimeEW()));
		redTimeNS.setText(Double.toString(_te.getAutomator().getRedTimeNS()));
		redTimeEW.setText(Double.toString(_te.getAutomator().getRedTimeEW()));
		arrowMinimumNS.setText(Double.toString(_te.getAutomator().getArrowMinimumNS()));
		arrowMinimumEW.setText(Double.toString(_te.getAutomator().getGreenMinimumEW()));
		arrowMaxNS.setText(Double.toString(_te.getAutomator().getArrowMaxNS()));
		arrowMaxEW.setText(Double.toString(_te.getAutomator().getArrowMaxEW()));
		crossTime.setText(Double.toString(_te.getAutomator().getCrossTime()));
		crossWarningTime.setText(Double.toString(_te.getAutomator().getCrossWarningTime()));
		rightArrowMinimum.setText(Double.toString(_te.getAutomator().getRightArrowTime()));

		
		
		

		
		setButtonVisibilityForMode();
	}
	
	

	
	private void setManualChecked()
	{
		greenOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Green, false, true));
		greenOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenDownArrow, false, true));
		yellowOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Yellow, false, true));
		yellowOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowX, false, true));
		redOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Red, false, true));
		redOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedX, false, true));
		greenArrowLeftOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenArrowLeft, false, true));
		yellowArrowLeftOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowLeft, false, true));
		yellowArrowLeftOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowLeft2, false, true));
		redArrowLeftOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedArrowLeft, false, true));
		crossOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Cross, false, true));
		dontCrossOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.DontCross, false, true));
		greenOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Green, false, false));
		greenOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenDownArrow, false, false));
		yellowOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Yellow, false, false));
		yellowOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowX, false, false));
		redOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Red, false, false));
		redOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedX, false, false));
		greenArrowLeftOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenArrowLeft, false, false));
		yellowArrowLeftOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowLeft, false, false));
		yellowArrowLeftOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowLeft2, false, false));
		redArrowLeftOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedArrowLeft, false, false));
		crossOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Cross, false, false));
		dontCrossOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.DontCross, false, false));
		greenArrowRightOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenArrowRight, false, true));
		yellowArrowRightOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowRight, false, true));
		yellowArrowRightOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowRight2, false, true));
		redArrowRightOn.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedArrowRight, false, true));
		greenArrowRightOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenArrowRight, false, false));
		yellowArrowRightOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowRight, false, false));
		yellowArrowRightOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowRight2, false, false));
		redArrowRightOff.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedArrowRight, false, false));
		// Flashing Bulbs
		greenOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Green, true, true));
		greenOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenDownArrow, true, true));
		yellowOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Yellow, true, true));
		yellowOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowX, true, true));
		redOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Red, true, true));
		redOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedX, true, true));
		greenArrowLeftOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenArrowLeft, true, true));
		yellowArrowLeftOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowLeft, true, true));
		yellowArrowLeftOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true));
		redArrowLeftOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedArrowLeft, true, true));
		crossOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Cross, true, true));
		dontCrossOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.DontCross, true, true));
		greenOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Green, true, false));
		greenOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenDownArrow, true, false));
		yellowOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Yellow, true, false));
		yellowOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowX, true, false));
		redOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Red, true, false));
		redOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedX, true, false));
		greenArrowLeftOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenArrowLeft, true, false));
		yellowArrowLeftOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false));
		yellowArrowLeftOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, false));
		redArrowLeftOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedArrowLeft, true, false));
		crossOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.Cross, true, false));
		dontCrossOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.DontCross, true, false));
		greenArrowRightOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenArrowRight, true, true));
		yellowArrowRightOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowRight, true, true));
		yellowArrowRightOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowRight2, true, true));
		redArrowRightOnFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedArrowRight, true, true));
		greenArrowRightOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.GreenArrowRight, true, false));
		yellowArrowRightOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowRight, true, false));
		yellowArrowRightOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.YellowArrowRight2, true, false));
		redArrowRightOffFlash.setIsChecked(getChecked(EnumTrafficLightBulbTypes.RedArrowRight, true, false));
	}
	
	public void setButtonVisibilityForMode()
	{
		boolean manualMode = _currentMode == Modes.ManualNorthSouth || _currentMode == Modes.ManualWestEast;
		
		buttonList
			.stream()
			.filter(b -> b instanceof GuiCheckBox)
			.forEach(b -> b.visible = manualMode);
		
		manualModeNorth.visible = manualMode;
		manualModeSouth.visible = manualMode;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		// TODO Auto-generated method stub
		int horizontalCenter = width / 2;
		int verticalCenter = height / 2;
		super.drawScreen(mouseX, mouseY, partialTicks);
		Minecraft.getMinecraft().getTextureManager().bindTexture(background);
		drawScaledCustomSizeModalRect(0, 0, 0, 0, 16, 16, width, height, 16, 16);

		
		if (_currentMode == Modes.ManualNorthSouth || _currentMode == Modes.ManualWestEast)
		{
			drawManualMode(horizontalCenter, verticalCenter);
			 
			
			
			
		}
		
		if (_currentMode == Modes.Automatic)
		{
			drawAutomaticMode(horizontalCenter, verticalCenter);
		}
		
		
		
				
		super.drawScreen(mouseX, mouseY, partialTicks);
		int hoverX = horizontalCenter - 54; // X coordinate of the designated point
	    int hoverY = verticalCenter + 75; // Y coordinate of the designated point
	    int hoverWidth = 18; // Width of the hovering text area
	    int hoverHeight = 16; // Height of the hovering text area
	    if (_currentMode == Modes.ManualNorthSouth || _currentMode == Modes.ManualWestEast)
		{
	    	
		if (mouseX >= hoverX && mouseX <= hoverX + hoverWidth && mouseY >= hoverY && mouseY <= hoverY + hoverHeight) {
			drawHoveredText(mouseX, mouseY, "right");
	    }
		hoverX = horizontalCenter - 54; // X coordinate of the designated point
	    hoverY = verticalCenter - 25; // Y coordinate of the designated point
	    
		
	    if (mouseX >= hoverX && mouseX <= hoverX + hoverWidth && mouseY >= hoverY && mouseY <= hoverY + hoverHeight) {
			drawHoveredText(mouseX, mouseY, "leftred");
	    }
		hoverX = horizontalCenter - 54; // X coordinate of the designated point
	    hoverY = verticalCenter - 5; // Y coordinate of the designated point
	    if (mouseX >= hoverX && mouseX <= hoverX + hoverWidth && mouseY >= hoverY && mouseY <= hoverY + hoverHeight) {
			drawHoveredText(mouseX, mouseY, "leftyellow");
	    }
	    hoverX = horizontalCenter - 54; // X coordinate of the designated point
	    hoverY = verticalCenter - 85; // Y coordinate of the designated point
	    if (mouseX >= hoverX && mouseX <= hoverX + hoverWidth && mouseY >= hoverY && mouseY <= hoverY + hoverHeight) {
			drawHoveredText(mouseX, mouseY, "red");
	    }
	    hoverX = horizontalCenter - 54; // X coordinate of the designated point
	    hoverY = verticalCenter - 65; // Y coordinate of the designated point
	    if (mouseX >= hoverX && mouseX <= hoverX + hoverWidth && mouseY >= hoverY && mouseY <= hoverY + hoverHeight) {
			drawHoveredText(mouseX, mouseY, "yellow");
	    }
	    hoverX = horizontalCenter - 54; // X coordinate of the designated point
	    hoverY = verticalCenter - 45; // Y coordinate of the designated point
	    if (mouseX >= hoverX && mouseX <= hoverX + hoverWidth && mouseY >= hoverY && mouseY <= hoverY + hoverHeight) {
			drawHoveredText(mouseX, mouseY, "green");
	    }
	    hoverX = horizontalCenter - 54; // X coordinate of the designated point
	    hoverY = verticalCenter + 15; // Y coordinate of the designated point
	    if (mouseX >= hoverX && mouseX <= hoverX + hoverWidth && mouseY >= hoverY && mouseY <= hoverY + hoverHeight) {
			drawHoveredText(mouseX, mouseY, "leftgreen");
	    }
		}
	}
	
	private void drawHoveredText(int mouseX, int mouseY, String type) {
		String text = "";
		
		switch(type) {
		case "right":
			text = "Right Arrow Red And No Right Turn";
				drawHoveringText(text, mouseX, mouseY);
			break;
		case "leftred": 
			text = "Left Arrow Red, No Left Turn, And U-Turn Arrow Red";
			drawHoveringText(text, mouseX, mouseY);
			break;
		case "leftyellow":
			text = "Left Arrow Yellow and U-Turn Arrow Yellow";
			drawHoveringText(text, mouseX, mouseY);
			break;
		case "leftgreen":
			text = "Left Arrow Green and U-Turn Arrow Green";
			drawHoveringText(text, mouseX, mouseY);
			break;
		case "red":
			text = "Solid Red and Striaght Arrow Red";
			drawHoveringText(text, mouseX, mouseY);
			break;
		case "yellow":
			text = "Solid Yellow and Striaght Arrow Yellow";
			drawHoveringText(text, mouseX, mouseY);
			break;
		case "green":
			text = "Solid Green and Striaght Arrow Green";
			drawHoveringText(text, mouseX, mouseY);
			break;
				
}			
	}
//	These are var's for what lets the code know what texture to should for special bulbs 
	// Must be anywhere above the next //comment() 
	private int tickCounter = 0;
	private Boolean isRightRed = true;
	private Boolean isLeftYellow = true;
	private String LeftTurn = "Red";
	private Boolean isLeftGreen = true;
	private Boolean isRed = true;
	private Boolean isYellow = true;
	private Boolean isGreen = true;
	private ResourceLocation textureRight = null;
	private ResourceLocation textureLeftRed = null;
	private ResourceLocation textureLeftYellow = null;
	private ResourceLocation textureLeftGreen = null;
	private ResourceLocation textureRed = null;
	private ResourceLocation textureYellow = null;
	private ResourceLocation textureGreen = null;
	
	// the above must stay above this comment if will break otherwise
	private void drawGreenBulb(int horizontalCenter, int verticalCenter) {
		Minecraft mc = Minecraft.getMinecraft();
		TextureMap textureMap = mc.getTextureMapBlocks();
		 TextureAtlasSprite sprite;
		 
		 if(isGreen) {
			 textureGreen = new ResourceLocation("realistictrafficcontrol:blocks/green");
		 } else {
			 textureGreen = new ResourceLocation("realistictrafficcontrol:blocks/straight_green");
		 }
		 
		 sprite = textureMap.getAtlasSprite(textureGreen.toString());
			drawTexturedModalRect(horizontalCenter - 54, verticalCenter - 45, sprite, 16, 16);
	}
	
	private void drawYellowBulb(int horizontalCenter, int verticalCenter) {
		Minecraft mc = Minecraft.getMinecraft();
		TextureMap textureMap = mc.getTextureMapBlocks();
		 TextureAtlasSprite sprite;
		 
		 if(isYellow) {
			 textureYellow = new ResourceLocation("realistictrafficcontrol:blocks/yellow_solid");
		 } else {
			 textureYellow = new ResourceLocation("realistictrafficcontrol:blocks/straight_yellow");
		 }
		 
		 sprite = textureMap.getAtlasSprite(textureYellow.toString());
			drawTexturedModalRect(horizontalCenter - 54, verticalCenter - 65, sprite, 16, 16);
	}
	
	private void drawRedBulb(int horizontalCenter, int verticalCenter) {
		Minecraft mc = Minecraft.getMinecraft();
		TextureMap textureMap = mc.getTextureMapBlocks();
		 TextureAtlasSprite sprite;
		 
		 if(isRed) {
			 textureRed = new ResourceLocation("realistictrafficcontrol:blocks/red_solid");
		 } else {
			 textureRed = new ResourceLocation("realistictrafficcontrol:blocks/straight_red");
		 }
		 
		 sprite = textureMap.getAtlasSprite(textureRed.toString());
			drawTexturedModalRect(horizontalCenter - 54, verticalCenter - 85, sprite, 16, 16);
	}
	
	private void drawLeftGreenBulb(int horizontalCenter, int verticalCenter) {
		Minecraft mc = Minecraft.getMinecraft();
		TextureMap textureMap = mc.getTextureMapBlocks();
		 TextureAtlasSprite sprite;
		 
		 if(isLeftGreen) {
			 textureLeftGreen = new ResourceLocation("realistictrafficcontrol:blocks/green_arrow_left");
		 } else {
			 textureLeftGreen = new ResourceLocation("realistictrafficcontrol:blocks/green_arrow_uturn");
		 }
		 
		 sprite = textureMap.getAtlasSprite(textureLeftGreen.toString());
			drawTexturedModalRect(horizontalCenter - 54, verticalCenter + 15, sprite, 16, 16);
	}
	
	private void drawLeftYellowBulb(int horizontalCenter, int verticalCenter) {
		Minecraft mc = Minecraft.getMinecraft();
		TextureMap textureMap = mc.getTextureMapBlocks();
		 TextureAtlasSprite sprite;
		 
		 if(isLeftYellow) {
			 textureLeftYellow = new ResourceLocation("realistictrafficcontrol:blocks/yellow_arrow_left");
		 } else {
			 textureLeftYellow = new ResourceLocation("realistictrafficcontrol:blocks/yellow_arrow_uturn");
		 }
		 
		 sprite = textureMap.getAtlasSprite(textureLeftYellow.toString());
			drawTexturedModalRect(horizontalCenter - 54, verticalCenter - 5, sprite, 16, 16);
	}
	
	private void drawLeftRedBulb(int horizontalCenter, int verticalCenter) {
		Minecraft mc = Minecraft.getMinecraft();
		TextureMap textureMap = mc.getTextureMapBlocks();
		 TextureAtlasSprite sprite;
		
		 if (LeftTurn == "Red") {
		        textureLeftRed = new ResourceLocation("realistictrafficcontrol:blocks/red_arrow_left");
		    } else if(LeftTurn == "No") {
		        textureLeftRed = new ResourceLocation("realistictrafficcontrol:blocks/no_left_turn");
		    } else if(LeftTurn == "UTurn"){
		    	textureLeftRed = new ResourceLocation("realistictrafficcontrol:blocks/red_arrow_uturn");
		    }
		    sprite = textureMap.getAtlasSprite(textureLeftRed.toString());
			drawTexturedModalRect(horizontalCenter - 54, verticalCenter - 25, sprite, 16, 16);
	}
		
		   
	
	
	
	private void drawRightRedBulb(int horizontalCenter, int verticalCenter) {
		Minecraft mc = Minecraft.getMinecraft();
		TextureMap textureMap = mc.getTextureMapBlocks();
		 TextureAtlasSprite sprite;
		 
		 if (isRightRed) {
		        textureRight = new ResourceLocation("realistictrafficcontrol:blocks/red_arrow_right");
		    } else {
		        textureRight = new ResourceLocation("realistictrafficcontrol:blocks/no_right_turn");
		    }
	    
	    sprite = textureMap.getAtlasSprite(textureRight.toString());
	    drawTexturedModalRect(horizontalCenter - 54, verticalCenter + 75, sprite, 16, 16);
	}

	
	private void switchSpecialBulbs(int horizontalCenter, int verticalCenter) {
	    
	    
	   
	    
	    tickCounter++;
	    if (tickCounter >= 180) { // 5 seconds (assuming 20 ticks per second)
	        tickCounter = 0;
	        isRightRed = !isRightRed;
	        isLeftYellow = !isLeftYellow;
	        isLeftGreen = !isLeftGreen;
	        isRed = !isRed;
	        isYellow = !isYellow;
	        isGreen = !isGreen;
	        if (LeftTurn == "Red") {
		        LeftTurn = "No";
		    } else if(LeftTurn == "No") {
		    	LeftTurn = "UTurn";
		    } else if(LeftTurn == "UTurn"){
		    	LeftTurn = "Red";
		    }
	       
	       
	    }
	    drawRightRedBulb(horizontalCenter, verticalCenter);
	    drawLeftRedBulb(horizontalCenter, verticalCenter);
        drawLeftYellowBulb(horizontalCenter, verticalCenter);
        drawLeftGreenBulb(horizontalCenter, verticalCenter);
        drawRedBulb(horizontalCenter, verticalCenter);
        drawYellowBulb(horizontalCenter, verticalCenter);
        drawGreenBulb(horizontalCenter, verticalCenter);
	}
	    
	    
	
    
	
	private void drawManualMode(int horizontalCenter, int verticalCenter)
	{
	   
		drawString(fontRenderer, "Manual Mode", horizontalCenter - 54, verticalCenter - 110, 0xFFFF00);
		drawString(fontRenderer, "Direction", horizontalCenter - 115, verticalCenter - 110, 0xFFFFFF);
		
		drawString(fontRenderer, "Bulb", horizontalCenter - 54, verticalCenter - 100, 0xFFFFFF);
		drawString(fontRenderer, "F", horizontalCenter - 11, verticalCenter - 100, 0xFFFFFF);
		drawString(fontRenderer, "F", horizontalCenter + 26, verticalCenter - 100, 0xFFFFFF);
		
		
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/redstone_torch_on");
		drawTexturedModalRect(horizontalCenter - 30, verticalCenter - 106, sprite, 16, 16);
		
		sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/redstone_torch_off");
		drawTexturedModalRect(horizontalCenter + 7, verticalCenter - 106, sprite, 16, 16);
		sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("realistictrafficcontrol:blocks/cross");
		drawTexturedModalRect(horizontalCenter - 54, verticalCenter + 35, sprite, 16, 16);
		
		sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("realistictrafficcontrol:blocks/dontcross");
		drawTexturedModalRect(horizontalCenter - 54, verticalCenter + 55, sprite, 16, 16);
		switchSpecialBulbs(horizontalCenter, verticalCenter);
		sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("realistictrafficcontrol:blocks/yellow_arrow_right");
		drawTexturedModalRect(horizontalCenter - 54, verticalCenter + 95, sprite, 16, 16);
		
		sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("realistictrafficcontrol:blocks/green_arrow_right");
		drawTexturedModalRect(horizontalCenter - 54, verticalCenter + 115, sprite, 16, 16);
		
	}
	
	
	private void drawAutomaticMode(int horizontalCenter, int verticalCenter) {
		int leftMargin = horizontalCenter - 54;
		int y = verticalCenter - 115;
		int spacing = 15;

		drawString(fontRenderer, "Automatic Mode", leftMargin, y, 0xFFFF00);
		y += spacing;
		drawString(fontRenderer, "Direction", horizontalCenter - 115, verticalCenter - 110, 0xFFFFFF);
		
		drawString(fontRenderer, "Green Minimum (0 = always use Max; otherwise used if no sensors trip)", leftMargin, y, 0xFFFFFF);
		
		y += 12;
		if (editingNorthSouth) {
		greenMinimumNS.drawTextBox();
		} else {
			greenMinimumEW.drawTextBox();
		}
		spacing += +4;
		y += spacing;
		

		drawString(fontRenderer, "Green Max", leftMargin, y, 0xFFFFFF);
		y += 12;
		if (editingNorthSouth) {
		greenMaxNS.drawTextBox();
		} else {
			greenMaxEW.drawTextBox();
		}
		spacing -= +1;
		y += spacing;

		drawString(fontRenderer, "Yellow Time", leftMargin, y, 0xFFFFFF);
		y += 12;
		if (editingNorthSouth) {
		yellowTimeNS.drawTextBox();
		} else {
			yellowTimeEW.drawTextBox();
		}
		y += spacing;

		drawString(fontRenderer, "Red Time", leftMargin, y, 0xFFFFFF);
		y += 12;
		if (editingNorthSouth) {
		redTimeNS.drawTextBox();
		} else {
			redTimeEW.drawTextBox();
		}
		y += spacing;

		drawString(fontRenderer, "Left Arrow Min (0 = always use Max; otherwise used if no sensors trip)", leftMargin, y, 0xFFFFFF);
		y += 12;
		if (editingNorthSouth) {
		arrowMinimumNS.drawTextBox();
		} else {
			arrowMinimumEW.drawTextBox();
		}
		y += spacing;

		drawString(fontRenderer, "Left Arrow Max", leftMargin, y, 0xFFFFFF);
		y += 12;
		if (editingNorthSouth) {
			arrowMaxNS.drawTextBox();
			} else {
				arrowMaxEW.drawTextBox();
			}
		y += spacing;

		drawString(fontRenderer, "Cross Time", leftMargin, y, 0xFFFFFF);
		y += 12;
		crossTime.drawTextBox();
		y += spacing;

		drawString(fontRenderer, "Cross Warning Time", leftMargin, y, 0xFFFFFF);
		y += 12;
		crossWarningTime.drawTextBox();
		y += spacing;

		drawString(fontRenderer, "Right Arrow", leftMargin, y, 0xFFFFFF);
		y += 12;
		rightArrowMinimum.drawTextBox();
		
		
		





		
		
	}




	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		
		if (button.id == 600) { // Your toggleAutoDirectionButton ID
		    editingNorthSouth = true;
		  //  initGui(); // Reinitialize GUI to reload correct text fields
		  autoModeNorth.setIsSelected(true);
		  autoModeSouth.setIsSelected(false);
		}
		if (button.id == 700) { // Your toggleAutoDirectionButton ID
		    editingNorthSouth = false;
		  //  initGui(); // Reinitialize GUI to reload correct text fields
		  autoModeSouth.setIsSelected(true);
		  autoModeNorth.setIsSelected(false);
		}
		if (button.id == 9001 && button instanceof GuiButtonToggle) {
		    GuiButtonToggle toggle = (GuiButtonToggle) button;
		    toggle.toggle();
		    boolean enabled = toggle.isToggled();

		    _te.setNightFlashEnabled(enabled); // client-side
		    ModNetworkHandler.INSTANCE.sendToServer(new PacketToggleNightFlash(_te.getPos(), enabled));
		}

		switch(button.id)
		{
		
			case ELEMENT_IDS.manualModeNS:
				setCurrentMode(Modes.ManualNorthSouth);
				setManualChecked();
				manualModeNorth.setIsSelected(true);
				manualModeSouth.setIsSelected(false);
				break;
			case ELEMENT_IDS.manualModeWE:
				setCurrentMode(Modes.ManualWestEast);
				setManualChecked();
				manualModeNorth.setIsSelected(false);
				manualModeSouth.setIsSelected(true);
				break;
			case ELEMENT_IDS.greenOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.Green, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenDownArrow, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightGreen, false, true);
				break;
			case ELEMENT_IDS.greenOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.Green, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenDownArrow, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightGreen, true, true);
				break;
			case ELEMENT_IDS.yellowOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.Yellow, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowX, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightYellow, false, true);
				break;
			case ELEMENT_IDS.yellowOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.Yellow, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowX, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightYellow, true, true);
				break;
			case ELEMENT_IDS.redOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.Red, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.RedX, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightRed, false, true);
				break;
			case ELEMENT_IDS.redOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.Red, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.RedX, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightRed, true, true);
				break;
			case ELEMENT_IDS.greenArrowLeftOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowLeft, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowUTurn, false, true);
				break;
			case ELEMENT_IDS.yellowArrowLeftOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowLeft, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowUTurn, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowLeft2, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowUTurn2, false, true);
				break;
			case ELEMENT_IDS.redArrowLeftOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowLeft, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.NoLeftTurn, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowUTurn, false, true);
				break;
			case ELEMENT_IDS.greenArrowLeftOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowLeft, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowUTurn, true, true);
				break;
			case ELEMENT_IDS.yellowArrowLeftOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowLeft, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowUTurn, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, true);
				break;
			case ELEMENT_IDS.redArrowLeftOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowLeft, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.NoLeftTurn, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowUTurn, true, true);
				break;
			case ELEMENT_IDS.greenOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.Green, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenDownArrow, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightGreen, false, false);
				break;
			case ELEMENT_IDS.greenOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.Green, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenDownArrow, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightGreen, true, false);
				break;
			case ELEMENT_IDS.yellowOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.Yellow, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowX, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightYellow, false, false);
				break;
			case ELEMENT_IDS.yellowOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.Yellow, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowX, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightYellow, true, false);
				break;
			case ELEMENT_IDS.redOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.Red, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.RedX, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightRed, false, false);
				break;
			case ELEMENT_IDS.redOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.Red, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.RedX, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.StraightRed, true, false);
				break;
			case ELEMENT_IDS.greenArrowLeftOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowLeft, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowUTurn, false, false);
				break;
			case ELEMENT_IDS.yellowArrowLeftOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowLeft, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowUTurn, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowLeft2, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowUTurn2, false, false);
				break;
			case ELEMENT_IDS.redArrowLeftOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowLeft, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowUTurn, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.NoLeftTurn, false, false);
				break;
			case ELEMENT_IDS.greenArrowLeftOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowLeft, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowUTurn, true, false);
				break;
			case ELEMENT_IDS.yellowArrowLeftOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowLeft2, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, false);
				break;
			case ELEMENT_IDS.redArrowLeftOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
				break;
			case ELEMENT_IDS.crossOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.Cross, false, true);
				break;
			case ELEMENT_IDS.crossOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.Cross, false, false);
				break;
			case ELEMENT_IDS.dontCrossOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.DontCross, false, true);
				break;
			case ELEMENT_IDS.dontCrossOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.DontCross, false, false);
				break;
			case ELEMENT_IDS.crossOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.Cross, true, true);
				break;
			case ELEMENT_IDS.crossOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.Cross, true, false);
				break;
			case ELEMENT_IDS.dontCrossOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.DontCross, true, true);
				break;
			case ELEMENT_IDS.dontCrossOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.DontCross, true, false);
				break;
			case ELEMENT_IDS.greenArrowRightOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowRight, false, true);
				break;
			case ELEMENT_IDS.yellowArrowRightOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowRight, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowRight2, false, true);
				break;
			case ELEMENT_IDS.redArrowRightOn:
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowRight, false, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.NoRightTurn, false, true);
				break;
			case ELEMENT_IDS.greenArrowRightOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowRight, true, true);
				
				break;
			case ELEMENT_IDS.yellowArrowRightOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowRight, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowRight2, true, true);
				break;
			case ELEMENT_IDS.redArrowRightOnFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowRight, true, true);
				handleManualClick(button, EnumTrafficLightBulbTypes.NoRightTurn, true, true);
				break;
			case ELEMENT_IDS.greenArrowRightOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowRight, false, false);
				break;
			case ELEMENT_IDS.yellowArrowRightOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowRight, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowRight2, false, false);
				break;
			case ELEMENT_IDS.redArrowRightOff:
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowRight, false, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.NoRightTurn, false, false);
				break;
			case ELEMENT_IDS.greenArrowRightOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
				break;
			case ELEMENT_IDS.yellowArrowRightOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.YellowArrowRight2, true, false);
				break;
			case ELEMENT_IDS.redArrowRightOffFlash:
				handleManualClick(button, EnumTrafficLightBulbTypes.RedArrowRight, true, false);
				handleManualClick(button, EnumTrafficLightBulbTypes.NoRightTurn, true, false);
				break;
		}
		_te.markDirty();
	}
	
	@Override
	public void onGuiClosed() {
		
	
        
        
		_te.performClientToServerSync();
		
		this.player.sendMessage(new TextComponentString("[Realistic Traffic Control] Control Box " + _te.getPos().getX() + "," + _te.getPos().getY() + "," + _te.getPos().getZ() + " Was Saved."));
		
		
	}
	
	private void handleManualClick(GuiButton button, EnumTrafficLightBulbTypes bulbType, boolean flash, boolean forActive)
	{
		GuiCheckBox box = (GuiCheckBox)button;
		if (_currentMode == Modes.ManualNorthSouth)
		{
			if (forActive)
			{
				_te.addRemoveNorthSouthActive(bulbType, flash, box.isChecked());
			}
			else
			{
				_te.addRemoveNorthSouthInactive(bulbType, flash, box.isChecked());
			}
		}
		else
		{
			if (forActive)
			{
				_te.addRemoveWestEastActive(bulbType, flash, box.isChecked());
			}
			else
			{
				_te.addRemoveWestEastInactive(bulbType, flash, box.isChecked());
			}
		}
	}
	
	private void setCurrentMode(Modes mode)
	{
		_currentMode = mode;
	}
	
	private boolean getChecked(EnumTrafficLightBulbTypes bulbType, boolean flash, boolean forActive)
	{
		if (_currentMode == Modes.ManualNorthSouth)
		{
			return _te.hasSpecificNorthSouthManualOption(bulbType, flash, forActive);
		}
		else
		{
			return _te.hasSpecificWestEastManualOption(bulbType, flash, forActive);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (greenMinimumNS != null) greenMinimumNS.mouseClicked(mouseX, mouseY, mouseButton);
		if (greenMinimumEW != null) greenMinimumEW.mouseClicked(mouseX, mouseY, mouseButton);
		if (greenMaxNS != null) greenMaxNS.mouseClicked(mouseX, mouseY, mouseButton);
		if (greenMaxEW != null) greenMaxEW.mouseClicked(mouseX, mouseY, mouseButton);
		if (yellowTimeNS != null) yellowTimeNS.mouseClicked(mouseX, mouseY, mouseButton);
		if (yellowTimeEW != null) yellowTimeEW.mouseClicked(mouseX, mouseY, mouseButton);
		if (redTimeNS != null) redTimeNS.mouseClicked(mouseX, mouseY, mouseButton);
		if (redTimeEW != null) redTimeEW.mouseClicked(mouseX, mouseY, mouseButton);
		if (arrowMinimumNS != null) arrowMinimumNS.mouseClicked(mouseX, mouseY, mouseButton);
		if (arrowMinimumEW != null) arrowMinimumEW.mouseClicked(mouseX, mouseY, mouseButton);
		if (arrowMaxNS != null) arrowMaxNS.mouseClicked(mouseX, mouseY, mouseButton);
		if (arrowMaxEW != null) arrowMaxEW.mouseClicked(mouseX, mouseY, mouseButton);
		if (crossTime != null) crossTime.mouseClicked(mouseX, mouseY, mouseButton);
		if (crossWarningTime != null) crossWarningTime.mouseClicked(mouseX, mouseY, mouseButton);
		if (rightArrowMinimum != null) rightArrowMinimum.mouseClicked(mouseX, mouseY, mouseButton);
		
		
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (editingNorthSouth) {
			checkedKeyTyped(greenMinimumNS, typedChar, keyCode, v -> _te.getAutomator().setGreenMinimumNS(v));
			checkedKeyTyped(greenMaxNS, typedChar, keyCode, v -> _te.getAutomator().setGreenMaxNS(v));
			checkedKeyTyped(arrowMinimumNS, typedChar, keyCode, v -> _te.getAutomator().setArrowMinimumNS(v));
			checkedKeyTyped(arrowMaxNS, typedChar, keyCode, v -> _te.getAutomator().setArrowMaxNS(v));
			checkedKeyTyped(yellowTimeNS, typedChar, keyCode, v -> _te.getAutomator().setYellowTimeNS(v));
			checkedKeyTyped(redTimeNS, typedChar, keyCode, v -> _te.getAutomator().setRedTimeNS(v));
		} else {
			checkedKeyTyped(greenMinimumEW, typedChar, keyCode, v -> _te.getAutomator().setGreenMinimumEW(v));
			checkedKeyTyped(greenMaxEW, typedChar, keyCode, v -> _te.getAutomator().setGreenMaxEW(v));
			checkedKeyTyped(arrowMinimumEW, typedChar, keyCode, v -> _te.getAutomator().setArrowMinimumEW(v));
			checkedKeyTyped(arrowMaxEW, typedChar, keyCode, v -> _te.getAutomator().setArrowMaxEW(v));
			checkedKeyTyped(yellowTimeEW, typedChar, keyCode, v -> _te.getAutomator().setYellowTimeEW(v));
			checkedKeyTyped(redTimeEW, typedChar, keyCode, v -> _te.getAutomator().setRedTimeEW(v));
		}

		checkedKeyTyped(crossTime, typedChar, keyCode, v -> _te.getAutomator().setCrossTime(v));
		checkedKeyTyped(crossWarningTime, typedChar, keyCode, v -> _te.getAutomator().setCrossWarningTime(v));
		checkedKeyTyped(rightArrowMinimum, typedChar, keyCode, v -> _te.getAutomator().setRightArrowTime(v));

		super.keyTyped(typedChar, keyCode);
	}

	
	private void checkedKeyTyped(GuiTextField textBox, char typedChar, int keyCode, Consumer<Double> onTypeSuccess)
	{
		if (Character.toString(typedChar).equals(".") && textBox.getText().contains("."))
		{
			return;
		}
		
		if (Keyboard.KEY_BACK == keyCode ||
				Keyboard.KEY_DELETE == keyCode ||
				Character.isDigit(typedChar) ||
				typedChar == '.')
		{
			textBox.textboxKeyTyped(typedChar, keyCode);
			
			if (textBox.isFocused())
			{
				if (textBox.getText().isEmpty())
				{
					onTypeSuccess.accept((double)0);
				}
				else
				{
					try
					{
						double value = Double.parseDouble(textBox.getText());
						onTypeSuccess.accept(value);
					}
					catch(NumberFormatException | NullPointerException ex) {}
				}
			}
		}
	}
	
	public static class ELEMENT_IDS
	{
		public static final int greenOn = 0;
		public static final int yellowOn = 1;
		public static final int redOn = 2;
		public static final int greenArrowLeftOn = 3;
		public static final int yellowArrowLeftOn = 4;
		public static final int redArrowLeftOn = 5;
		public static final int greenOff = 6;
		public static final int yellowOff = 7;
		public static final int redOff = 8;
		public static final int greenArrowLeftOff = 9;
		public static final int yellowArrowLeftOff = 10;
		public static final int redArrowLeftOff = 11;
		public static final int greenOnFlash = 12;
		public static final int yellowOnFlash = 13;
		public static final int redOnFlash = 14;
		public static final int greenArrowLeftOnFlash = 15;
		public static final int yellowArrowLeftOnFlash = 16;
		public static final int redArrowLeftOnFlash = 17;
		public static final int greenOffFlash = 18;
		public static final int yellowOffFlash = 19;
		public static final int redOffFlash = 20;
		public static final int greenArrowLeftOffFlash = 21;
		public static final int yellowArrowLeftOffFlash = 22;
		public static final int redArrowLeftOffFlash = 23;
		public static final int manualModeNS = 25;
		public static final int manualModeWE = 26;
		public static final int greenMinimum = 27;
		public static final int yellowTime = 28;
		public static final int redTime = 29;
		public static final int arrowMinimum = 30;
		
		public static final int crossOn = 31;
		public static final int crossOff = 32;
		public static final int dontCrossOn = 33;
		public static final int dontCrossOff = 34;
		public static final int crossOnFlash = 35;
		public static final int crossOffFlash = 36;
		public static final int dontCrossOnFlash = 37;
		public static final int dontCrossOffFlash = 38;
		public static final int crossTime = 39;
		public static final int crossWarningTime = 40;
		public static final int greenArrowRightOn = 41;
		public static final int yellowArrowRightOn = 42;
		public static final int redArrowRightOn = 43;
		public static final int greenArrowRightOff = 44;
		public static final int yellowArrowRightOff = 45;
		public static final int redArrowRightOff = 46;
		public static final int greenArrowRightOnFlash = 47;
		public static final int yellowArrowRightOnFlash = 48;
		public static final int redArrowRightOnFlash = 49;
		public static final int greenArrowRightOffFlash = 50;
		public static final int yellowArrowRightOffFlash = 51;
		public static final int redArrowRightOffFlash = 52;
		public static final int rightArrowMinimum = 53;
		public static final int greenMax = 54;
		public static final int arrowMax = 55;
		public static final int dirNorthCheck = 56;
		public static final int dirSouthCheck = 57;
		public static final int dirEastCheck  = 58;
		public static final int dirWestCheck  = 59;
		
	}

	private enum Modes
	{
		ManualNorthSouth,
		ManualWestEast,
		Automatic
	}
}
