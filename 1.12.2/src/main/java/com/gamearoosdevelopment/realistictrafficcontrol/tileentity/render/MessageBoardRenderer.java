package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockMessageBoard;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardTileEntity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/** Renders the oversized portable board without illegal out-of-block baked-model geometry. */
public class MessageBoardRenderer extends TileEntitySpecialRenderer<MessageBoardTileEntity> {
	private static final int DARK_METAL = 0x151719;
	private static final int EDGE_METAL = 0x303336;
	private static final int TRAILER_ORANGE = 0xD96A08;
	private static final int RUBBER = 0x111111;
	private static final double SCREEN_Z = 0.283;

	@Override
	public void render(MessageBoardTileEntity board, double x, double y, double z, float partialTicks,
			int destroyStage, float alpha) {
		IBlockState state = board.getWorld().getBlockState(board.getPos());
		float rotation = state.getValue(BlockMessageBoard.ROTATION) * -22.5F;

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y, z + 0.5);
		GlStateManager.rotate(rotation, 0, 1, 0);
		GlStateManager.translate(-0.5, 0, -0.5);
		GlStateManager.disableLighting();
		GlStateManager.disableTexture2D();
		GlStateManager.disableCull();

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

		// Trailer chassis, draw bar, mast, stabilizers, and wheels.
		drawBox(builder, -0.60, 0.13, 0.34, 1.60, 0.25, 0.66, TRAILER_ORANGE);
		drawBox(builder, -1.10, 0.15, 0.44, -0.60, 0.21, 0.56, TRAILER_ORANGE);
		drawBox(builder, 0.43, 0.22, 0.43, 0.57, 1.76, 0.57, TRAILER_ORANGE);
		drawBox(builder, -0.68, 0.05, 0.45, -0.54, 0.20, 0.55, TRAILER_ORANGE);
		drawBox(builder, 1.54, 0.05, 0.45, 1.68, 0.20, 0.55, TRAILER_ORANGE);
		drawBox(builder, -0.55, 0.00, 0.23, -0.30, 0.38, 0.77, RUBBER);
		drawBox(builder, 1.30, 0.00, 0.23, 1.55, 0.38, 0.77, RUBBER);
		drawBox(builder, -0.49, 0.10, 0.20, -0.36, 0.28, 0.23, EDGE_METAL);
		drawBox(builder, 1.36, 0.10, 0.20, 1.49, 0.28, 0.23, EDGE_METAL);

		// Three-block-wide cabinet and a raised metal bezel.
		drawBox(builder, -0.72, 1.72, 0.32, 1.72, 2.98, 0.68, DARK_METAL);
		drawBox(builder, -0.76, 1.68, 0.27, -0.65, 3.00, 0.73, EDGE_METAL);
		drawBox(builder, 1.65, 1.68, 0.27, 1.76, 3.00, 0.73, EDGE_METAL);
		drawBox(builder, -0.65, 2.87, 0.27, 1.65, 3.00, 0.73, EDGE_METAL);
		drawBox(builder, -0.65, 1.68, 0.27, 1.65, 1.81, 0.73, EDGE_METAL);
		drawBox(builder, -0.65, 1.81, SCREEN_Z, 1.65, 2.87, 0.30, 0x050606);
		tess.draw();

		if (board.getMode() == MessageBoardTileEntity.DisplayMode.TEXT) {
			GlStateManager.enableTexture2D();
			renderText(board);
			GlStateManager.disableTexture2D();
		} else if (board.getMode() == MessageBoardTileEntity.DisplayMode.ARROW_LEFT
				|| board.getMode() == MessageBoardTileEntity.DisplayMode.ARROW_RIGHT) {
			renderArrow(board);
		} else if (board.getMode() == MessageBoardTileEntity.DisplayMode.CAUTION) {
			renderCaution(board);
		}

