package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.TesrBoxHelper.Box;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.TesrBoxHelper.TextureInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.TesrBoxHelper.TextureInfoCollection;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingGateGateBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingGateGateBlockEntity.GateLightCount;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.ModelResourceLocation;

/** Port of 1.12.2 {@code RendererCrossingGateGate}. */
public class CrossingGateGateBlockEntityRenderer implements BlockEntityRenderer<CrossingGateGateBlockEntity> {

    private static final ResourceLocation GENERIC =
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "textures/block/generic.png");
    private static final ResourceLocation GATE =
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "textures/block/gate.png");
    private static final ResourceLocation LIGHT_MODEL =
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "block/crossing_gate_light");

    public CrossingGateGateBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrossingGateGateBlockEntity te, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(te.getFacingRotation()));
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        poseStack.translate(3, 2, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(te.getGateRotation()));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entitySolid(GENERIC));
        renderWeightVertices(poseStack, consumer, packedLight);
        renderGateVertices(poseStack, consumer, packedLight, te.getCrossingGateLength());

        if (te.getGateLightCount() == GateLightCount.OneLight
                || te.getCrossingGateLength() - te.getLightStartOffset() >= 2) {
            renderGateLights(poseStack, buffer, packedLight, packedOverlay, te);
        }

        poseStack.popPose();
    }

    private void renderWeightVertices(PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        TextureInfoCollection collection = new TextureInfoCollection(
                new TextureInfo(GENERIC, 0, 0, 1, 1),
                new TextureInfo(GENERIC, 0, 0, 8, 1),
                new TextureInfo(GENERIC, 0, 0, 1, 1),
                new TextureInfo(GENERIC, 0, 0, 8, 1),
                new TextureInfo(GENERIC, 0, 0, 8, 1),
                new TextureInfo(GENERIC, 0, 0, 8, 1));
        new Box(-7.5, -9.5, 4, 1, 2, -8, collection).render(poseStack, consumer, packedLight, rl -> {
        });

        collection = new TextureInfoCollection(
                new TextureInfo(GENERIC, 3, 4, 8, 5),
                new TextureInfo(GENERIC, 7, 4, 8, 5),
                new TextureInfo(GENERIC, 3, 3, 8, 4),
                new TextureInfo(GENERIC, 4, 4, 5, 5),
                new TextureInfo(GENERIC, 4, 4, 5, 9),
                new TextureInfo(GENERIC, 4, 4, 5, 9));
        new Box(-6.5, -9.5, 4, 7, 2, -1, collection).render(poseStack, consumer, packedLight, rl -> {
        });
        new Box(-6.5, -9.5, -3, 7, 2, -1, collection).render(poseStack, consumer, packedLight, rl -> {
        });

        collection = new TextureInfoCollection(
                new TextureInfo(GENERIC, 5, 9, 12, 12),
                new TextureInfo(GENERIC, 1, 7, 8, 8),
                new TextureInfo(GENERIC, 6, 10, 13, 13),
                new TextureInfo(GENERIC, 2, 7, 9, 8),
                new TextureInfo(GENERIC, 5, 6, 6, 9),
                new TextureInfo(GENERIC, 2, 3, 3, 6));
        new Box(-2.5, -7.5, 4, 3, 8.5, -1, collection).render(poseStack, consumer, packedLight, rl -> {
        });
        new Box(-2.5, -7.5, -3, 3, 8.5, -1, collection).render(poseStack, consumer, packedLight, rl -> {
        });
        new Box(0.5, -2, 4, 3, 3, -1, collection).render(poseStack, consumer, packedLight, rl -> {
        });
        new Box(0.5, -2, -3, 3, 3, -1, collection).render(poseStack, consumer, packedLight, rl -> {
        });
        new Box(3.5, -3.5, 4, 10, 6, -1, collection).render(poseStack, consumer, packedLight, rl -> {
        });
        new Box(3.5, -3.5, -3, 10, 6, -1, collection).render(poseStack, consumer, packedLight, rl -> {
        });
    }

    private void renderGateVertices(PoseStack poseStack, VertexConsumer consumer, int packedLight,
            float crossingGateLength) {
        TextureInfoCollection collection = new TextureInfoCollection(
                new TextureInfo(GATE, 0, 0, 16, 0.7),
                new TextureInfo(GATE, 0, 2, 16, 2.7),
                new TextureInfo(GATE, 0, 0, 15, 0.7),
                new TextureInfo(GATE, 0, 1, 16, 1.7),
                new TextureInfo(GATE, 0, 2, 3, 4),
                new TextureInfo(GATE, 0, 2, 3, 4));
        new Box(-(crossingGateLength * 16) - 13, -9.5, 0.5, (crossingGateLength * 16) + 5.5, 2, -1, collection)
                .render(poseStack, consumer, packedLight, rl -> {
                });
    }

    private void renderGateLights(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay,
            CrossingGateGateBlockEntity te) {
        BakedModel modelOff = BerModelHelper.standaloneModel(LIGHT_MODEL);
        BakedModel modelOn = BerModelHelper.standaloneModel(LIGHT_MODEL);
        float crossingGateLength = te.getCrossingGateLength();
        CrossingLampState flashState = te.getFlashState();
        float lightStartOffset = te.getLightStartOffset();
        GateLightCount gateLightCount = te.getGateLightCount();

        poseStack.pushPose();
        poseStack.translate(-20.5 - lightStartOffset * 16, -7.5, -8);

        if (gateLightCount == GateLightCount.ThreeLights) {
            poseStack.pushPose();
            poseStack.scale(16, 16, 16);
            BerModelHelper.renderModel(poseStack,
                    flashState == CrossingLampState.Flash2 ? modelOn : modelOff, null, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        poseStack.translate(-(crossingGateLength - lightStartOffset) * 16 / 2 + 1, 0, 0);
        if (gateLightCount == GateLightCount.ThreeLights) {
            poseStack.pushPose();
            poseStack.scale(16, 16, 16);
            BerModelHelper.renderModel(poseStack,
                    flashState == CrossingLampState.Flash1 ? modelOn : modelOff, null, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        poseStack.translate(-(crossingGateLength - lightStartOffset) * 16 / 2 + 1, 0, 0);
        poseStack.pushPose();
        poseStack.scale(16, 16, 16);
        BerModelHelper.renderModel(poseStack,
                flashState == CrossingLampState.Off ? modelOff : modelOn, null, buffer, packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        poseStack.popPose();
    }
}
