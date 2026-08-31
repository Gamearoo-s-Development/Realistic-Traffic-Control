package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockDigitalSign;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignHorizontalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignVerticalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignTileEntity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public class SignRenderer extends TileEntitySpecialRenderer<SignTileEntity> {
	// The digital cabinet model is shifted 13 pixels toward its mounting pole.
	// Keep the TESR-rendered face and connecting bezel flush with that model.
	private static final double DIGITAL_MODEL_OFFSET = 13.0 / 16.0;
	private static final double DIGITAL_FACE_Z = -0.103 + DIGITAL_MODEL_OFFSET;
	private static final double DIGITAL_BEZEL_Z = -0.13 + DIGITAL_MODEL_OFFSET;

	@Override
	public void render(SignTileEntity te, double x, double y, double z, float partialTicks, int destroyStage,
			float alpha) {
		Sign sign = te.getSign();
		if (sign == null)
		{
			return;
		}
		
		
		TextureManager texManager = Minecraft.getMinecraft().getRenderManager().renderEngine;
				
		IBlockState block = te.getWorld().getBlockState(te.getPos());
		if (!(block.getBlock() instanceof BlockSign) && !(block.getBlock() instanceof BlockDigitalSign))
		{
			return;
		}
		float rotation = block.getBlock() instanceof BlockSign
				? block.getValue(BlockSign.ROTATION) * -22.5F
				: block.getValue(BlockDigitalSign.ROTATION) * -22.5F;
		boolean digital = block.getBlock() instanceof BlockDigitalSign;
		
		
		
		GlStateManager.pushMatrix();
		texManager.bindTexture(sign.getFrontImageResourceLocation());
		if (digital) {
			GlStateManager.disableLighting();
			GlStateManager.translate(x + 0.5, y, z + 0.5);
			GlStateManager.rotate(rotation, 0, 1, 0);
			boolean connectedLeft = BlockDigitalSign.hasNeighbor(te.getWorld(), te.getPos(), block, -1, 0);
			boolean connectedRight = BlockDigitalSign.hasNeighbor(te.getWorld(), te.getPos(), block, 1, 0);
			boolean connectedDown = BlockDigitalSign.hasNeighbor(te.getWorld(), te.getPos(), block, 0, -1);
			boolean connectedUp = BlockDigitalSign.hasNeighbor(te.getWorld(), te.getPos(), block, 0, 1);
			renderDigitalBezel(connectedLeft, connectedRight, connectedDown, connectedUp);
			texManager.bindTexture(sign.getFrontImageResourceLocation());
			double left = connectedLeft ? 0 : 0.0625;
			double right = connectedRight ? 1 : 0.9375;
			double bottom = connectedDown ? 0 : 0.0625;
			double top = connectedUp ? 1 : 0.9375;
			GlStateManager.translate(-0.5 + left, bottom, DIGITAL_FACE_Z);
			GlStateManager.scale(right - left, top - bottom, 1);
		} else {
		GlStateManager.translate(x, y, z);
		if (rotation == -90) {
		GlStateManager.translate(1.44, 0.4, 0.41);
		} else if (rotation == 0) {
			GlStateManager.translate(0.4, 0.4, -0.44);
			} else if (rotation == -180)  {
				GlStateManager.translate(0.6, 0.4, 1.44);
			} else {
			GlStateManager.translate(-0.44, 0.4, 0.59);
		}
		GlStateManager.rotate(rotation, 0, 1, 0);
		GlStateManager.translate(-0.4, -0.4, 0.06875);
		}
		
		// Draw front
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		
		if (digital) {
			// The digital cabinet faces the opposite side of the legacy freestanding
			// sign transform, so wind its selected front toward the player.
			builder.pos(1, 1, 0).tex(0, 0).endVertex();
			builder.pos(1, 0, 0).tex(0, 1).endVertex();
			builder.pos(0, 0, 0).tex(1, 1).endVertex();
			builder.pos(0, 1, 0).tex(1, 0).endVertex();
		} else {
			builder.pos(0, 1, 0).tex(0, 0).endVertex();
			builder.pos(0, 0, 0).tex(0, 1).endVertex();
			builder.pos(1, 0, 0).tex(1, 1).endVertex();
			builder.pos(1, 1, 0).tex(1, 0).endVertex();
		}
		
		tess.draw();
		
		// === Draw text ===
		if (sign.getTextLines().size() > 0)
		{
			// Scale to sign
			FontRenderer fontRenderer = getFontRenderer();
			GlStateManager.scale(1F / fontRenderer.FONT_HEIGHT, -1F / fontRenderer.FONT_HEIGHT, 1);
			GlStateManager.translate(0, -9, 0.01);
			GlStateManager.scale(1 / 16F, 1 / 16F, 1);
			
			for(int i = 0; i < sign.getTextLines().size(); i++)
			{
				Sign.TextLine textLine = sign.getTextLines().get(i);
				
				GlStateManager.translate(textLine.getX() * fontRenderer.FONT_HEIGHT, textLine.getY() * fontRenderer.FONT_HEIGHT, 0);
				GlStateManager.scale(textLine.getXScale(), textLine.getYScale(), 1);
				if (textLine.getvAlign() == SignVerticalAlignment.Center)
				{
					GlStateManager.translate(0, -fontRenderer.FONT_HEIGHT / 2.0, 0);
				}
				else if (textLine.getvAlign() == SignVerticalAlignment.Bottom)
				{
					GlStateManager.translate(0,  -fontRenderer.FONT_HEIGHT, 0);
				}
				
				if (textLine.gethAlign() == SignHorizontalAlignment.Center)
				{
					GlStateManager.translate(-(textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) / 2.0, 0, 0);
				}
				else if (textLine.gethAlign() == SignHorizontalAlignment.Right)
				{
					GlStateManager.translate(-textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT, 0, 0);
				}
				
				int textWidth = fontRenderer.getStringWidth(te.getTextLine(i));
				if (textWidth > 0)
				{
					double widthScaling = ((textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) / (textWidth));
					if (widthScaling > 1)
					{
						widthScaling = 1;
					}
					
					GlStateManager.scale(widthScaling, 1, 1);
					int textX = 0;
					if (textLine.gethAlign() == SignHorizontalAlignment.Center && widthScaling == 1)
					{
						textX = (int)((textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) / 2) - (textWidth / 2);
					}
					else if (textLine.gethAlign() == SignHorizontalAlignment.Right)
					{
						textX = (int)(textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) - textWidth;
					}
					fontRenderer.drawString(te.getTextLine(i), textX + 1, 1, textLine.getColor());
					GlStateManager.scale(1 / widthScaling, 1, 1);
				}
				
				if (textLine.gethAlign() == SignHorizontalAlignment.Center)
				{
					GlStateManager.translate((textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT) / 2.0, 0, 0);
				}
				else if (textLine.gethAlign() == SignHorizontalAlignment.Right)
				{
					GlStateManager.translate(textLine.getScaleAdjustedWidth() * fontRenderer.FONT_HEIGHT, 0, 0);
				}
				
				if (textLine.getvAlign() == SignVerticalAlignment.Center)
				{
					GlStateManager.translate(0, fontRenderer.FONT_HEIGHT / 2.0, 0);
				}
				else if (textLine.getvAlign() == SignVerticalAlignment.Bottom)
				{
					GlStateManager.translate(0,  fontRenderer.FONT_HEIGHT, 0);
				}
				GlStateManager.scale(1 / textLine.getXScale(), 1 / textLine.getYScale(), 1);
				GlStateManager.translate(-textLine.getX() * fontRenderer.FONT_HEIGHT, -textLine.getY() * fontRenderer.FONT_HEIGHT, 0);
			}
			
			// Reverse scale to sign
			GlStateManager.scale(16F, 16F, 1);
			GlStateManager.translate(0, 9, -0.0001);
			GlStateManager.scale(fontRenderer.FONT_HEIGHT, -fontRenderer.FONT_HEIGHT, 1);
		}
		
		// Electronic cabinets already have a modeled rear panel. Drawing the generic
		// sign back here placed it in front of the selected digital image.
		if (!digital) {
			GlStateManager.translate(0, 0, -0.01);
			GlStateManager.color(1, 1, 1);
			texManager.bindTexture(sign.getBackImageResourceLocation());
			builder = tess.getBuffer();
			builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			builder.pos(1, 1, 0).tex(0, 0).endVertex();
			builder.pos(1, 0, 0).tex(0, 1).endVertex();
			builder.pos(0, 0, 0).tex(1, 1).endVertex();
			builder.pos(0, 1, 0).tex(1, 0).endVertex();
			tess.draw();
		}
		if (digital) GlStateManager.enableLighting();
		GlStateManager.popMatrix();
	}

	private void renderDigitalBezel(boolean leftConnected, boolean rightConnected, boolean downConnected, boolean upConnected) {
		GlStateManager.disableTexture2D();
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		if (!leftConnected) bezelQuad(builder, -0.5, 0, -0.4375, 1);
		if (!rightConnected) bezelQuad(builder, 0.4375, 0, 0.5, 1);
		if (!downConnected) bezelQuad(builder, -0.4375, 0, 0.4375, 0.0625);
		if (!upConnected) bezelQuad(builder, -0.4375, 0.9375, 0.4375, 1);
		tess.draw();
		GlStateManager.enableTexture2D();
	}

	private void bezelQuad(BufferBuilder builder, double x1, double y1, double x2, double y2) {
		int color = 82;
		builder.pos(x1, y1, DIGITAL_BEZEL_Z).color(color, color, color, 255).endVertex();
		builder.pos(x1, y2, DIGITAL_BEZEL_Z).color(color, color, color, 255).endVertex();
		builder.pos(x2, y2, DIGITAL_BEZEL_Z).color(color, color, color, 255).endVertex();
		builder.pos(x2, y1, DIGITAL_BEZEL_Z).color(color, color, color, 255).endVertex();
	}
}
