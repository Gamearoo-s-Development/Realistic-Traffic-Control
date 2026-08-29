package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
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

    private static final int MAX_WIRE_SPAN = 48;
    private static final int SEGMENTS = 16;

    public WireAnchorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WireAnchorBlockEntity te, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        if (te.getLevel() == null) {
            return;
        }
        BlockPos origin = te.getBlockPos();
        double sag = 0.5;
        double swayBase = te.getLevel().isThundering() ? 0.09 : te.getLevel().isRaining() ? 0.06 : 0.05;
        double tTime = (te.getLevel().getGameTime() + partialTick) / 10.0;
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

        Vec3 start = new Vec3(0.5, 0.40, 0.5);

        for (BlockPos target : te.connections) {
            if (target == null || target.compareTo(origin) <= 0) {
                continue;
            }
            if (!te.getLevel().getBlockState(target).is(ModBlocks.WIRE_ANCHOR.get())) {
                continue;
            }
            int dx = target.getX() - origin.getX();
            int dy = target.getY() - origin.getY();
            int dz = target.getZ() - origin.getZ();
            if (dx * dx + dy * dy + dz * dz > MAX_WIRE_SPAN * MAX_WIRE_SPAN) {
                continue;
            }

            Vec3 end = new Vec3(dx + 0.5, dy + 0.40, dz + 0.5);
            PoseStack.Pose pose = poseStack.last();
            var matrix = pose.pose();

            for (int i = 0; i < SEGMENTS; i++) {
                double t0 = (double) i / SEGMENTS;
                double t1 = (double) (i + 1) / SEGMENTS;
                Vec3 p0 = sagPoint(start, end, t0, sag, tTime, swayBase);
                Vec3 p1 = sagPoint(start, end, t1, sag, tTime, swayBase);
                consumer.addVertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z).setColor(0, 0, 0, 255)
                        .setNormal(pose, 0f, 1f, 0f);
                consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).setColor(0, 0, 0, 255)
                        .setNormal(pose, 0f, 1f, 0f);
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
