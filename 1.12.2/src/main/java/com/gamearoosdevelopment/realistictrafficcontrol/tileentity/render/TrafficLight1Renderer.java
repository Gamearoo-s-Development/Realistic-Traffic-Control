package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;
import com.google.common.collect.ImmutableList;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class TrafficLight1Renderer extends BaseTrafficLightRenderer
{
	@Override
	protected double getBulbZLocation()
	{
		return -5.6;
	}
	
	@Override
	protected List<BulbRenderer> getBulbRenderers()
	{
		return ImmutableList
				.<BulbRenderer>builder()
				.add(new BulbRenderer(5.2, 5.5, 0))
				.build();
	}
}
