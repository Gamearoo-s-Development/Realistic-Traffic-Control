package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.gui.GuiProxy;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.SignRenderer;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class BlockDigitalSign extends BlockDisplayBase {
	public BlockDigitalSign() {
		super("digital_sign", "digital_sign");
	}

	public static boolean hasNeighbor(IBlockAccess world, BlockPos pos, IBlockState state, int horizontal, int vertical) {
		BlockPos target;
		if (vertical != 0) {
			target = pos.add(0, vertical, 0);
		} else {
			double angle = Math.toRadians(state.getValue(ROTATION) * 22.5);
			int dx = (int) Math.round(Math.cos(angle)) * horizontal;
			int dz = (int) Math.round(Math.sin(angle)) * horizontal;
			target = pos.add(dx, 0, dz);
		}
		IBlockState other = world.getBlockState(target);
		return other.getBlock() instanceof BlockDigitalSign && other.getValue(ROTATION).equals(state.getValue(ROTATION));
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new DigitalSignTileEntity();
	}

	@Override
	protected int getGuiId() {
		return GuiProxy.GUI_IDs.DIGITAL_SIGN;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, net.minecraft.util.math.BlockPos pos) {
		double angle = Math.toRadians(state.getValue(ROTATION) * 22.5);
		double xRadius = Math.abs(Math.cos(angle)) * 0.5 + Math.abs(Math.sin(angle)) * 0.22;
		double zRadius = Math.abs(Math.sin(angle)) * 0.5 + Math.abs(Math.cos(angle)) * 0.22;
		return new AxisAlignedBB(0.5 - xRadius, 0, 0.5 - zRadius, 0.5 + xRadius, 1, 0.5 + zRadius);
	}

	@Override
	public void initModel() {
		super.initModel();
		ClientRegistry.bindTileEntitySpecialRenderer(DigitalSignTileEntity.class, new SignRenderer());
	}
}
