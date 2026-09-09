package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import org.joml.Matrix4f;

import java.util.function.Consumer;

/** Port of 1.12.2 {@code TESRHelper} / {@code RenderBoxHelper} box renderer for modern {@link VertexConsumer} output. */
public final class TesrBoxHelper {

    private static final double EPSILON = 1.0E-6;

    public static class Box {
        private final double x;
        private final double y;
        private final double z;
        private final double width;
        private final double height;
        private final double depth;
        private final TextureInfoCollection textureInfoCollection;
        private final boolean fixedVertexWay;
        private final boolean pixelUnits;

        public Box(double x, double y, double z, double width, double height, double depth,
                TextureInfoCollection textureInfoCollection) {
            this(x, y, z, width, height, depth, textureInfoCollection, false, false);
        }

        public Box(double x, double y, double z, double width, double height, double depth,
                TextureInfoCollection textureInfoCollection, boolean fixedVertexWay) {
            this(x, y, z, width, height, depth, textureInfoCollection, fixedVertexWay, false);
        }

        /**
         * @param pixelUnits when true, box coordinates are Minecraft pixels (÷16), matching 1.12
         *        {@code RenderBoxHelper}. Leave false when the caller already scales the pose (crossing gate).
         */
        public Box(double x, double y, double z, double width, double height, double depth,
                TextureInfoCollection textureInfoCollection, boolean fixedVertexWay, boolean pixelUnits) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.textureInfoCollection = textureInfoCollection;
            this.fixedVertexWay = fixedVertexWay;
            this.pixelUnits = pixelUnits;
        }

        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
            render(poseStack, buffer, packedLight, texture -> {
            });
        }

        /** Binds one {@link RenderType#entitySolid} buffer per distinct face texture (1.21 requirement). */
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                Consumer<ResourceLocation> bindTexture) {
            emitFaces(poseStack, packedLight, bindTexture, (texture, consumerSoFar) ->
                    buffer.getBuffer(RenderType.entitySolid(texture)));
        }

        public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                Consumer<ResourceLocation> bindTexture) {
            emitFaces(poseStack, packedLight, bindTexture, (texture, consumerSoFar) -> consumer);
        }

        private void emitFaces(PoseStack poseStack, int packedLight, Consumer<ResourceLocation> bindTexture,
                BufferLookup bufferLookup) {
            double[][][] faces = getFaces();
            ResourceLocation currentTexture = null;
            VertexConsumer consumer = null;
            Matrix4f matrix = poseStack.last().pose();
            for (int faceIndex = 0; faceIndex < faces.length; faceIndex++) {
                double[][] verts = faces[faceIndex];
                if (verts == null) {
                    continue;
                }
                TextureInfo info = getTextureInfo(faceIndex);
                if (currentTexture == null || !info.texture.equals(currentTexture)) {
                    currentTexture = info.texture;
                    bindTexture.accept(currentTexture);
                    consumer = bufferLookup.get(currentTexture, consumer);
                }
                for (int count = 0; count < 4; count++) {
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
                    double[] vertexPoint = verts[count];
                    consumer.addVertex(matrix, (float) vertexPoint[0], (float) vertexPoint[1], (float) vertexPoint[2])
                            .setColor(255, 255, 255, 255)
                            .setUv((float) uvX, (float) uvY)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(packedLight)
                            .setNormal(poseStack.last(), 0, 1, 0);
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

        /**
         * Six faces in north/up/south/down/east/west order. Degenerate (zero-area) faces are omitted so
         * modern {@code entitySolid} does not explode them into stray quads.
         */
        private double[][][] getFaces() {
            double cx = pixelUnits ? x / 16.0 : x;
            double cy = pixelUnits ? y / 16.0 : y;
            double cz = pixelUnits ? z / 16.0 : z;
            double cw = pixelUnits ? width / 16.0 : width;
            double ch = pixelUnits ? height / 16.0 : height;
            double cd = pixelUnits ? depth / 16.0 : depth;

            double[][][] all = fixedVertexWay
                    ? getFixedFaces(cx, cy, cz, cw, ch, cd)
                    : getDefaultFaces(cx, cy, cz, cw, ch, cd);
            if (!hasArea(cw, ch)) {
                all[0] = null;
                all[2] = null;
            }
            if (!hasArea(cw, cd)) {
                all[1] = null;
                all[3] = null;
            }
            if (!hasArea(ch, cd)) {
                all[4] = null;
                all[5] = null;
            }
            return all;
        }

        private static boolean hasArea(double a, double b) {
            return Math.abs(a) > EPSILON && Math.abs(b) > EPSILON;
        }

        private static double[][][] getDefaultFaces(double x, double y, double z, double width, double height,
                double depth) {
            return new double[][][] {
                    {
                            {x + width, y, z},
                            {x + width, y + height, z},
                            {x, y + height, z},
                            {x, y, z}
                    },
                    {
                            {x + width, y + height, z},
                            {x + width, y + height, z + depth},
                            {x, y + height, z + depth},
                            {x, y + height, z}
                    },
                    {
                            {x, y, z + depth},
                            {x, y + height, z + depth},
                            {x + width, y + height, z + depth},
                            {x + width, y, z + depth}
                    },
                    {
                            {x + width, y, z + depth},
                            {x + width, y, z},
                            {x, y, z},
                            {x, y, z + depth}
                    },
                    {
                            {x + width, y, z + depth},
                            {x + width, y + height, z + depth},
                            {x + width, y + height, z},
                            {x + width, y, z}
                    },
                    {
                            {x, y, z},
                            {x, y + height, z},
                            {x, y + height, z + depth},
                            {x, y, z + depth}
                    }
            };
        }

        private static double[][][] getFixedFaces(double x, double y, double z, double width, double height,
                double depth) {
            return new double[][][] {
                    {
                            {x + width, y, z},
                            {x, y, z},
                            {x, y + height, z},
                            {x + width, y + height, z}
                    },
                    {
                            {x + width, y + height, z},
                            {x, y + height, z},
                            {x, y + height, z + depth},
                            {x + width, y + height, z + depth}
                    },
                    {
                            {x, y, z + depth},
                            {x + width, y, z + depth},
                            {x + width, y + height, z + depth},
                            {x, y + height, z + depth}
                    },
                    {
                            {x + width, y, z + depth},
                            {x, y, z + depth},
                            {x, y, z},
                            {x + width, y, z}
                    },
                    {
                            {x + width, y, z + depth},
                            {x + width, y, z},
                            {x + width, y + height, z},
                            {x + width, y + height, z + depth}
                    },
                    {
                            {x, y, z},
                            {x, y, z + depth},
                            {x, y + height, z + depth},
                            {x, y + height, z}
                    }
            };
        }
    }

    @FunctionalInterface
    private interface BufferLookup {
        VertexConsumer get(ResourceLocation texture, VertexConsumer previous);
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
