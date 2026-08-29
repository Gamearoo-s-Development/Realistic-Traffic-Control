package com.gamearoosdevelopment.realistictrafficcontrol.client.model;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCRotation;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.QuadTransformers;

import com.mojang.math.Transformation;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Applies 16-step Y rotation from {@link RTCProperties#ROTATION} at render time. Replaces invalid
 * 1.12.2 blockstate {@code "y": 337} style rotations that 1.21.1 rejects.
 */
public class RotatedBlockModelWrapper extends BakedModelWrapper<BakedModel> {

    // Baked quad vertices are in block-space units (0..1), not model JSON units (0..16).
    private static final Vector3f BLOCK_CENTER = new Vector3f(0.5f, 0.5f, 0.5f);

    public RotatedBlockModelWrapper(BakedModel original) {
        super(original);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return rotate(super.getQuads(state, side, random), state);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
            ModelData modelData, RenderType renderType) {
        return rotate(super.getQuads(state, side, random, modelData, renderType), state);
    }

    private static List<BakedQuad> rotate(List<BakedQuad> quads, BlockState state) {
        if (state == null || !state.hasProperty(RTCProperties.ROTATION)) {
            return quads;
        }
        int rotation = state.getValue(RTCProperties.ROTATION);
        if (rotation == 0) {
            return quads;
        }
        float degrees = RTCRotation.placementRotationDegrees(rotation);
        float radians = (float) Math.toRadians(degrees);
        Matrix4f matrix = new Matrix4f()
                .translate(BLOCK_CENTER)
                .rotateY(radians)
                .translate(-BLOCK_CENTER.x, -BLOCK_CENTER.y, -BLOCK_CENTER.z);
        Transformation transform = new Transformation(matrix);
        return QuadTransformers.applying(transform).process(quads);
    }
}
