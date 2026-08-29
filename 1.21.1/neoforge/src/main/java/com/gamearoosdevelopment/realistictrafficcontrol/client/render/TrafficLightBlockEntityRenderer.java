package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.Map;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single BER for the shared {@link TrafficLightBlockEntity} type. Dispatches to the frame-specific layout
 * classes ported from the 1.12.2 TESRs.
 */
public class TrafficLightBlockEntityRenderer implements BlockEntityRenderer<TrafficLightBlockEntity> {

    private static final Map<Block, BaseTrafficLightRenderer> LAYOUTS = Map.ofEntries(
            Map.entry(ModBlocks.TRAFFIC_LIGHT.get(), new TrafficLightRenderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_HOZ.get(), new TrafficLightHozRenderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_1.get(), new TrafficLight1Renderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_2.get(), new TrafficLight2Renderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_2_HOZ.get(), new TrafficLight2HozRenderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_4.get(), new TrafficLight4Renderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_4_HOZ.get(), new TrafficLight4HozRenderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_5.get(), new TrafficLight5Renderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_5_HOZ.get(), new TrafficLight5HozRenderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_DOGHOUSE.get(), new TrafficLightDoghouseRenderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_6.get(), new TrafficLight6Renderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_7.get(), new TrafficLight7Renderer()),
            Map.entry(ModBlocks.TRAFFIC_LIGHT_8.get(), new TrafficLight8Renderer()));

    public TrafficLightBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TrafficLightBlockEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        BaseTrafficLightRenderer layout = LAYOUTS.get(state.getBlock());
        if (layout != null) {
            layout.render(entity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, state);
        }
    }
}
