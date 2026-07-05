package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.WigWagBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code RendererWigWag}. */
public class WigWagBlockEntityRenderer implements BlockEntityRenderer<WigWagBlockEntity> {

    private static final ResourceLocation MOUNT_MODEL =
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "block/wig_wag_arm_mount");
    private static final ResourceLocation ARM_MODEL =
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "block/wig_wag_arm");

    public WigWagBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WigWagBlockEntity te, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        BlockState state = te.getBlockState();
        if (state.getBlock() != ModBlocks.WIG_WAG.get()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.545, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.getValue(RTCProperties.ROTATION) * -22.5F));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(0.03F, 0.06F, 0.21F);
        poseStack.translate(blockToWorld(-3.5), blockToWorld(16.5), blockToWorld(7.5));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));

        float pivotY = (float) blockToWorld(0.45);
        poseStack.translate(0, pivotY, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(te.getRotation()));
        poseStack.translate(0, -pivotY, 0);

        BakedModel mountModel = BerModelHelper.standaloneModel(MOUNT_MODEL);
        BerModelHelper.renderModel(poseStack, mountModel, state, buffer, packedLight, OverlayTexture.NO_OVERLAY);

        BakedModel armModel = BerModelHelper.standaloneModel(ARM_MODEL);
        BerModelHelper.renderModel(poseStack, armModel, state, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static double blockToWorld(double blockCoord) {
        return blockCoord / 16;
    }
}
