package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockType3BarrierBase;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.Type3BarrierBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

/** Port of 1.12.2 {@code RendererType3Barrier}. */
public class Type3BarrierBlockEntityRenderer implements BlockEntityRenderer<Type3BarrierBlockEntity> {

    private static final ResourceLocation ROAD_CLOSED = ResourceLocation.fromNamespaceAndPath(
            ModRealisticTrafficControl.MODID, "textures/block/road_closed_sign.png");
    private static final ResourceLocation GENERIC = ResourceLocation.fromNamespaceAndPath(
            ModRealisticTrafficControl.MODID, "textures/block/generic.png");

    public Type3BarrierBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Type3BarrierBlockEntity te, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        if (te.getLevel() == null) {
            return;
        }
        BlockState currentState = te.getBlockState();
        if (!(currentState.getBlock() instanceof BlockType3BarrierBase base)) {
            return;
        }
        currentState = base.computeActualState(currentState, te.getLevel(), te.getBlockPos());

        if (te.getRenderSign() && currentState.getValue(RTCProperties.ISFURTHESTLEFT)) {
            renderSpanningSign(te, poseStack, buffer, packedLight, currentState);
        }
        if (te.getRenderThisSign()) {
            renderSecondarySign(te, poseStack, buffer, packedLight, currentState);
        }
    }

    private void renderSpanningSign(Type3BarrierBlockEntity te, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, BlockState currentState) {
        BlockPos below = te.getBlockPos().below();
        VoxelShape belowShape = te.getLevel().getBlockState(below).getShape(te.getLevel(), below);
        double offsetY = 1.0 - belowShape.max(Direction.Axis.Y);

        BlockPos farthestLeft = te.getBlockPos();
        BlockPos farthestRight = te.getBlockPos();
        Direction facing = currentState.getValue(BlockType3BarrierBase.FACING);
        BlockState working = currentState;
        Block blockInstance = ((BlockType3BarrierBase) working.getBlock()).getBlockInstance();
        while (working.getBlock() == blockInstance && !working.getValue(RTCProperties.ISFURTHESTRIGHT)) {
            farthestRight = farthestRight.relative(facing.getClockWise());
            working = te.getLevel().getBlockState(farthestRight);
            if (!(working.getBlock() instanceof BlockType3BarrierBase b)) {
                break;
            }
            working = b.computeActualState(working, te.getLevel(), farthestRight);
        }

        float renderX = Math.abs(farthestRight.getX() - farthestLeft.getX());
        if (facing == Direction.WEST || facing == Direction.EAST) {
            renderX = Math.abs(farthestRight.getZ() - farthestLeft.getZ());
        }
        float renderZ = 1.126F;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 - offsetY, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees((4 - (facing.get2DDataValue() + 2)) * 90));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(renderX / 2, 0.75, renderZ / 2);

        float textureBottomY = 0.25F;
        float heightFactor = 0.25F;
        if (te.getSignType() == Type3BarrierBlockEntity.SignType.LaneClosed) {
            textureBottomY = 0.5F;
        } else if (te.getSignType() == Type3BarrierBlockEntity.SignType.RoadClosedThruTraffic) {
            textureBottomY = 1F;
            heightFactor = 0.5F;
        }

        drawTexturedQuad(poseStack, buffer, packedLight, ROAD_CLOSED, 0, 0, 0.6875F, 0.5F, textureBottomY,
                textureBottomY - heightFactor);
        drawTexturedQuad(poseStack, buffer, packedLight, GENERIC, 0, 0, 0.6875F, 1, 0, 0.6875F);
        poseStack.popPose();
    }

    private void renderSecondarySign(Type3BarrierBlockEntity te, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, BlockState currentState) {
        var sign = te.getThisSign();
        if (sign == null) {
            return;
        }
        BlockPos below = te.getBlockPos().below();
        VoxelShape belowShape = te.getLevel().getBlockState(below).getShape(te.getLevel(), below);
        double offsetY = 1.0 - belowShape.max(Direction.Axis.Y);
        Direction facing = currentState.getValue(BlockType3BarrierBase.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 - offsetY, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees((4 - (facing.get2DDataValue() + 2)) * 90));
        poseStack.translate(-0.375, 0, 0.063);
        SignTextRenderHelper.renderSignFace(poseStack, buffer, packedLight, sign.getFrontImageResourceLocation(), 0.75F,
                0.75F, false);
        SignTextRenderHelper.renderSignText(poseStack, buffer, packedLight, sign, te::getThisSignTextLine);
        SignTextRenderHelper.renderSignFace(poseStack, buffer, packedLight, sign.getBackImageResourceLocation(), 0.75F,
                0.75F, true);
        poseStack.popPose();
    }

    private static void drawTexturedQuad(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            ResourceLocation texture, float x0, float y0, float y1, float uSplit, float vBottom, float vTop) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(texture));
        Matrix4f matrix = poseStack.last().pose();
        consumer.addVertex(matrix, 1, y0, 0).setColor(255, 255, 255, 255).setUv(uSplit, vBottom)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0, 1, 0);
        consumer.addVertex(matrix, 1, y1, 0).setColor(255, 255, 255, 255).setUv(uSplit, vTop)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, y1, 0).setColor(255, 255, 255, 255).setUv(0, vTop)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, y0, 0).setColor(255, 255, 255, 255).setUv(0, vBottom)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0, 1, 0);
    }
}
