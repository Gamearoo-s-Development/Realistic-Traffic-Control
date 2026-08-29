package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import org.joml.Matrix4f;

import java.util.function.Consumer;

/** Port of 1.12.2 {@code TESRHelper} box renderer for modern {@link VertexConsumer} output. */
public final class TesrBoxHelper {

    public static class Box {
        private final double x;
        private final double y;
        private final double z;
        private final double width;
        private final double height;
        private final double depth;
        private final TextureInfoCollection textureInfoCollection;

        public Box(double x, double y, double z, double width, double height, double depth,
                TextureInfoCollection textureInfoCollection) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.textureInfoCollection = textureInfoCollection;
        }

        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
            render(poseStack, buffer, packedLight, texture -> {
            });
        }

        /** Binds one {@link RenderType#entitySolid} buffer per distinct face texture (1.21 requirement). */
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                Consumer<ResourceLocation> bindTexture) {
            double[][] vertexPoints = getVertexPoints();
            int index = 0;
            int count = 0;
            ResourceLocation currentTexture = null;
            VertexConsumer consumer = null;
            Matrix4f matrix = poseStack.last().pose();
            for (double[] vertexPoint : vertexPoints) {
                TextureInfo info = getTextureInfo(index);
                if (currentTexture == null || !info.texture.equals(currentTexture)) {
                    currentTexture = info.texture;
                    bindTexture.accept(currentTexture);
                    consumer = buffer.getBuffer(RenderType.entitySolid(currentTexture));
                }

                double uvX;
                double uvY;
                switch (count) {
                    case 0 -> {
                        uvX = info.getConvertedEndX();
                        uvY = info.getConvertedEndY();
                    }
                    case 1 -> {
                        uvX = info.getConvertedEndX();
                        uvY = info.getConvertedStartY();
                    }
                    case 2 -> {
                        uvX = info.getConvertedStartX();
                        uvY = info.getConvertedStartY();
                    }
                    default -> {
                        uvX = info.getConvertedStartX();
                        uvY = info.getConvertedEndY();
                    }
                }

                consumer.addVertex(matrix, (float) vertexPoint[0], (float) vertexPoint[1], (float) vertexPoint[2])
                        .setColor(255, 255, 255, 255)
                        .setUv((float) uvX, (float) uvY)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(poseStack.last(), 0, 1, 0);
                count++;
                if (count >= 4) {
                    index++;
                    count = 0;
                }
            }
        }

        public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                Consumer<ResourceLocation> bindTexture) {
            double[][] vertexPoints = getVertexPoints();
            int index = 0;
            int count = 0;
            ResourceLocation lastResourceLocation = null;
            Matrix4f matrix = poseStack.last().pose();
            for (double[] vertexPoint : vertexPoints) {
                TextureInfo info = getTextureInfo(index);
                if (info.texture != lastResourceLocation) {
                    lastResourceLocation = info.texture;
                    bindTexture.accept(lastResourceLocation);
                }

                double uvX;
                double uvY;
                switch (count) {
                    case 0 -> {
                        uvX = info.getConvertedEndX();
                        uvY = info.getConvertedEndY();
                    }
                    case 1 -> {
                        uvX = info.getConvertedEndX();
                        uvY = info.getConvertedStartY();
                    }
                    case 2 -> {
                        uvX = info.getConvertedStartX();
                        uvY = info.getConvertedStartY();
                    }
                    default -> {
                        uvX = info.getConvertedStartX();
                        uvY = info.getConvertedEndY();
                    }
                }

                consumer.addVertex(matrix, (float) vertexPoint[0], (float) vertexPoint[1], (float) vertexPoint[2])
                        .setColor(255, 255, 255, 255)
                        .setUv((float) uvX, (float) uvY)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(poseStack.last(), 0, 1, 0);
                count++;
                if (count >= 4) {
                    index++;
                    count = 0;
                }
            }
        }

        private TextureInfo getTextureInfo(int count) {
            return switch (count) {
                case 0 -> textureInfoCollection.northFace;
                case 1 -> textureInfoCollection.upFace;
                case 2 -> textureInfoCollection.southFace;
                case 3 -> textureInfoCollection.downFace;
                case 4 -> textureInfoCollection.eastFace;
                case 5 -> textureInfoCollection.westFace;
                default -> textureInfoCollection.northFace;
            };
        }

        private double[][] getVertexPoints() {
            return new double[][] {
                    {x + width, y, z},
                    {x + width, y + height, z},
                    {x, y + height, z},
                    {x, y, z},
                    {x + width, y + height, z},
                    {x + width, y + height, z + depth},
                    {x, y + height, z + depth},
                    {x, y + height, z},
                    {x, y, z + depth},
                    {x, y + height, z + depth},
                    {x + width, y + height, z + depth},
                    {x + width, y, z + depth},
                    {x + width, y, z + depth},
                    {x + width, y, z},
                    {x, y, z},
                    {x, y, z + depth},
                    {x + width, y, z + depth},
                    {x + width, y + height, z + depth},
                    {x + width, y + height, z},
                    {x + width, y, z},
                    {x, y, z},
                    {x, y + height, z},
                    {x, y + height, z + depth},
                    {x, y, z + depth}
            };
        }
    }

    public record TextureInfoCollection(TextureInfo southFace, TextureInfo upFace, TextureInfo northFace,
            TextureInfo downFace, TextureInfo eastFace, TextureInfo westFace) {
    }

    public record TextureInfo(ResourceLocation texture, double startX, double startY, double endX, double endY) {
        public double getConvertedStartX() {
            return startX / 16;
        }

        public double getConvertedStartY() {
            return startY / 16;
        }

        public double getConvertedEndX() {
            return endX / 16;
        }

        public double getConvertedEndY() {
            return endY / 16;
        }
    }

    private TesrBoxHelper() {
    }
}
