package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGateLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockLampBase;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockOverheadLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockRotatableCrossingLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingLampsBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code CrossingLampsRenderer}. */
public class CrossingLampsBlockEntityRenderer implements BlockEntityRenderer<CrossingLampsBlockEntity> {

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "block/" + path);
    }

    public CrossingLampsBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrossingLampsBlockEntity te, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState blockState = te.getBlockState();
        if (!(blockState.getBlock() instanceof BlockLampBase)) {
            return;
        }

        String modelPrefix = ((BlockLampBase) blockState.getBlock()).getLampRegistryName();
        String stateName = te.getState().getSerializedName();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        boolean renderPoleExtensions = false;
        boolean north = false;
        boolean west = false;
        boolean south = false;
        boolean east = false;
        boolean down = false;

        if (blockState.getBlock() instanceof BlockCrossingGateLamps
                || blockState.getBlock() instanceof BlockRotatableCrossingLamps) {
            int rotation = blockState.getValue(RTCProperties.ROTATION);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation * -22.5F + 180F));
            renderPoleExtensions = true;
            north = blockState.getValue(RTCProperties.NORTH);
            west = blockState.getValue(RTCProperties.WEST);
            south = blockState.getValue(RTCProperties.SOUTH);
            east = blockState.getValue(RTCProperties.EAST);
            down = blockState.getValue(RTCProperties.DOWN);
        } else if (blockState.getBlock() instanceof BlockOverheadLamps) {
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    (4 - blockState.getValue(BlockOverheadLamps.FACING).get2DDataValue()) * 90 + 180));
        }

        poseStack.translate(-0.5, -0.5, -0.5);

        renderLampIfPresent(poseStack, buffer, packedLight, modelPrefix + "_ne_lamp", te.getNeBulbRotation(),
                stateName);
        renderLampIfPresent(poseStack, buffer, packedLight, modelPrefix + "_nw_lamp", te.getNwBulbRotation(),
                stateName);
        renderLampIfPresent(poseStack, buffer, packedLight, modelPrefix + "_se_lamp", te.getSeBulbRotation(),
                stateName);
        renderLampIfPresent(poseStack, buffer, packedLight, modelPrefix + "_sw_lamp", te.getSwBulbRotation(),
                stateName);

        if (renderPoleExtensions && down) {
            renderPoleExtension(poseStack, buffer, packedLight, Direction.DOWN);
        }

        poseStack.popPose();

        if (renderPoleExtensions) {
            poseStack.pushPose();
            if (north) {
                renderPoleExtension(poseStack, buffer, packedLight, Direction.NORTH);
            }
            if (west) {
                renderPoleExtension(poseStack, buffer, packedLight, Direction.WEST);
            }
            if (south) {
                renderPoleExtension(poseStack, buffer, packedLight, Direction.SOUTH);
            }
            if (east) {
                renderPoleExtension(poseStack, buffer, packedLight, Direction.EAST);
            }
            poseStack.popPose();
        }
    }

    private void renderLampIfPresent(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            String modelName, int bulbRotation, String stateName) {
        if (bulbRotation < 0) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(bulbRotation * -22.5F));
        poseStack.translate(-0.5, -0.5, -0.5);
        BakedModel model = BerModelHelper.standaloneModel(model(modelName));
        BerModelHelper.renderModel(poseStack, model, null, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private void renderPoleExtension(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            Direction direction) {
        BakedModel model = BerModelHelper.standaloneModel(model("crossing_gate_pole_ext"));
        poseStack.pushPose();
        switch (direction) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(0));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            default -> {
            }
        }
        BerModelHelper.renderModel(poseStack, model, null, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
