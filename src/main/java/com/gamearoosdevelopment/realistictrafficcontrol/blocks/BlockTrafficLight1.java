package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight1TileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.TrafficLight1Renderer;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.TrafficLightRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockTrafficLight1 extends BlockBaseTrafficLight
{
	public BlockTrafficLight1()
	{
		super("traffic_light_1");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void initModel()
	{
		ClientRegistry.bindTileEntitySpecialRenderer(TrafficLight1TileEntity.class, new TrafficLight1Renderer());
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		int rotation = state.getValue(BlockBaseTrafficLight.ROTATION);
		if (!CustomAngleCalculator.isCardinal(rotation))
		{
			return super.getBoundingBox(state, source, pos);
		}
		
		if (CustomAngleCalculator.isEast(rotation))
		{
			return new AxisAlignedBB(0.2375, 0.13, 0.1875, 0.65, 0.98, 0.8125);
		}
		else if (CustomAngleCalculator.isNorth(rotation))
		{
			return new AxisAlignedBB(0.1875, 0.13, 0.38, 0.8125, 0.98, 0.7625);
		}
		else if (CustomAngleCalculator.isSouth(rotation))
		{
			return new AxisAlignedBB(0.1875, 0.13, 0.25, 0.8125, 0.98, 0.5625);
		}
		else if (CustomAngleCalculator.isWest(rotation))
		{
			return new AxisAlignedBB(0.4375, 0.13, 0.1875, 0.75, 0.98, 0.8125);
		}
		return super.getBoundingBox(state, source, pos);
	}
	
	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return new TrafficLight1TileEntity();
	}
	
	@Override
	protected BaseItemTrafficLightFrame getItemVersionOfBlock()
	{
		return ModItems.traffic_light_1_frame;
	}
}
