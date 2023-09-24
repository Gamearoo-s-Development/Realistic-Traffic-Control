package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockWigWag;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.WigWagTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.RenderBoxHelper.Box;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.RenderBoxHelper.TextureInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.RenderBoxHelper.TextureInfoCollection;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class RendererWigWag extends TileEntitySpecialRenderer<WigWagTileEntity> {

	ResourceLocation genericTexture = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/generic.png");
	ResourceLocation blackTexture = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/black.png");
	ResourceLocation redTexture = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/red.png");
	@Override
	public void render(WigWagTileEntity te, double x, double y, double z, float partialTicks, int destroyStage,
			float alpha) {		
		IBlockState state = te.getWorld().getBlockState(te.getPos());
		
		if (state.getBlock() != ModBlocks.wig_wag)
		{
			return;
		}
		
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y, z + 0.5);
		GlStateManager.rotate(state.getValue(BlockWigWag.ROTATION) * -22.5F, 0, 1, 0);
		GlStateManager.translate(-0.5, 0, -0.5);
		
		GlStateManager.translate(bcwc(-3.5), bcwc(16.5), bcwc(7.5));
		GlStateManager.rotate(te.getRotation(), 0, 0, 1);
		GlStateManager.translate(bcwc(3.5), bcwc(-16.5), bcwc(-7.5));
		
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder builder = tessellator.getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		
		int bright = te.getWorld().getCombinedLight(te.getPos(), 0);
        int brightX = bright % 65536;
        int brightY = bright / 65536;
        
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightX, brightY);
        
        renderSuspendedPole(builder);
        tessellator.draw();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        renderBacking(builder);
        tessellator.draw();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        renderLamp(builder, te.isActive());
        tessellator.draw();

		GlStateManager.popMatrix();
	}
	
	private void renderSuspendedPole(BufferBuilder builder)
	{
		GlStateManager.translate(bcwc(-3.5), bcwc(10.5), bcwc(7.5));
		Box box = new Box(0, 0, 1, 1, 6.5, -1, new TextureInfoCollection(new TextureInfo(genericTexture, 0, 0, 1, 6),
																			new TextureInfo(genericTexture, 0, 0, 1, 1), 
																			new TextureInfo(genericTexture, 0, 0, 1, 6), 
																			new TextureInfo(genericTexture, 0, 0, 1, 1), 
																			new TextureInfo(genericTexture, 0, 0, 1, 6), 
																			new TextureInfo(genericTexture, 0, 0, 1, 6)));
		
		box.render(builder, (rl) -> bindTexture(rl));
	}
	
	private void renderBacking(BufferBuilder builder)
	{
		GlStateManager.translate(bcwc(-2.5), bcwc(-6), 0);
		Box box = new Box(0, 0, 1, 6, 6, -1, new TextureInfoCollection(new TextureInfo(blackTexture, 0, 0, 6, 6),
																			new TextureInfo(blackTexture, 0, 0, 6, 1), 
																			new TextureInfo(blackTexture, 0, 0, 6, 6), 
																			new TextureInfo(blackTexture, 0, 0, 6, 1), 
																			new TextureInfo(blackTexture, 0, 0, 1, 6), 
																			new TextureInfo(blackTexture, 0, 0, 1, 6)));
		
		box.render(builder, (rl) -> bindTexture(rl));
	}
	
	private void renderLamp(BufferBuilder builder, boolean active)
	{
		GlStateManager.translate(bcwc(1.5), bcwc(1.5), bcwc(1));
		Box box = new Box(0, 0, 1, 3, 3, -1, new TextureInfoCollection(new TextureInfo(active ? redTexture : blackTexture, 0, 0, 6, 6),
																			new TextureInfo(active ? redTexture : blackTexture, 0, 0, 6, 1), 
																			new TextureInfo(active ? redTexture : blackTexture, 0, 0, 6, 6), 
																			new TextureInfo(active ? redTexture : blackTexture, 0, 0, 6, 1), 
																			new TextureInfo(active ? redTexture : blackTexture, 0, 0, 1, 6), 
																			new TextureInfo(active ? redTexture : blackTexture, 0, 0, 1, 6)));
		
		box.render(builder, (rl) -> bindTexture(rl));
	}
	
	// Block Coordinate -> World Coordinate
	private double bcwc(double blockCoord)
	{
		return blockCoord / 16;
	}
}
