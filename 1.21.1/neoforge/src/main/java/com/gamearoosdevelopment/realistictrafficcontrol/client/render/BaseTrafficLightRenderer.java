package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;
import com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCRotation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared bulb-quad rendering for every traffic-light frame layout. Ported from the 1.12.2
 * {@code BaseTrafficLightRenderer} TESR; subclasses only supply bulb positions.
 *
 * <p>Bulb corners are pole-mounted with the same {@link RTCRotation#rotatePoleMountedXZ}
 * path as {@code RotatedBlockModelWrapper}, so the lights stay in the housings.
 */
public abstract class BaseTrafficLightRenderer {

    private static final ResourceLocation BLACK =
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "textures/block/black.png");

    public void render(TrafficLightBlockEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockState state) {
        if (!(state.getBlock() instanceof BlockBaseTrafficLight)) {
            return;
        }

        int rotation = state.getValue(RTCProperties.ROTATION);
        int mount = PoleAssembly.mountCardinal(state);
        double bulbZ = getBulbZLocation();

        List<BulbRenderer> bulbRenderers = getBulbRenderers();
        int overlay = OverlayTexture.NO_OVERLAY;
        int fullBright = LightTexture.FULL_BRIGHT;

        for (BulbRenderer renderer : bulbRenderers) {
            renderer.renderBlack(entity, poseStack, bufferSource, fullBright, overlay, rotation, mount, bulbZ);
        }

        ResourceLocation lastTexture = BLACK;
        for (BulbRenderer renderer : bulbRenderers) {
            lastTexture = renderer.render(entity, poseStack, bufferSource, fullBright, overlay, lastTexture,
                    rotation, mount, bulbZ);
        }
    }

    protected abstract double getBulbZLocation();

    protected abstract List<BulbRenderer> getBulbRenderers();

    public static class BulbRenderer {
        private final double x;
        private final double y;
        private final int bulbSlot;

        public BulbRenderer(double x, double y, int bulbSlot) {
            this.x = x;
            this.y = y;
            this.bulbSlot = bulbSlot;
        }

        public void renderBlack(TrafficLightBlockEntity entity, PoseStack poseStack, MultiBufferSource bufferSource,
                int packedLight, int packedOverlay, int rotation, int mount, double bulbZ) {
            if ((entity.getActiveBySlot(bulbSlot) && entity.getFlashBySlot(bulbSlot) && entity.getFlashCurrentBySlot(bulbSlot))
                    || (entity.getActiveBySlot(bulbSlot) && !entity.getFlashBySlot(bulbSlot))) {
                return;
            }
            render(entity, poseStack, bufferSource, packedLight, packedOverlay, BLACK, true, rotation, mount, bulbZ);
        }

        public ResourceLocation render(TrafficLightBlockEntity entity, PoseStack poseStack, MultiBufferSource bufferSource,
                int packedLight, int packedOverlay, ResourceLocation lastTexture, int rotation, int mount, double bulbZ) {
            return render(entity, poseStack, bufferSource, packedLight, packedOverlay, lastTexture, false,
                    rotation, mount, bulbZ);
        }

        private ResourceLocation render(TrafficLightBlockEntity entity, PoseStack poseStack, MultiBufferSource bufferSource,
                int packedLight, int packedOverlay, ResourceLocation lastTexture, boolean renderBlack,
                int rotation, int mount, double bulbZ) {
            if (!renderBlack && (!entity.getActiveBySlot(bulbSlot)
                    || (entity.getFlashBySlot(bulbSlot) && !entity.getFlashCurrentBySlot(bulbSlot)))) {
                return lastTexture;
            }

            ResourceLocation texture = renderBlack ? BLACK : textureForBulb(entity.getBulbTypeBySlot(bulbSlot));
            if (!renderBlack && texture.equals(BLACK)) {
                return lastTexture;
            }
            drawQuad(poseStack, bufferSource, texture, packedLight, packedOverlay, x, y, bulbZ, rotation, mount);
            return texture;
        }
    }

    private static void drawQuad(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
            int packedLight, int packedOverlay, double bulbX, double bulbY, double bulbZ, int rotation, int mount) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();

        float[] normal = { 0f, 1f };
        RTCRotation.rotatePoleMountedDirectionXZ(normal, rotation, mount);

        emit(consumer, pose, bulbX + 5.6, bulbY, bulbZ + 2, 1f, 1f, packedLight, packedOverlay, rotation, mount, normal);
        emit(consumer, pose, bulbX + 5.6, bulbY + 5.5, bulbZ + 2, 1f, 0f, packedLight, packedOverlay, rotation, mount, normal);
        emit(consumer, pose, bulbX, bulbY + 5.5, bulbZ + 2, 0f, 0f, packedLight, packedOverlay, rotation, mount, normal);
        emit(consumer, pose, bulbX, bulbY, bulbZ + 2, 0f, 1f, packedLight, packedOverlay, rotation, mount, normal);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose,
            double px, double py, double pz, float u, float v, int packedLight, int packedOverlay,
            int rotation, int mount, float[] normal) {
        float[] xz = { (float) (px / 16.0), (float) (pz / 16.0) };
        RTCRotation.rotatePoleMountedXZ(xz, rotation, mount);
        consumer.addVertex(pose.pose(), xz[0], (float) (py / 16.0), xz[1])
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, normal[0], 0f, normal[1]);
    }

    private static ResourceLocation textureForBulb(EnumTrafficLightBulbTypes bulbType) {
        if (bulbType == null) {
            return BLACK;
        }
        String path = switch (bulbType) {
            case Green -> "textures/block/green.png";
            case GreenDownArrow -> "textures/block/green_down.png";
            case StraightGreen -> "textures/block/straight_green.png";
            case GreenArrowLeft, GreenArrowLeft2 -> "textures/block/green_arrow_left.png";
            case Red, Red2 -> "textures/block/red_solid.png";
            case RedX -> "textures/block/x_dithered.png";
            case YellowX -> "textures/block/yellow_x.png";
            case StraightRed -> "textures/block/straight_red.png";
            case RedArrowLeft, RedArrowLeft2 -> "textures/block/red_arrow_left.png";
            case Yellow -> "textures/block/yellow_solid.png";
            case StraightYellow -> "textures/block/straight_yellow.png";
            case YellowArrowLeft, YellowArrowLeft2, YellowArrowLeft3 -> "textures/block/yellow_arrow_left.png";
            case Cross -> "textures/block/cross.png";
            case DontCross -> "textures/block/dontcross.png";
            case GreenArrowRight, GreenArrowRight2 -> "textures/block/green_arrow_right.png";
            case RedArrowRight, RedArrowRight2 -> "textures/block/red_arrow_right.png";
            case NoRightTurn -> "textures/block/no_right_turn.png";
            case NoLeftTurn -> "textures/block/no_left_turn.png";
            case YellowArrowRight, YellowArrowRight2, YellowArrowRight3 -> "textures/block/yellow_arrow_right.png";
            case GreenArrowUTurn, GreenArrowUTurn2 -> "textures/block/green_arrow_uturn.png";
            case YellowArrowUTurn, YellowArrowUTurn2, YellowArrowUTurn3 -> "textures/block/yellow_arrow_uturn.png";
            case RedArrowUTurn, RedArrowUTurn2 -> "textures/block/red_arrow_uturn.png";
            default -> "textures/block/black.png";
        };
        return ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, path);
    }
}
