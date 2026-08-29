package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.client.CrossingLampClientModels;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGateLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockLampBase;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockOverheadLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockRotatableCrossingLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.IHorizontalPoleConnectable;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingLampsBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        CrossingLampState lampState = te.getState();

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

        renderLampAssembly(poseStack, buffer, packedLight, blockState, modelPrefix, "ne",
                te.getNeBulbRotation(), lampState, true);
        renderLampAssembly(poseStack, buffer, packedLight, blockState, modelPrefix, "nw",
                te.getNwBulbRotation(), lampState, false);
        renderLampAssembly(poseStack, buffer, packedLight, blockState, modelPrefix, "se",
                te.getSeBulbRotation(), lampState, true);
        renderLampAssembly(poseStack, buffer, packedLight, blockState, modelPrefix, "sw",
                te.getSwBulbRotation(), lampState, false);
        if (blockState.getBlock() instanceof BlockCrossingGateLamps
                || blockState.getBlock() instanceof BlockRotatableCrossingLamps) {
            renderDownPoleExtension(te, poseStack, buffer, packedLight);
        }

        poseStack.popPose();

        if (blockState.getBlock() instanceof BlockCrossingGateLamps
                || blockState.getBlock() instanceof BlockRotatableCrossingLamps) {
            renderHorizontalPoleExtensions(te, poseStack, buffer, packedLight);
        }
    }

    private void renderHorizontalPoleExtensions(CrossingLampsBlockEntity te, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        if (te.getLevel() == null) {
            return;
        }
        BakedModel model = poleExtensionModel();
        BlockPos pos = te.getBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!connectsTo(te, direction)) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(poleRotation(direction)));
            poseStack.translate(-0.5, -0.5, -0.5);
            BerModelHelper.renderModel(poseStack, model, te.getBlockState(), buffer, packedLight,
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    private void renderDownPoleExtension(CrossingLampsBlockEntity te, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight) {
        if (te.getLevel() == null || !connectsTo(te, Direction.DOWN)) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(270));
        poseStack.translate(-0.5, -0.5, -0.5);
        BerModelHelper.renderModel(poseStack, poleExtensionModel(), te.getBlockState(), buffer, packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private boolean connectsTo(CrossingLampsBlockEntity te, Direction direction) {
        BlockPos neighborPos = te.getBlockPos().relative(direction);
        BlockState neighbor = te.getLevel().getBlockState(neighborPos);
        if (direction == Direction.DOWN
                && neighbor.getBlock().builtInRegistryHolder().key().location().getNamespace()
                        .equals(ModRealisticTrafficControl.MODID)) {
            return true;
        }
        if (neighbor.getBlock() instanceof IHorizontalPoleConnectable connectable) {
            return connectable.canConnectHorizontalPole(neighbor, direction.getOpposite());
        }
        return neighbor.isFaceSturdy(te.getLevel(), neighborPos, direction.getOpposite());
    }

    private BakedModel poleExtensionModel() {
        return BerModelHelper.standaloneModel(CrossingLampClientModels.modelLocation("crossing_gate_pole_ext"));
    }

    private static float poleRotation(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case WEST -> 270;
            case SOUTH -> 180;
            case EAST -> 90;
            default -> 0;
        };
    }

    private void renderLampAssembly(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            BlockState blockState, String modelPrefix, String quadrant, int bulbRotation, CrossingLampState lampState,
            boolean litOnFlash1) {
        if (bulbRotation < 0) {
            return;
        }

        LampTransform transform = lampTransform(modelPrefix, quadrant);
        if (transform.hasSupport()) {
            BakedModel support = BerModelHelper.standaloneModel(
                    CrossingLampClientModels.modelLocation(modelPrefix + "_" + quadrant + "_support"));
            BerModelHelper.renderModel(poseStack, support, blockState, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY);
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
        BerModelHelper.renderModel(poseStack, model, blockState, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
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
