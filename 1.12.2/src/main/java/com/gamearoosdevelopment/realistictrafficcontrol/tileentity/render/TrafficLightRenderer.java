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

public class TrafficLightRenderer extends BaseTrafficLightRenderer {

	@Override
	protected double getBulbZLocation() {
		return -5.6;
	}

	@Override
	protected List<BulbRenderer> getBulbRenderers() {
		return ImmutableList
		.<BulbRenderer>builder()
		.add(new BulbRenderer(5.2, 9, 0))
		.add(new BulbRenderer(5.2, 2.5, 1))
		.add(new BulbRenderer(5.2, -4, 2))
		.build();
	}
	/*ResourceLocation black = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/black.png");
	
	@Override
	public void render(TrafficLightTileEntity te, double x, double y, double z, float partialTicks, int destroyStage,
			float alpha) {
		GL11.glPushMatrix();
		GlStateManager.disableLighting();
		GL11.glTranslated(x, y, z);
		float scale = 1F/16F;
		GL11.glScalef(scale, scale, scale);
		
		GL11.glTranslated(8, 8, 8);
		GL11.glRotated(te.getYRotation(), 0, 1, 0);
		GL11.glTranslated(-8, -8, -8);
		
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		
		// Render black textures first
		bindTexture(black);
		if (!te.getActiveBySlot(0) || (te.getFlashBySlot(0) && !te.getFlashCurrentBySlot(0)))
		{
			GL11.glTranslated(5.2, 9, 10.1);
			builder.pos(5.6, 0, 0).tex(1, 0).endVertex();
			builder.pos(5.6, 5.5, 0).tex(1, 1).endVertex();
			builder.pos(0, 5.5, 0).tex(0, 1).endVertex();
			builder.pos(0, 0, 0).tex(0, 0).endVertex();
			builder.endVertex();
			tess.draw();
			builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			GL11.glTranslated(-5.2, -9, -10.1);
		}
		if (!te.getActiveBySlot(1) || (te.getFlashBySlot(1) && !te.getFlashCurrentBySlot(1)))
		{
			GL11.glTranslated(5.2, 2.5, 10.1);
			builder.pos(5.6, 0, 0).tex(1, 0).endVertex();
			builder.pos(5.6, 5.5, 0).tex(1, 1).endVertex();
			builder.pos(0, 5.5, 0).tex(0, 1).endVertex();
			builder.pos(0, 0, 0).tex(0, 0).endVertex();
			builder.endVertex();
			tess.draw();
			builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			GL11.glTranslated(-5.2, -2.5, -10.1);
		}
		if (!te.getActiveBySlot(2) || (te.getFlashBySlot(2) && !te.getFlashCurrentBySlot(2)))
		{
			GL11.glTranslated(5.2, -4, 10.1);
			builder.pos(5.6, 0, 0).tex(1, 0).endVertex();
			builder.pos(5.6, 5.5, 0).tex(1, 1).endVertex();
			builder.pos(0, 5.5, 0).tex(0, 1).endVertex();
			builder.pos(0, 0, 0).tex(0, 0).endVertex();
			builder.endVertex();
			tess.draw();
			builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			GL11.glTranslated(-5.2, 4, -10.1);
		}
		tess.draw();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		
		// Render specific arrows
		ResourceLocation lastRL = null;
		
		if (te.getActiveBySlot(0) && (!te.getFlashBySlot(0) || te.getFlashCurrentBySlot(0)))
		{
			EnumTrafficLightBulbTypes bulb = te.getBulbTypeBySlot(0);
			ResourceLocation thisRL = getResourceLocation(bulb);
			bindTexture(thisRL);
			lastRL = thisRL;
			
			GL11.glTranslated(5.2, 9, 10.1);
			builder.pos(5.6, 0, 0).tex(1, 0).endVertex();
			builder.pos(5.6, 5.5, 0).tex(1, 1).endVertex();
			builder.pos(0, 5.5, 0).tex(0, 1).endVertex();
			builder.pos(0, 0, 0).tex(0, 0).endVertex();
			builder.endVertex();
			tess.draw();
			builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			GL11.glTranslated(-5.2, -9, -10.1);
		}
		
		if (te.getActiveBySlot(1) && (!te.getFlashBySlot(1) || te.getFlashCurrentBySlot(1)))
		{
			EnumTrafficLightBulbTypes bulb = te.getBulbTypeBySlot(1);
			
			ResourceLocation thisRL = getResourceLocation(bulb);
			
			if (thisRL != lastRL)
			{
				bindTexture(getResourceLocation(bulb));
				lastRL = thisRL;
			}
			
			GL11.glTranslated(5.2, 2.5, 10.1);
			builder.pos(5.6, 0, 0).tex(1, 0).endVertex();
			builder.pos(5.6, 5.5, 0).tex(1, 1).endVertex();
			builder.pos(0, 5.5, 0).tex(0, 1).endVertex();
			builder.pos(0, 0, 0).tex(0, 0).endVertex();
			builder.endVertex();
			tess.draw();
			builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			GL11.glTranslated(-5.2, -2.5, -10.1);
		}
		
		if (te.getActiveBySlot(2) && (!te.getFlashBySlot(2) || te.getFlashCurrentBySlot(2)))
		{
			EnumTrafficLightBulbTypes bulb = te.getBulbTypeBySlot(2);
			
			ResourceLocation thisRL = getResourceLocation(bulb);
			
			if (thisRL != lastRL)
			{
				bindTexture(getResourceLocation(bulb));
				lastRL = thisRL;
			}
			
			GL11.glTranslated(5.2, -4, 10.1);
			builder.pos(5.6, 0, 0).tex(1, 0).endVertex();
			builder.pos(5.6, 5.5, 0).tex(1, 1).endVertex();
			builder.pos(0, 5.5, 0).tex(0, 1).endVertex();
			builder.pos(0, 0, 0).tex(0, 0).endVertex();
			tess.draw();
			builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			GL11.glTranslated(-5.2, 4, -10.1);
		}
		
		tess.draw();
		GlStateManager.enableLighting();
		GL11.glPopMatrix();
	}
	
	private ResourceLocation getResourceLocation(EnumTrafficLightBulbTypes bulbType)
	{
		switch(bulbType)
		{
			case Green:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/green.png");
			case GreenArrowLeft:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/green_arrow_left.png");
			case Red:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/red.png");
			case RedArrowLeft:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/red_arrow_left.png");
			case Yellow:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/yellow_solid.png");
			case YellowArrowLeft:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/yellow_arrow_left.png");
		}
		return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/black.png");
	}*/
}
