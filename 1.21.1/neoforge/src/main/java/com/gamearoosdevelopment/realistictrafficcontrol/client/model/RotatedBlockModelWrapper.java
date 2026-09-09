package com.gamearoosdevelopment.realistictrafficcontrol.client.model;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCRotation;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.IQuadTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies 16-step Y rotation from {@link RTCProperties#ROTATION}. Poles spin around
 * the block center. Frames and signs use pole-mount so off-90° steps stay flush.
 */
public class RotatedBlockModelWrapper extends BakedModelWrapper<BakedModel> {

    private final boolean poleMounted;

    public RotatedBlockModelWrapper(BakedModel original) {
        this(original, false);
    }

    public RotatedBlockModelWrapper(BakedModel original, boolean poleMounted) {
        super(original);
        this.poleMounted = poleMounted;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return rotatedQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
            ModelData modelData, RenderType renderType) {
        return rotatedQuads(state, side, random, modelData, renderType);
    }

    private List<BakedQuad> rotatedQuads(BlockState state, Direction side, RandomSource random,
            ModelData modelData, RenderType renderType) {
        if (state == null || !state.hasProperty(RTCProperties.ROTATION)) {
            return super.getQuads(state, side, random, modelData, renderType);
        }
        int rotation = state.getValue(RTCProperties.ROTATION);
        int mount = PoleAssembly.mountCardinal(state);
        if (rotation == 0 && mount == 0) {
            return super.getQuads(state, side, random, modelData, renderType);
        }
        // After a Y spin, culled faces no longer match world sides. Emit the
        // full model once on the general list so the bar does not shed halves.
        if (side != null) {
            return List.of();
        }
        List<BakedQuad> quads = new ArrayList<>(super.getQuads(state, null, random, modelData, renderType));
        for (Direction direction : Direction.values()) {
            quads.addAll(super.getQuads(state, direction, random, modelData, renderType));
        }
        return rotateQuads(quads, rotation, mount, poleMounted);
    }

    private static List<BakedQuad> rotateQuads(List<BakedQuad> quads, int rotation, int mount, boolean poleMounted) {
        float[] xz = new float[2];
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices().clone();
            for (int i = 0; i < 4; i++) {
                int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
                xz[0] = Float.intBitsToFloat(vertices[offset]);
                xz[1] = Float.intBitsToFloat(vertices[offset + 2]);
                if (poleMounted) {
                    RTCRotation.rotatePoleMountedXZ(xz, rotation, mount);
                } else {
                    RTCRotation.rotateAround(xz, 0.5F, 0.5F,
                            (float) Math.toRadians(RTCRotation.placementRotationDegrees(rotation)));
                }
                vertices[offset] = Float.floatToRawIntBits(xz[0]);
                vertices[offset + 2] = Float.floatToRawIntBits(xz[1]);
            }
            out.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(),
                    quad.isShade(), quad.hasAmbientOcclusion()));
        }
        return out;
    }
}
