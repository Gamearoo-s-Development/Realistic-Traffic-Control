package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.BaseTrafficLightRenderer.BulbRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;
import com.google.common.collect.ImmutableList;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class TrafficLight4HozRenderer extends BaseTrafficLightRenderer
{
	@Override
	protected double getBulbZLocation()
	{
		return 10.25;
	}
	
	@Override
	protected List<BulbRenderer> getBulbRenderers() 
	{
		return ImmutableList
				.<BulbRenderer>builder()
				.add(new BulbRenderer(-5.5, 5.3, 0))
				.add(new BulbRenderer(1, 5.3, 1))
				.add(new BulbRenderer(7, 5.3, 2))
				.add(new BulbRenderer(14, 5.3, 3))
				.build();
	}
}
