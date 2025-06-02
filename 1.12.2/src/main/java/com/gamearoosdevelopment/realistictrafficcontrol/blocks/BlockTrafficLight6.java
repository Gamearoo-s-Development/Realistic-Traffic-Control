package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight6TileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight1TileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight2TileEntity;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.TrafficLight4Renderer;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.TrafficLight1Renderer;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.TrafficLightRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.TrafficLight6Renderer;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.TrafficLightRenderer;

import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockTrafficLight6 extends BlockBaseTrafficLight {

	public BlockTrafficLight6()
	{
		super("traffic_light_6");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void initModel() {
		ClientRegistry.bindTileEntitySpecialRenderer(TrafficLight6TileEntity.class, new TrafficLight6Renderer());
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TrafficLight6TileEntity();
	}

	@Override
	protected BaseItemTrafficLightFrame getItemVersionOfBlock() {
		return ModItems.traffic_light_6_frame;
	}
}
