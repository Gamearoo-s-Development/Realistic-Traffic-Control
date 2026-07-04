package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockVerticalWigWag;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.VerticalWigWagTileEntity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;

public class VerticalWigWagRenderer extends TileEntitySpecialRenderer<VerticalWigWagTileEntity>
{
	final ArrayList<EnumFacing> facings = new ArrayList<>();
	
	public VerticalWigWagRenderer() {
		facings.add(null);
		for(EnumFacing facing : EnumFacing.values())
		{
			facings.add(facing);
		}
	}
	
	@Override
	public void render(VerticalWigWagTileEntity te, double x, double y, double z, float partialTicks, int destroyStage,
			float alpha) {		
		IBlockState state = te.getWorld().getBlockState(te.getPos());
		
		if (state.getBlock() != ModBlocks.vertical_wig_wag)
		{
			return;
		}
		
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
		GlStateManager.rotate(state.getValue(BlockVerticalWigWag.ROTATION) * -22.5F, 0, 1, 0);
		GlStateManager.translate(-0.5, -0.5, -0.5);
		GlStateManager.translate(0.5, 0.4375, 0.27);

		// Same pivot as horizontal arm mount (joint Y = 0.45 in 16ths): whole mount + pendulum side-to-side.
		final float pivotY = 0.45F / 16F;
		GlStateManager.translate(0, pivotY, 0);
		GlStateManager.rotate(te.getRotation(), 0, 0, 1);
		GlStateManager.translate(0, -pivotY, 0);
		
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
		IBakedModel mountModel = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getModel(new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":vertical_wig_wag_arm_mount", "normal"));
		for (EnumFacing facing : facings)
		{
			for (BakedQuad quad : mountModel.getQuads(state, facing, 0))
			{
				builder.addVertexData(quad.getVertexData());
			}
		}
		tess.draw();

		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
		String variant = te.isActive() ? "lamp=on" : "lamp=off";
		IBakedModel armModel = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getModel(new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":vertical_wig_wag_arm", variant));
		for(EnumFacing facing : facings)
		{
			for(BakedQuad quad : armModel.getQuads(state, facing, 0))
			{
				builder.addVertexData(quad.getVertexData());
			}
		}
		
		tess.draw();

		GlStateManager.popMatrix();
	}
}