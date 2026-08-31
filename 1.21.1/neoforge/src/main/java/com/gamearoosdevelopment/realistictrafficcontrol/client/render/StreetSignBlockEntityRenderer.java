package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSignBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Port of 1.12.2 {@code StreetSignRenderer}. */
public class StreetSignBlockEntityRenderer implements BlockEntityRenderer<StreetSignBlockEntity> {

    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(
            ModRealisticTrafficControl.MODID, "textures/block/street_sign.png");

    public StreetSignBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StreetSignBlockEntity te, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        for (int i = 0; i < StreetSignBlockEntity.MAX_STREET_SIGNS; i++) {
            StreetSign sign = te.getStreetSign(i);
            if (sign != null) {
                renderStreetSign(poseStack, buffer, packedLight, sign, i);
            }
        }
        poseStack.popPose();
    }

    private void renderStreetSign(PoseStack poseStack, MultiBufferSource buffer, int packedLight, StreetSign sign,
            int signIndex) {
        poseStack.pushPose();
        poseStack.translate(8, 8, 8);
        poseStack.mulPose(Axis.YP.rotationDegrees(sign.getRotation() * -22.5F));
        poseStack.translate(-8, -8, -8);

        double xOffset = sign.getColor().getCol() - 1;
        double yOffset = 0.25 * (sign.getColor().getRow() - 1);
        int yRenderOffset = 4 * signIndex;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(ATLAS));
        Matrix4f matrix = poseStack.last().pose();
        drawAtlasQuad(consumer, matrix, packedLight, 16, yRenderOffset + 4, 8, xOffset + 1, yOffset,
                16, yRenderOffset, 8, xOffset + 1, yOffset + 0.25,
                0, yRenderOffset, 8, xOffset, yOffset + 0.25,
                0, yRenderOffset + 4, 8, xOffset, yOffset);
        drawAtlasQuad(consumer, matrix, packedLight, 0, yRenderOffset + 4, 8, xOffset + 1, yOffset,
                0, yRenderOffset, 8, xOffset + 1, yOffset + 0.25,
                16, yRenderOffset, 8, xOffset, yOffset + 0.25,
                16, yRenderOffset + 4, 8, xOffset, yOffset);

        Font font = Minecraft.getInstance().font;
        poseStack.scale(-1, -1, 1);
        poseStack.translate(-8, -2.8 - yRenderOffset, 7.99);
        int width = font.width(sign.getText());
        double scaleFactor = 15.0 / width;
        if (scaleFactor > 0.25) {
            scaleFactor = 0.25;
        }
        poseStack.scale((float) scaleFactor, 0.25F, 1);
        font.drawInBatch(sign.getText(), -(width / 2F), 0, sign.getTextColor(), false, poseStack.last().pose(), buffer,
                Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        poseStack.popPose();
    }

    private static void drawAtlasQuad(VertexConsumer consumer, Matrix4f matrix, int packedLight,
            float x0, float y0, float z0, double u0, double v0,
            float x1, float y1, float z1, double u1, double v1,
            float x2, float y2, float z2, double u2, double v2,
            float x3, float y3, float z3, double u3, double v3) {
        consumer.addVertex(matrix, x0, y0, z0).setColor(255, 255, 255, 255).setUv((float) u0, (float) v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(255, 255, 255, 255).setUv((float) u1, (float) v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(255, 255, 255, 255).setUv((float) u2, (float) v2)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(255, 255, 255, 255).setUv((float) u3, (float) v3)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 1, 0);
    }
}
