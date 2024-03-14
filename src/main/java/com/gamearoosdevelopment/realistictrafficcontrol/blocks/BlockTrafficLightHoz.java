package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightHozTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.TrafficLightRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.*;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockTrafficLightHoz extends BlockBaseTrafficLight {

	public BlockTrafficLightHoz()
	{
		super("traffic_light_hoz");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void initModel() {
		ClientRegistry.bindTileEntitySpecialRenderer(TrafficLightHozTileEntity.class, new TrafficLightHozRenderer());
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TrafficLightHozTileEntity();
	}

	@Override
	protected BaseItemTrafficLightFrame getItemVersionOfBlock() {
		return ModItems.traffic_light_hoz_frame;
	}

}
