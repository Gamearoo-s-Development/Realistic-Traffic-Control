package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.gui.GuiProxy;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignControllerTileEntity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockDigitalSignController extends BlockDisplayControllerBase {
	public BlockDigitalSignController() {
		super("digital_sign_controller", "digital_sign_controller");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new DigitalSignControllerTileEntity();
	}

	@Override
	protected int getGuiId() {
		return GuiProxy.GUI_IDs.DIGITAL_SIGN_CONTROLLER;
	}
}
