package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockDigitalSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** Port of 1.12.2 {@code SignRenderer}. */
public class SignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
    private static final float DIGITAL_FACE_Z = (float) (-.103 + 13D / 16D);
    private static final float DIGITAL_BEZEL_Z = (float) (-.13 + 13D / 16D);

    public SignBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SignBlockEntity te, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        var sign = te.getSign();
        if (sign == null || te.getLevel() == null) {
            return;
        }
        BlockState block = te.getLevel().getBlockState(te.getBlockPos());
        if (!(block.getBlock() instanceof BlockSign) && !(block.getBlock() instanceof BlockDigitalSign)) {
            return;
        }
        int rotation = block.getValue(RTCProperties.ROTATION);

        poseStack.pushPose();
        if (block.getBlock() instanceof BlockDigitalSign) {
            poseStack.translate(.5, 0, .5);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation * -22.5F));
            boolean left = BlockDigitalSign.hasNeighbor(te.getLevel(), te.getBlockPos(), block, -1, 0);
            boolean right = BlockDigitalSign.hasNeighbor(te.getLevel(), te.getBlockPos(), block, 1, 0);
            boolean down = BlockDigitalSign.hasNeighbor(te.getLevel(), te.getBlockPos(), block, 0, -1);
            boolean up = BlockDigitalSign.hasNeighbor(te.getLevel(), te.getBlockPos(), block, 0, 1);
            renderDigitalBezel(poseStack, buffer, left, right, down, up);

            float faceLeft = left ? 0 : .0625F;
            float faceRight = right ? 1 : .9375F;
            float faceBottom = down ? 0 : .0625F;
            float faceTop = up ? 1 : .9375F;
            poseStack.translate(-.5F + faceLeft, faceBottom, DIGITAL_FACE_Z);
            poseStack.scale(faceRight - faceLeft, faceTop - faceBottom, 1);
            SignTextRenderHelper.renderDigitalFace(poseStack, buffer, LightTexture.FULL_BRIGHT,
                    sign.getFrontImageResourceLocation(), 1, 1);
            SignTextRenderHelper.renderDigitalSignText(
                    poseStack, buffer, LightTexture.FULL_BRIGHT, sign, te::getTextLine);
        } else {
            SignTextRenderHelper.applySignRotation(poseStack, rotation);
            SignTextRenderHelper.renderSignFace(poseStack, buffer, packedLight,
                    sign.getFrontImageResourceLocation(), 1, 1, false);
            SignTextRenderHelper.renderSignText(poseStack, buffer, packedLight, sign, te::getTextLine);
            SignTextRenderHelper.renderSignFace(poseStack, buffer, packedLight,
                    sign.getBackImageResourceLocation(), 1, 1, true);
        }
        poseStack.popPose();
    }

    private static void renderDigitalBezel(PoseStack pose, MultiBufferSource buffers, boolean left,
            boolean right, boolean down, boolean up) {
        // Each bezel side is an independent quad. A triangle-strip render type
        // connects them and produces the large crossed triangles seen in game.
        VertexConsumer out = buffers.getBuffer(RenderType.debugQuads());
        if (!left) face(out, pose.last().pose(), -.5F, 0, -.4375F, 1);
        if (!right) face(out, pose.last().pose(), .4375F, 0, .5F, 1);
        if (!down) face(out, pose.last().pose(), -.4375F, 0, .4375F, .0625F);
        if (!up) face(out, pose.last().pose(), -.4375F, .9375F, .4375F, 1);
    }

    private static void face(VertexConsumer out, Matrix4f matrix, float x1, float y1, float x2, float y2) {
        vertex(out, matrix, x1, y1);
        vertex(out, matrix, x1, y2);
        vertex(out, matrix, x2, y2);
        vertex(out, matrix, x2, y1);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, float x, float y) {
        out.addVertex(matrix, x, y, DIGITAL_BEZEL_Z).setColor(82, 82, 82, 255);
    }
}
