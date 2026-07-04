package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockWigWag;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.WigWagTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.RenderBoxHelper.Box;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.RenderBoxHelper.TextureInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.RenderBoxHelper.TextureInfoCollection;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class RendererWigWag extends TileEntitySpecialRenderer<WigWagTileEntity> {

	ResourceLocation genericTexture = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/generic.png");
	ResourceLocation wigWagTexture = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/wigwag.png");
	ResourceLocation redTexture = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/red.png");
	ResourceLocation offBulbTexture = new ResourceLocation(ModRealisticTrafficControl.MODID + ":textures/blocks/lamp_off.png");

	final ArrayList<EnumFacing> facings = new ArrayList<>();

	public RendererWigWag() {
		facings.add(null);
		for (EnumFacing facing : EnumFacing.values()) {
			facings.add(facing);
		}
	}

	@Override
	public void render(WigWagTileEntity te, double x, double y, double z, float partialTicks, int destroyStage,
			float alpha) {
				IBlockState state = te.getWorld().getBlockState(te.getPos());
		
				if (state.getBlock() != ModBlocks.wig_wag)
				{
					return;
				}
				
				GlStateManager.pushMatrix();

				// Same pivot as the baked wig_wag model: rotate around block center (not an off-center
				// point), so the arm assembly stays locked to the mast at every placement rotation.
				GlStateManager.translate(x + 0.5, y + 0.545, z + 0.5);
				GlStateManager.rotate(state.getValue(BlockWigWag.ROTATION) * -22.5F, 0, 1, 0);
				GlStateManager.translate(-0.5, -0.5, -0.5);
				// Legacy TESR anchor vs center (old: translate(0.53,0.06,0.71) then (-0.5,0,-0.5) at 0┬░).
				GlStateManager.translate(0.03F, 0.06F, 0.21F);

				GlStateManager.translate(bcwc(-3.5), bcwc(16.5), bcwc(7.5));
				GlStateManager.rotate(180F, 0, 0, 1);

				// Pivot at top of mount / rod joint (model Y = 0.45). Side-to-side = rotate about Z.
				// Apply once, then draw mount + swinging parts so the whole arm assembly moves together.
				final float pivotY = (float) bcwc(0.45);
				GlStateManager.translate(0, pivotY, 0);
				GlStateManager.rotate(te.getRotation(), 0, 0, 1);
				GlStateManager.translate(0, -pivotY, 0);

				Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				Tessellator armTess = Tessellator.getInstance();
				BufferBuilder armBuilder = armTess.getBuffer();
				IBakedModel mountModel = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes()
						.getModelManager()
						.getModel(new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":wig_wag_arm_mount", "normal"));
				armBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
				addBakedQuads(armBuilder, mountModel, state);
				armTess.draw();

				
				// Tessellator tessellator = Tessellator.getInstance();
				// BufferBuilder builder = tessellator.getBuffer();
				// builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
				
				// int bright = te.getWorld().getCombinedLight(te.getPos(), 0);
				// int brightX = bright % 65536;
				// int brightY = bright / 65536;
				
				// OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightX, brightY);
				
				// renderSuspendedPole(builder);
				// tessellator.draw();
				// builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
				// renderBacking(builder);
				// tessellator.draw();
				// builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
				// renderLamp(builder, te.isActive());
				// tessellator.draw();

				armBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
				String variant = te.isActive() ? "lamp=on" : "lamp=off";
				IBakedModel armModel = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes()
						.getModelManager()
						.getModel(new ModelResourceLocation(ModRealisticTrafficControl.MODID + ":wig_wag_arm", variant));
				addBakedQuads(armBuilder, armModel, state);
				armTess.draw();

				GlStateManager.popMatrix();
	}

	private void addBakedQuads(BufferBuilder armBuilder, IBakedModel model, IBlockState state) {
		for (EnumFacing facing : facings) {
			for (BakedQuad quad : model.getQuads(state, facing, 0)) {
				armBuilder.addVertexData(quad.getVertexData());
			}
		}
	}

	private void renderSuspendedPole(BufferBuilder builder) {
		GlStateManager.translate(bcwc(-3.5), bcwc(10.5), bcwc(7.5));
		Box box = new Box(0, 0, 1, 1, 6.5, -1, new TextureInfoCollection(new TextureInfo(genericTexture, 0, 0, 1, 6),
				new TextureInfo(genericTexture, 0, 0, 1, 1), new TextureInfo(genericTexture, 0, 0, 1, 6),
				new TextureInfo(genericTexture, 0, 0, 1, 1), new TextureInfo(genericTexture, 0, 0, 1, 6),
				new TextureInfo(genericTexture, 0, 0, 1, 6)));

		box.render(builder, (rl) -> bindTexture(rl));
	}

	private void renderBacking(BufferBuilder builder) {
		GlStateManager.translate(bcwc(-2.5), bcwc(-6), 0);
		Box box = new Box(0, 0, 1, 6, 6, -1, new TextureInfoCollection(new TextureInfo(wigWagTexture, 0, 0, 16, 16),
				new TextureInfo(wigWagTexture, 0, 0, 0, 0), new TextureInfo(wigWagTexture, 0, 0, 16, 16),
				new TextureInfo(wigWagTexture, 0, 0, 0, 0), new TextureInfo(wigWagTexture, 0, 0, 0, 16),
				new TextureInfo(wigWagTexture, 0, 0, 0, 0)));

		box.render(builder, (rl) -> bindTexture(rl));
	}

	private void renderLamp(BufferBuilder builder, boolean active) {
		GlStateManager.translate(bcwc(1.7), bcwc(1.7), bcwc(0.7));
		Box box = new Box(0, 0, 1, 2.5, 2.5, -2.5,
				new TextureInfoCollection(
						new TextureInfo(active ? redTexture : offBulbTexture, 0, 0, 6, 6),
						new TextureInfo(active ? redTexture : offBulbTexture, 0, 0, 6, 1),
						new TextureInfo(active ? redTexture : offBulbTexture, 0, 0, 6, 6),
						new TextureInfo(active ? redTexture : offBulbTexture, 0, 0, 6, 1),
						new TextureInfo(active ? redTexture : offBulbTexture, 0, 0, 1, 6),
						new TextureInfo(active ? redTexture : offBulbTexture, 0, 0, 1, 6)));

		box.render(builder, (rl) -> bindTexture(rl));
	}

	private double bcwc(double blockCoord) {
		return blockCoord / 16;
	}
}