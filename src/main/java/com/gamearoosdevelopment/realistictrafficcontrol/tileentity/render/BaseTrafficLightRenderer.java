package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public abstract class BaseTrafficLightRenderer extends TileEntitySpecialRenderer<BaseTrafficLightTileEntity> {

	private static ResourceLocation blackRL = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/black.png");
	@Override
	public void render(BaseTrafficLightTileEntity te, double x, double y, double z, float partialTicks,
			int destroyStage, float alpha) {
		IBlockState state = te.getWorld().getBlockState(te.getPos());
		if (!(state.getBlock() instanceof BlockBaseTrafficLight))
		{
			return;
		}
		
		GlStateManager.pushMatrix();
		GlStateManager.disableLighting();
		
		GlStateManager.translate(x, y, z);
		double scale = (double)1/(double)16;
		GlStateManager.scale(scale, scale, scale);
		
		
		GlStateManager.translate(8, 8, 8);
		GlStateManager.rotate(state.getValue(BlockBaseTrafficLight.ROTATION) * -22.5F, 0, 1, 0);
		GlStateManager.translate(-8, -8, -8);
		
		GlStateManager.translate(0, 0, getBulbZLocation());
		
		List<BulbRenderer> bulbRenderers = getBulbRenderers();
		
		if (te.getIsPigAbove()) // Easter Egg
		{
			Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation("minecraft:textures/entity/pig/pig.png"));
		}
		else	
		{
			Minecraft.getMinecraft().renderEngine.bindTexture(blackRL);
		}
		
		// Render black texture first to avoid texture spam
		for(BulbRenderer renderer : bulbRenderers)
		{
			renderer.renderBlack(te, blackRL);
		}
		
		ResourceLocation lastRL = blackRL;
		for(BulbRenderer renderer : bulbRenderers)
		{
			lastRL = renderer.render(te, lastRL);
		}
		
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();
	}
	
	protected abstract double getBulbZLocation();
	
	protected abstract List<BulbRenderer> getBulbRenderers();
	
	public static class BulbRenderer
	{
		private double x;
		private double y;
		private int bulbSlot;
		
		public BulbRenderer(double x, double y, int bulbSlot)
		{
			this.x = x;
			this.y = y;
			this.bulbSlot = bulbSlot;
		}
		
		public ResourceLocation renderBlack(BaseTrafficLightTileEntity entity, ResourceLocation lastRL)
		{
			if ((entity.getActiveBySlot(bulbSlot) && entity.getFlashBySlot(bulbSlot) && entity.getFlashCurrentBySlot(bulbSlot)) || (entity.getActiveBySlot(bulbSlot) && !entity.getFlashBySlot(bulbSlot)))
			{
				return lastRL;
			}
			
			if (!lastRL.equals(blackRL))
			{
				Minecraft.getMinecraft().renderEngine.bindTexture(blackRL);
			}
			
			return render(entity, blackRL, true);
		}
		
		public ResourceLocation render(BaseTrafficLightTileEntity entity, ResourceLocation lastRL)
		{
			return render(entity, lastRL, false);
		}
		
		private ResourceLocation render(BaseTrafficLightTileEntity entity, ResourceLocation lastRL, boolean doRenderBlack)
		{
			if (!doRenderBlack && (!entity.getActiveBySlot(bulbSlot) || (entity.getFlashBySlot(bulbSlot) && !entity.getFlashCurrentBySlot(bulbSlot))))
			{
				return lastRL;
			}
			
			GlStateManager.translate(x, y, 0);
			
			if (doRenderBlack && !lastRL.equals(blackRL))
			{
				Minecraft.getMinecraft().renderEngine.bindTexture(blackRL);
				lastRL = blackRL;
			}
			else if (!doRenderBlack)
			{
				ResourceLocation thisRL = getResourceLocation(entity.getBulbTypeBySlot(bulbSlot));
				if (!thisRL.equals(lastRL))
				{
					Minecraft.getMinecraft().renderEngine.bindTexture(thisRL);
					lastRL = thisRL;
				}
			}
			
			Tessellator tess = Tessellator.getInstance();
			BufferBuilder builder = tess.getBuffer();
			
			builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			builder.pos(5.6, 0, 2).tex(1, 1).endVertex();
			builder.pos(5.6, 5.5, 2).tex(1, 0).endVertex();
			builder.pos(0, 5.5, 2).tex(0, 0).endVertex();
			builder.pos(0, 0, 2).tex(0, 1).endVertex();
			
			tess.draw();
			
			GlStateManager.translate(-x, -y, 0);
			
			return lastRL;
		}
	}
	
	private static ResourceLocation getResourceLocation(EnumTrafficLightBulbTypes bulbType)
	{
		switch(bulbType)
		{
			case Green:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/green.png");
			case StraightGreen:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/straight_green.png");
			case GreenArrowLeft:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/green_arrow_left.png");
			case Red:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/red_solid.png");
			case StraightRed:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/straight_red.png");
			case RedArrowLeft:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/red_arrow_left.png");
			case Yellow:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/yellow_solid.png");
			case StraightYellow:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/straight_yellow.png");
			case YellowArrowLeft:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/yellow_arrow_left.png");
			case YellowArrowLeft2:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/yellow_arrow_left.png");
			case Cross:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/cross.png");
			case DontCross:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/dontcross.png");
			case GreenArrowRight:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/green_arrow_right.png");
			case RedArrowRight:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/red_arrow_right.png");
			case NoRightTurn:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/no_right_turn.png");
			case NoLeftTurn:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/no_left_turn.png");
			case YellowArrowRight:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/yellow_arrow_right.png");
			case YellowArrowRight2:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/yellow_arrow_right.png");
			case GreenArrowUTurn:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/green_arrow_uturn.png");
			case YellowArrowUTurn:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/yellow_arrow_uturn.png");
			case YellowArrowUTurn2:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/yellow_arrow_uturn.png");
			case RedArrowUTurn:
				return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/red_arrow_uturn.png");
		}
		return new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/black.png");
	}
}
