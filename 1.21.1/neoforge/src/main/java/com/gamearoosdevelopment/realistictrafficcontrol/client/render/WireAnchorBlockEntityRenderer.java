package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.WireAnchorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Port of 1.12.2 {@code TESRWireAnchor} as a BER. */
public class WireAnchorBlockEntityRenderer implements BlockEntityRenderer<WireAnchorBlockEntity> {

    public WireAnchorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WireAnchorBlockEntity te, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        if (te.getLevel() == null) {
            return;
        }
        BlockPos origin = te.getBlockPos();
        int segments = 16;
        double sag = 0.5;
        double swayBase = 0.05;
        if (te.getLevel().isThundering()) {
            swayBase = 0.09;
        } else if (te.getLevel().isRaining()) {
            swayBase = 0.06;
        }
        double tTime = (te.getLevel().getGameTime() + partialTick) / 10.0;
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

        for (BlockPos target : te.connections) {
            if (target == null || target.compareTo(origin) <= 0) {
                continue;
            }
            Vec3 start = new Vec3(origin.getX() + 0.5, origin.getY() + 0.40, origin.getZ() + 0.5);
            Vec3 end = new Vec3(target.getX() + 0.5, target.getY() + 0.40, target.getZ() + 0.5);
            double sway = swayBase;
            for (int i = 0; i < segments; i++) {
                double t0 = (double) i / segments;
                double t1 = (double) (i + 1) / segments;
                Vec3 p0 = sagPoint(start, end, t0, sag, tTime, sway);
                Vec3 p1 = sagPoint(start, end, t1, sag, tTime, sway);
                poseStack.pushPose();
                poseStack.translate(-origin.getX(), -origin.getY(), -origin.getZ());
                var matrix = poseStack.last().pose();
                consumer.addVertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z).setColor(0, 0, 0, 255)
                        .setNormal(0, 1, 0);
                consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).setColor(0, 0, 0, 255)
                        .setNormal(0, 1, 0);
                poseStack.popPose();
            }
        }
    }

    private static Vec3 sagPoint(Vec3 start, Vec3 end, double t, double sag, double tTime, double sway) {
        double px = start.x + (end.x - start.x) * t;
        double py = start.y + (end.y - start.y) * t - Math.sin(t * Math.PI) * sag;
        double pz = start.z + (end.z - start.z) * t;
        px += Math.sin(tTime + t * Math.PI * 2) * sway;
        return new Vec3(px, py, pz);
    }
}
