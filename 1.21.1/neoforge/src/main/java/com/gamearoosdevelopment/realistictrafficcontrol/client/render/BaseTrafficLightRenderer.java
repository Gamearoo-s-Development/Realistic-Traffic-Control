package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCRotation;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared bulb-quad rendering for every traffic-light frame layout. Ported from the 1.12.2
 * {@code BaseTrafficLightRenderer} TESR; subclasses only supply bulb positions.
 */
public abstract class BaseTrafficLightRenderer {

    private static final ResourceLocation BLACK =
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "textures/block/black.png");

    public void render(TrafficLightBlockEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockState state) {
        if (!(state.getBlock() instanceof BlockBaseTrafficLight)) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(1f / 16f, 1f / 16f, 1f / 16f);
        poseStack.translate(8, 8, 8);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                RTCRotation.placementRotationDegrees(
                        state.getValue(com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties.ROTATION))));
        poseStack.translate(-8, -8, -8);
        poseStack.translate(0, 0, getBulbZLocation());

        List<BulbRenderer> bulbRenderers = getBulbRenderers();
        int overlay = OverlayTexture.NO_OVERLAY;
        int fullBright = LightTexture.FULL_BRIGHT;

        for (BulbRenderer renderer : bulbRenderers) {
            renderer.renderBlack(entity, poseStack, bufferSource, fullBright, overlay);
        }

        ResourceLocation lastTexture = BLACK;
        for (BulbRenderer renderer : bulbRenderers) {
            lastTexture = renderer.render(entity, poseStack, bufferSource, fullBright, overlay, lastTexture);
        }

        poseStack.popPose();
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
                int packedLight, int packedOverlay) {
            if ((entity.getActiveBySlot(bulbSlot) && entity.getFlashBySlot(bulbSlot) && entity.getFlashCurrentBySlot(bulbSlot))
                    || (entity.getActiveBySlot(bulbSlot) && !entity.getFlashBySlot(bulbSlot))) {
                return;
            }
            render(entity, poseStack, bufferSource, packedLight, packedOverlay, BLACK, true);
        }

        public ResourceLocation render(TrafficLightBlockEntity entity, PoseStack poseStack, MultiBufferSource bufferSource,
                int packedLight, int packedOverlay, ResourceLocation lastTexture) {
            return render(entity, poseStack, bufferSource, packedLight, packedOverlay, lastTexture, false);
        }

        private ResourceLocation render(TrafficLightBlockEntity entity, PoseStack poseStack, MultiBufferSource bufferSource,
                int packedLight, int packedOverlay, ResourceLocation lastTexture, boolean renderBlack) {
            if (!renderBlack && (!entity.getActiveBySlot(bulbSlot)
                    || (entity.getFlashBySlot(bulbSlot) && !entity.getFlashCurrentBySlot(bulbSlot)))) {
                return lastTexture;
            }

            poseStack.pushPose();
            poseStack.translate(x, y, 0);

            ResourceLocation texture = renderBlack ? BLACK : textureForBulb(entity.getBulbTypeBySlot(bulbSlot));
            if (!renderBlack && texture.equals(BLACK)) {
                // No bulb configured in this slot — skip colored pass (black backing still drawn).
                poseStack.popPose();
                return lastTexture;
            }
            drawQuad(poseStack, bufferSource, texture, packedLight, packedOverlay);

            poseStack.popPose();
            return texture;
        }
    }

    private static void drawQuad(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
            int packedLight, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();

        addVertex(consumer, pose, 5.6f, 0f, 2f, 1f, 1f, packedLight, packedOverlay);
        addVertex(consumer, pose, 5.6f, 5.5f, 2f, 1f, 0f, packedLight, packedOverlay);
        addVertex(consumer, pose, 0f, 5.5f, 2f, 0f, 0f, packedLight, packedOverlay);
        addVertex(consumer, pose, 0f, 0f, 2f, 0f, 1f, packedLight, packedOverlay);
    }

    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose,
            float x, float y, float z, float u, float v, int packedLight, int packedOverlay) {
        consumer.addVertex(pose.pose(), x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, 0f, 0f, 1f);
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
