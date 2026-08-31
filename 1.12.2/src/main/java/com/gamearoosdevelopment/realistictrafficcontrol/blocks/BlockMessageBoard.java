package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.GuiProxy;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.MessageBoardRenderer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class BlockMessageBoard extends BlockDisplayBase {
	public BlockMessageBoard() {
		super("message_board", "message_board");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new MessageBoardTileEntity();
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, net.minecraft.util.math.BlockPos pos) {
		double angle = Math.toRadians(state.getValue(ROTATION) * 22.5);
		double xRadius = Math.abs(Math.cos(angle)) * 1.26 + Math.abs(Math.sin(angle)) * 0.24;
		double zRadius = Math.abs(Math.sin(angle)) * 1.26 + Math.abs(Math.cos(angle)) * 0.24;
		return new AxisAlignedBB(0.5 - xRadius, 0, 0.5 - zRadius, 0.5 + xRadius, 3.0, 0.5 + zRadius);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
			EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		Item heldItem = player.getHeldItem(hand).getItem();
		if (heldItem == ModItems.screwdriver
				|| heldItem == Item.getItemFromBlock(ModBlocks.message_board_controller)) {
			return false;
		}
		return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
	}

	@Override
	protected int getGuiId() {
		return GuiProxy.GUI_IDs.MESSAGE_BOARD;
	}

	@Override
	public void initModel() {
		super.initModel();
		ClientRegistry.bindTileEntitySpecialRenderer(MessageBoardTileEntity.class, new MessageBoardRenderer());
	}
}
