package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.gui.GuiProxy;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardControllerTileEntity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockMessageBoardController extends BlockDisplayControllerBase {
	public BlockMessageBoardController() {
		super("message_board_controller", "message_board_controller");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new MessageBoardControllerTileEntity();
	}

	@Override
	protected int getGuiId() {
		return GuiProxy.GUI_IDs.MESSAGE_BOARD_CONTROLLER;
	}
}
