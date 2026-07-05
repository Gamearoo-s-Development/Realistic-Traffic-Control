package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code SignRenderer}. */
public class SignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {

    public SignBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SignBlockEntity te, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        var sign = te.getSign();
        if (sign == null || te.getLevel() == null) {
            return;
        }
        BlockState block = te.getLevel().getBlockState(te.getBlockPos());
        if (!(block.getBlock() instanceof BlockSign)) {
            return;
        }
        int rotation = block.getValue(RTCProperties.ROTATION);

        poseStack.pushPose();
        poseStack.translate(0, 0, 0);
        SignTextRenderHelper.applySignRotation(poseStack, rotation);
        SignTextRenderHelper.renderSignFace(poseStack, buffer, packedLight, sign.getFrontImageResourceLocation(), 1, 1,
                false);
        SignTextRenderHelper.renderSignText(poseStack, buffer, packedLight, sign, te::getTextLine);
        SignTextRenderHelper.renderSignFace(poseStack, buffer, packedLight, sign.getBackImageResourceLocation(), 1, 1,
                true);
        poseStack.popPose();
    }
}
