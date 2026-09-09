package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.client.CrossingLampClientModels;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;

/**
 * Shared crossing-lamp housing + cantilever/lamp-head drawing for the world BER, config GUI, and
 * inventory. 1.12 baked the cantilever frames into the block/item model; 1.21 uses an empty pole
 * plus these standalone assemblies.
 */
public final class CrossingLampRender {

    private CrossingLampRender() {
    }

    public static void renderHousing(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            String modelPrefix, boolean gui) {
        BakedModel housing = BerModelHelper.standaloneModel(
                CrossingLampClientModels.modelLocation(housingModel(modelPrefix)));
        renderBaked(poseStack, housing, buffer, packedLight, gui);
    }

    public static void renderAssemblies(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            String modelPrefix, int neRotation, int nwRotation, int seRotation, int swRotation,
            CrossingLampState lampState, boolean gui) {
        renderAssembly(poseStack, buffer, packedLight, modelPrefix, "ne", neRotation, lampState, true, gui);
        renderAssembly(poseStack, buffer, packedLight, modelPrefix, "nw", nwRotation, lampState, false, gui);
        renderAssembly(poseStack, buffer, packedLight, modelPrefix, "se", seRotation, lampState, true, gui);
        renderAssembly(poseStack, buffer, packedLight, modelPrefix, "sw", swRotation, lampState, false, gui);
    }

    public static void renderHousingAndAssemblies(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            String modelPrefix, int neRotation, int nwRotation, int seRotation, int swRotation,
            CrossingLampState lampState, boolean gui) {
        renderHousing(poseStack, buffer, packedLight, modelPrefix, gui);
        renderAssemblies(poseStack, buffer, packedLight, modelPrefix, neRotation, nwRotation, seRotation,
                swRotation, lampState, gui);
    }

    private static void renderAssembly(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            String modelPrefix, String quadrant, int bulbRotation, CrossingLampState lampState,
            boolean litOnFlash1, boolean gui) {
        if (bulbRotation < 0) {
            return;
        }

        LampTransform transform = lampTransform(modelPrefix, quadrant);
        if (transform.hasSupport()) {
            BakedModel support = BerModelHelper.standaloneModel(
                    CrossingLampClientModels.modelLocation(modelPrefix + "_" + quadrant + "_support"));
            renderBaked(poseStack, support, buffer, packedLight, gui);
        }

        poseStack.pushPose();
        poseStack.translate(transform.x(), transform.y(), transform.z());
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(bulbRotation * -22.5F));
        poseStack.translate(-0.5, -0.5, -0.5);
        boolean lit = lampState == (litOnFlash1 ? CrossingLampState.Flash1 : CrossingLampState.Flash2);
        BakedModel model = BerModelHelper.standaloneModel(
                CrossingLampClientModels.modelLocation(
                        modelPrefix + "_" + quadrant + "_lamp" + (lit ? "_lit" : "")));
        int renderLight = lit ? LightTexture.FULL_BRIGHT : packedLight;
        renderBaked(poseStack, model, buffer, renderLight, gui);
        poseStack.popPose();
    }

    private static void renderBaked(PoseStack poseStack, BakedModel model, MultiBufferSource buffer,
            int packedLight, boolean gui) {
        if (gui) {
            BerModelHelper.renderModelGui(poseStack, model, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        } else {
            BerModelHelper.renderModel(poseStack, model, null, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        }
    }

    private static String housingModel(String modelPrefix) {
        return switch (modelPrefix) {
            case "crossing_gate_lamps" -> "crossing_gate_lamps_empty";
            case "ped_crossing_lamps" -> "ped_crossing_light";
            default -> modelPrefix;
        };
    }

    private static LampTransform lampTransform(String modelPrefix, String quadrant) {
        if ("crossing_gate_lamps".equals(modelPrefix)) {
            return switch (quadrant) {
                case "ne" -> new LampTransform(0.28125, 0.9375, -0.5, true);
                case "nw" -> new LampTransform(-0.28125, 0.9375, -0.5, true);
                case "se" -> new LampTransform(0.25625, 0.9375, 0.4, true);
                case "sw" -> new LampTransform(-0.25625, 0.9375, 0.4, true);
                default -> LampTransform.NONE;
            };
        }
        if ("overhead_lamps".equals(modelPrefix)) {
            return switch (quadrant) {
                case "ne" -> new LampTransform(0.28125, 0.6875, -0.0625, true);
                case "nw" -> new LampTransform(-0.28125, 0.6875, -0.0625, true);
                case "se" -> new LampTransform(0.28125, 0.6875, 0.0625, true);
                case "sw" -> new LampTransform(-0.28125, 0.6875, 0.0625, true);
                default -> LampTransform.NONE;
            };
        }
        return LampTransform.NONE;
    }

    private record LampTransform(double x, double y, double z, boolean hasSupport) {
        private static final LampTransform NONE = new LampTransform(0, 0, 0, false);
    }
}
