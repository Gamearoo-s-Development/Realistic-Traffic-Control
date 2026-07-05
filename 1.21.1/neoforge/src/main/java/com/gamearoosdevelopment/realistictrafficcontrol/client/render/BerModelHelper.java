package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Shared baked-quad rendering helpers for block-entity renderers. */
public final class BerModelHelper {

    public static BakedModel standaloneModel(ResourceLocation modelLocation) {
        return Minecraft.getInstance().getModelManager()
                .getModel(ModelResourceLocation.standalone(modelLocation));
    }

    public static void renderModel(PoseStack poseStack, BakedModel model, BlockState state,
            net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight, int packedOverlay) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        for (Direction direction : Direction.values()) {
            renderQuads(poseStack, consumer, model.getQuads(state, direction, RandomSource.create()), packedLight,
                    packedOverlay);
        }
        renderQuads(poseStack, consumer, model.getQuads(state, null, RandomSource.create()), packedLight, packedOverlay);
    }

    private static void renderQuads(PoseStack poseStack, VertexConsumer consumer, List<BakedQuad> quads,
            int packedLight, int packedOverlay) {
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
        }
    }

    private BerModelHelper() {
    }
}
