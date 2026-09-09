package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGateLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockLampBase;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockOverheadLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockRotatableCrossingLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingLampsBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code CrossingLampsRenderer} — housing via block model; BER draws lens submodels only. */
public class CrossingLampsBlockEntityRenderer implements BlockEntityRenderer<CrossingLampsBlockEntity> {

    public CrossingLampsBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrossingLampsBlockEntity te, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState blockState = te.getBlockState();
        if (!(blockState.getBlock() instanceof BlockLampBase lampBlock)) {
            return;
        }

        String modelPrefix = lampBlock.getLampRegistryName();
        CrossingLampState lampState = blockState.hasProperty(RTCProperties.LAMP_STATE)
                ? blockState.getValue(RTCProperties.LAMP_STATE)
                : te.getState();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        if (blockState.getBlock() instanceof BlockCrossingGateLamps
                || blockState.getBlock() instanceof BlockRotatableCrossingLamps) {
            int rotation = blockState.getValue(RTCProperties.ROTATION);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation * -22.5F + 180F));
        } else if (blockState.getBlock() instanceof BlockOverheadLamps) {
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    (4 - blockState.getValue(BlockOverheadLamps.FACING).get2DDataValue()) * 90 + 180));
        }

        poseStack.translate(-0.5, -0.5, -0.5);

        CrossingLampRender.renderAssemblies(poseStack, buffer, packedLight, modelPrefix,
                te.getNeBulbRotation(), te.getNwBulbRotation(), te.getSeBulbRotation(),
                te.getSwBulbRotation(), lampState, false);

        poseStack.popPose();
    }
}
