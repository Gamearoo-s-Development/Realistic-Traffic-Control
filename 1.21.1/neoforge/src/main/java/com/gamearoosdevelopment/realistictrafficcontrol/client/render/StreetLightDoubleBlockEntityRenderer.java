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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Port of 1.12.2 {@code StreetLightDoubleRenderer}. Boxes stay in pixel units; no pose scale. */
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
        if (!(state.getBlock() instanceof com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetLightDouble)
                || !state.hasProperty(RTCProperties.ROTATION)) {
            return;
        }
        int rotation = state.getValue(RTCProperties.ROTATION);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation * -22.5F));
        poseStack.translate(-0.5, -0.5, -0.5);

        List<Box> boxes = new ArrayList<>();
        boxes.add(px(6, 0, 6, 4, 16, 4, POST_THICK));
        boxes.add(px(6, 16, 6, 4, 16, 4, POST_THICK));
        boxes.add(px(7, 32, 7, 2, 16, 2, POST_THIN));
        boxes.add(px(7, 48, 7, 2, 16, 2, POST_THIN));
        boxes.add(px(7, 65.35, 23.2, 2, 2, 16, ARM));
        boxes.add(px(5, 64.35, 25.2, 1, 1, 14, ARM));
        boxes.add(px(10, 64.35, 25.2, 1, 1, 14, ARM));
        boxes.add(px(6, 64.35, 25.2, 4, 1, 1, ARM));
        boxes.add(px(6, 64.35, 38.2, 4, 1, 1, ARM));
        boxes.add(px(6, 65.34, 25.2, 4, 0, 14, ARM));
        boxes.add(px(7, 65.35, -23.2, 2, 2, 16, ARM));
        boxes.add(px(5, 64.35, -23.2, 1, 1, 14, ARM));
        boxes.add(px(10, 64.35, -23.2, 1, 1, 14, ARM));
        boxes.add(px(6, 64.35, -23.2, 4, 1, 1, ARM));
        boxes.add(px(6, 64.35, -10.2, 4, 1, 1, ARM));
        boxes.add(px(6, 65.34, -23.2, 4, 0, 14, ARM));
        boxes.add(px(7, 64.83, 26.2, 2, 0.5, 12, LAMP));
        boxes.add(px(7, 64.83, -22.2, 2, 0.5, 12, LAMP));
        for (Box box : boxes) {
            box.render(poseStack, buffer, packedLight);
        }

        poseStack.translate(0.4375, 3.75, 0.5625);
        poseStack.mulPose(Axis.XP.rotationDegrees(-20));
        px(0, 0, 0, 2, 2, 16, ARM).render(poseStack, buffer, packedLight);
        poseStack.mulPose(Axis.XP.rotationDegrees(20));
        poseStack.translate(0, 0.34375, -1.0625);
        poseStack.mulPose(Axis.XP.rotationDegrees(20));
        px(0, 0, 0, 2, 2, 16, ARM).render(poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(StreetLightDoubleBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(StreetLightDoubleBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(4, 0, 4).expandTowards(0, 5, 0);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    private static Box px(double x, double y, double z, double width, double height, double depth,
            TextureInfoCollection textures) {
        return new Box(x, y, z, width, height, depth, textures, true, true);
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
