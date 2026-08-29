package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignHorizontalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignVerticalAlignment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.function.IntFunction;

/** Shared sign texture + text rendering for sign BERs. */
public final class SignTextRenderHelper {

    private SignTextRenderHelper() {
    }

    public static void renderSignFace(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            ResourceLocation texture, float width, float height, boolean backFace) {
        poseStack.pushPose();
        if (backFace) {
            poseStack.translate(0, 0, -0.01);
        }
        var consumer = buffer.getBuffer(RenderType.entityCutout(texture));
        Matrix4f matrix = poseStack.last().pose();
        if (backFace) {
            putVertex(consumer, matrix, packedLight, 1, 1, 0, 0, 0);
            putVertex(consumer, matrix, packedLight, 1, 0, 0, 0, 1);
            putVertex(consumer, matrix, packedLight, 0, 0, 0, 1, 1);
            putVertex(consumer, matrix, packedLight, 0, height, 0, 1, 0);
        } else {
            putVertex(consumer, matrix, packedLight, 0, height, 0, 0, 0);
            putVertex(consumer, matrix, packedLight, 0, 0, 0, 0, 1);
            putVertex(consumer, matrix, packedLight, width, 0, 0, 1, 1);
            putVertex(consumer, matrix, packedLight, width, height, 0, 1, 0);
        }
        poseStack.popPose();
    }

    private static void putVertex(com.mojang.blaze3d.vertex.VertexConsumer consumer, Matrix4f matrix, int packedLight,
            float x, float y, float z, float u, float v) {
        consumer.addVertex(matrix, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setLight(packedLight)
                .setNormal(0, 1, 0);
    }

    public static void renderSignText(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Sign sign,
            IntFunction<String> textProvider) {
        if (sign.getTextLines().isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.scale(1F / font.lineHeight, -1F / font.lineHeight, 1);
        poseStack.translate(0, -9, 0.01);
        poseStack.scale(1 / 16F, 1 / 16F, 1);

        for (int i = 0; i < sign.getTextLines().size(); i++) {
            Sign.TextLine textLine = sign.getTextLines().get(i);
            poseStack.pushPose();
            poseStack.translate(textLine.getX() * font.lineHeight, textLine.getY() * font.lineHeight, 0);
            poseStack.scale((float) textLine.getXScale(), (float) textLine.getYScale(), 1);
            if (textLine.getvAlign() == SignVerticalAlignment.Center) {
                poseStack.translate(0, -font.lineHeight / 2.0, 0);
            } else if (textLine.getvAlign() == SignVerticalAlignment.Bottom) {
                poseStack.translate(0, -font.lineHeight, 0);
            }
            if (textLine.gethAlign() == SignHorizontalAlignment.Center) {
                poseStack.translate(-(float) (textLine.getScaleAdjustedWidth() * font.lineHeight) / 2.0, 0, 0);
            } else if (textLine.gethAlign() == SignHorizontalAlignment.Right) {
                poseStack.translate(-(float) (textLine.getScaleAdjustedWidth() * font.lineHeight), 0, 0);
            }

            String text = textProvider.apply(i);
            if (text != null) {
                int textWidth = font.width(text);
                if (textWidth > 0) {
                    double widthScaling = (textLine.getScaleAdjustedWidth() * font.lineHeight) / textWidth;
                    if (widthScaling > 1) {
                        widthScaling = 1;
                    }
                    poseStack.scale((float) widthScaling, 1, 1);
                    int textX = 0;
                    if (textLine.gethAlign() == SignHorizontalAlignment.Center && widthScaling == 1) {
                        textX = (int) ((textLine.getScaleAdjustedWidth() * font.lineHeight) / 2) - (textWidth / 2);
                    } else if (textLine.gethAlign() == SignHorizontalAlignment.Right) {
                        textX = (int) (textLine.getScaleAdjustedWidth() * font.lineHeight) - textWidth;
                    }
                    font.drawInBatch(text, textX + 1, 1, textLine.getColor(), false, poseStack.last().pose(),
                            buffer, Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
                }
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void applySignRotation(PoseStack poseStack, int rotation) {
        float degrees = rotation * -22.5F;
        if (degrees == -90) {
            poseStack.translate(1.44, 0.4, 0.41);
        } else if (degrees == 0) {
            poseStack.translate(0.4, 0.4, -0.44);
        } else if (degrees == -180) {
            poseStack.translate(0.6, 0.4, 1.44);
        } else {
            poseStack.translate(-0.44, 0.4, 0.59);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        poseStack.translate(-0.4, -0.4, 0.06875);
    }
}
