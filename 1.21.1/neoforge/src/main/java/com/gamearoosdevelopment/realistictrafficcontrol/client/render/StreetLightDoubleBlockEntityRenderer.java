package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.TesrBoxHelper.Box;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.TesrBoxHelper.TextureInfo;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.TesrBoxHelper.TextureInfoCollection;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetLightDoubleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Port of 1.12.2 {@code StreetLightDoubleRenderer}. */
public class StreetLightDoubleBlockEntityRenderer implements BlockEntityRenderer<StreetLightDoubleBlockEntity> {

    private static final ResourceLocation GENERIC = ResourceLocation.fromNamespaceAndPath(
            ModRealisticTrafficControl.MODID, "textures/block/generic.png");
    private static final ResourceLocation YELLOW = ResourceLocation.fromNamespaceAndPath(
            ModRealisticTrafficControl.MODID, "textures/block/yellow.png");

    private static final TextureInfoCollection POST_THICK = boxCollection(GENERIC, 4, 16, 4);
    private static final TextureInfoCollection POST_THIN = boxCollection(GENERIC, 2, 16, 2);
    private static final TextureInfoCollection ARM = armCollection(GENERIC);
    private static final TextureInfoCollection LAMP = boxCollection(YELLOW, 2, 13, 1);

    public StreetLightDoubleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StreetLightDoubleBlockEntity te, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (te.getLevel() == null) {
            return;
        }
        BlockState state = te.getLevel().getBlockState(te.getBlockPos());
        int rotation = state.getValue(RTCProperties.ROTATION);

        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(6, 0, 6, 4, 16, 4, POST_THICK));
        boxes.add(new Box(6, 16, 6, 4, 16, 4, POST_THICK));
        boxes.add(new Box(7, 32, 7, 2, 16, 2, POST_THIN));
        boxes.add(new Box(7, 48, 7, 2, 16, 2, POST_THIN));
        addArm(boxes, 23.2, 25.2, 38.2);
        addArm(boxes, -23.2, -23.2, -10.2);
        boxes.add(new Box(7, 64.83, 26.2, 2, 0.5, 12, LAMP));
        boxes.add(new Box(7, 64.83, -22.2, 2, 0.5, 12, LAMP));

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation * -22.5F));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.scale(1 / 16F, 1 / 16F, 1 / 16F);
        for (Box box : boxes) {
            box.render(poseStack, buffer.getBuffer(RenderType.entitySolid(GENERIC)), packedLight, rl -> {
            });
        }
        poseStack.pushPose();
        poseStack.translate(0.4375 * 16, 3.75 * 16, 0.5625 * 16);
        poseStack.mulPose(Axis.XP.rotationDegrees(-20));
        new Box(0, 0, 0, 2, 2, 16, ARM).render(poseStack,
                buffer.getBuffer(RenderType.entitySolid(GENERIC)), packedLight, rl -> {
                });
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.4375 * 16, 3.75 * 16, -0.5625 * 16);
        poseStack.mulPose(Axis.XP.rotationDegrees(20));
        new Box(0, 0, -16, 2, 2, 16, ARM).render(poseStack,
                buffer.getBuffer(RenderType.entitySolid(GENERIC)), packedLight, rl -> {
                });
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void addArm(List<Box> boxes, double zCenter, double zStart, double zEnd) {
        boxes.add(new Box(7, 65.35, zCenter, 2, 2, 16, ARM));
        boxes.add(new Box(5, 64.35, zStart, 1, 1, 14, ARM));
        boxes.add(new Box(10, 64.35, zStart, 1, 1, 14, ARM));
        boxes.add(new Box(6, 64.35, zStart, 4, 1, 1, ARM));
        boxes.add(new Box(6, 64.35, zEnd, 4, 1, 1, ARM));
        boxes.add(new Box(6, 65.34, zStart, 4, 0, 14, ARM));
    }

    private static TextureInfoCollection boxCollection(ResourceLocation tex, double side, double height, double end) {
        TextureInfo sideInfo = new TextureInfo(tex, 0, 0, side, height);
        TextureInfo endInfo = new TextureInfo(tex, 0, 0, end, end);
        return new TextureInfoCollection(sideInfo, endInfo, sideInfo, endInfo, sideInfo, sideInfo);
    }

    private static TextureInfoCollection armCollection(ResourceLocation tex) {
        TextureInfo end = new TextureInfo(tex, 0, 0, 2, 2);
        TextureInfo side = new TextureInfo(tex, 0, 0, 16, 2);
        return new TextureInfoCollection(end, side, end, side, side, side);
    }
}
