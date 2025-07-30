package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.TrafficLightRenderer;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockTrafficLight extends BlockBaseTrafficLight {

	public BlockTrafficLight()
	{
		super("traffic_light");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void initModel() {
		ClientRegistry.bindTileEntitySpecialRenderer(TrafficLightTileEntity.class, new TrafficLightRenderer());
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TrafficLightTileEntity();
	}
	
	

	@Override
	protected BaseItemTrafficLightFrame getItemVersionOfBlock() {
		return ModItems.traffic_light_frame;
	}

}