		GlStateManager.enableCull();
		GlStateManager.enableTexture2D();
		GlStateManager.enableLighting();
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.popMatrix();
	}

	private void renderText(MessageBoardTileEntity board) {
		GlStateManager.translate(0.5, 2.73, SCREEN_Z - 0.002);
		// The font quad is viewed from the opposite winding on this cabinet. Flip
		// only the display's X axis; rotating the whole trailer moves it to the rear.
		double scale = board.getTextScale() / 92.0;
		GlStateManager.scale(-scale, -scale, 1);
		for (int i = 0; i < MessageBoardTileEntity.MAX_LINES; i++) {
			String line = board.getStyledLine(i);
			int width = getFontRenderer().getStringWidth(line);
			getFontRenderer().drawString(line, -width / 2, i * 22, litColor(board));
		}
	}

	private void renderArrow(MessageBoardTileEntity board) {
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.getBuffer();
		builder.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
		int color = litColor(board);
		double z = SCREEN_Z - 0.012;
		if (board.getMode() == MessageBoardTileEntity.DisplayMode.ARROW_LEFT) {
			// The display face is mirrored relative to the cabinet model, so +X is
			// visually left from the road-facing side.
			triangle(builder, 0.58, 1.94, 1.52, 2.34, 0.58, 2.74, z, color);
			quadAsTriangles(builder, -0.48, 2.22, 0.78, 2.46, z, color);
		} else {
			triangle(builder, 0.42, 1.94, -0.52, 2.34, 0.42, 2.74, z, color);
			quadAsTriangles(builder, 0.22, 2.22, 1.48, 2.46, z, color);
		}
		tess.draw();
	}

	private void renderCaution(MessageBoardTileEntity board) {
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		int color = litColor(board);
		double z = SCREEN_Z - 0.012;
		drawDiamond(builder, -0.43, 2.05, 0.11, 0.14, z, color);
		drawDiamond(builder, 1.43, 2.05, 0.11, 0.14, z, color);
		drawDiamond(builder, -0.43, 2.63, 0.11, 0.14, z, color);
		drawDiamond(builder, 1.43, 2.63, 0.11, 0.14, z, color);
		tess.draw();

		GlStateManager.enableTexture2D();
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.5, 2.46, SCREEN_Z - 0.014);
		GlStateManager.scale(-1.0 / 40.0, -1.0 / 40.0, 1);
		String caution = "CAUTION";
		getFontRenderer().drawString(caution, -getFontRenderer().getStringWidth(caution) / 2, 0, color);
		GlStateManager.popMatrix();
		GlStateManager.disableTexture2D();
	}

	private int litColor(MessageBoardTileEntity board) {
		int color = board.getColor() & 0xFFFFFF;
		float b = board.getBrightness();
		int r = (int) (((color >> 16) & 255) * b);
		int g = (int) (((color >> 8) & 255) * b);
		int blue = (int) ((color & 255) * b);
		return (r << 16) | (g << 8) | blue;
	}

	private void triangle(BufferBuilder builder, double x1, double y1, double x2, double y2,
			double x3, double y3, double z, int color) {
		vertex(builder, x1, y1, z, color);
		vertex(builder, x2, y2, z, color);
		vertex(builder, x3, y3, z, color);
	}

	private void quadAsTriangles(BufferBuilder builder, double x1, double y1, double x2, double y2,
			double z, int color) {
		triangle(builder, x1, y1, x1, y2, x2, y2, z, color);
		triangle(builder, x1, y1, x2, y2, x2, y1, z, color);
	}

	private void drawDiamond(BufferBuilder builder, double cx, double cy, double halfWidth,
			double halfHeight, double z, int color) {
		vertex(builder, cx, cy + halfHeight, z, color);
		vertex(builder, cx + halfWidth, cy, z, color);
		vertex(builder, cx, cy - halfHeight, z, color);
		vertex(builder, cx - halfWidth, cy, z, color);
	}

	private void quad(BufferBuilder b, double x1, double y1, double x2, double y2, double z, int color) {
		vertex(b, x1, y1, z, color); vertex(b, x1, y2, z, color);
		vertex(b, x2, y2, z, color); vertex(b, x2, y1, z, color);
	}

	private void drawBox(BufferBuilder b, double x1, double y1, double z1, double x2, double y2, double z2, int c) {
		// Front/back.
		vertex(b,x1,y1,z1,c); vertex(b,x1,y2,z1,c); vertex(b,x2,y2,z1,c); vertex(b,x2,y1,z1,c);
		vertex(b,x2,y1,z2,c); vertex(b,x2,y2,z2,c); vertex(b,x1,y2,z2,c); vertex(b,x1,y1,z2,c);
		// Left/right.
		vertex(b,x1,y1,z2,c); vertex(b,x1,y2,z2,c); vertex(b,x1,y2,z1,c); vertex(b,x1,y1,z1,c);
		vertex(b,x2,y1,z1,c); vertex(b,x2,y2,z1,c); vertex(b,x2,y2,z2,c); vertex(b,x2,y1,z2,c);
		// Top/bottom.
		vertex(b,x1,y2,z1,c); vertex(b,x1,y2,z2,c); vertex(b,x2,y2,z2,c); vertex(b,x2,y2,z1,c);
		vertex(b,x1,y1,z2,c); vertex(b,x1,y1,z1,c); vertex(b,x2,y1,z1,c); vertex(b,x2,y1,z2,c);
	}

	private void vertex(BufferBuilder b, double x, double y, double z, int color) {
		b.pos(x, y, z).color((color >> 16) & 255, (color >> 8) & 255, color & 255, 255).endVertex();
	}
}
