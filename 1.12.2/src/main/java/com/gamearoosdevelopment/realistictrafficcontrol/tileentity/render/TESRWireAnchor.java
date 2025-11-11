package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;
import org.lwjgl.opengl.GL11;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TileEntityWireAnchor;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class TESRWireAnchor extends TileEntitySpecialRenderer<TileEntityWireAnchor> {

    @Override
    public void render(TileEntityWireAnchor te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te.connections == null) return;
        BlockPos origin = te.getPos();

        int segments = 16;
        double sag = 0.5;
        double swayBase = 0.05;

        for (BlockPos target : te.connections) {
            if (target == null) continue;
            // Only render from the block with the lower position (to avoid duplicate wire)
            if (target.compareTo(origin) <= 0) continue;

            Vec3d start = new Vec3d(x + 0.5, y + 0.40, z + 0.5);
            double dx = target.getX() - te.getPos().getX();
            double dy = target.getY() - te.getPos().getY();
            double dz = target.getZ() - te.getPos().getZ();
            Vec3d end = new Vec3d(x + dx, y + dy, z + dz).addVector(0.5, 0.40, 0.5);

            double sway = swayBase;
            if (te.getWorld().isThundering()) {
                sway = 0.09;
            } else if (te.getWorld().isRaining()) {
                sway = 0.06;
            }

            double tTime = (te.getWorld().getTotalWorldTime() + partialTicks) / 10.0;

            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.glLineWidth(2.0f);
            GlStateManager.color(0f, 0f, 0f, 1f);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();
            buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);

            for (int i = 0; i <= segments; i++) {
                double t = (double) i / segments;
                double px = start.x + (end.x - start.x) * t;
                double py = start.y + (end.y - start.y) * t - Math.sin(t * Math.PI) * sag;
                double pz = start.z + (end.z - start.z) * t;

                double swayOffset = Math.sin(tTime + t * Math.PI * 2) * sway;
                px += swayOffset;

                buffer.pos(px, py, pz).endVertex();
            }

            tess.draw();
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }
}