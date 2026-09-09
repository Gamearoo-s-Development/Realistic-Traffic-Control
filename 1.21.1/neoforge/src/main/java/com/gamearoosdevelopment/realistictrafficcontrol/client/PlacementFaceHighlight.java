package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlights the targeted model face when holding a mount item. Does not rewrite
 * {@code mc.hitResult} — break and place use vanilla pick, like Create shafts.
 */
@EventBusSubscriber(modid = ModRealisticTrafficControl.MODID, value = Dist.CLIENT)
public final class PlacementFaceHighlight {

    private static final float FILL_R = 1.0F;
    private static final float FILL_G = 0.78F;
    private static final float FILL_B = 0.12F;
    private static final float FILL_A = 0.42F;
    private static final float LINE_A = 0.95F;
    private static final double OUTSET = 0.004;
    private static final RandomSource RANDOM = RandomSource.create(42L);

    private PlacementFaceHighlight() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || mc.options.hideGui) {
            return;
        }

        BlockHitResult hit = event.getTarget();
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!shouldHighlight(player, state)) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);

        MultiBufferSource buffers = event.getMultiBufferSource();
        PoseStack.Pose pose = poseStack.last();
        Vec3 localHit = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        Vec3[] modelFace = findModelFace(mc, level, pos, state, hit.getDirection(), localHit);
        if (modelFace != null) {
            renderModelFace(buffers, pose, modelFace);
        } else {
            renderShapeFace(state.getShape(level, pos, CollisionContext.of(player)), localHit, hit.getDirection(),
                    buffers, pose);
        }

        poseStack.popPose();
    }

    private static boolean shouldHighlight(Player player, BlockState state) {
        if (!PoleAssembly.isPoleMountTarget(state)) {
            return false;
        }
        return PoleAssembly.isPoleMountItem(player.getMainHandItem())
                || PoleAssembly.isPoleMountItem(player.getOffhandItem());
    }

    private static Vec3[] findModelFace(Minecraft mc, Level level, BlockPos pos, BlockState state,
            Direction hitFace, Vec3 localHit) {
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        ModelData data = model.getModelData(level, pos, state, level.getModelData(pos));
        List<BakedQuad> quads = new ArrayList<>();
        quads.addAll(model.getQuads(state, null, RANDOM, data, null));
        for (Direction side : Direction.values()) {
            quads.addAll(model.getQuads(state, side, RANDOM, data, null));
        }
        if (quads.isEmpty()) {
            return null;
        }

        Vec3 faceNormal = Vec3.atLowerCornerOf(hitFace.getNormal());
        Vec3[] best = null;
        double bestScore = Double.MAX_VALUE;
        for (BakedQuad quad : quads) {
            Vec3[] verts = quadVertices(quad);
            Vec3 normal = quadNormal(verts);
            if (normal.lengthSqr() < 1.0E-8) {
                continue;
            }
            normal = normal.normalize();
            if (normal.dot(faceNormal) < 0.45) {
                continue;
            }
            double planeDist = Math.abs(localHit.subtract(verts[0]).dot(normal));
            Vec3 centroid = verts[0].add(verts[1]).add(verts[2]).add(verts[3]).scale(0.25);
            Vec3 planar = localHit.subtract(centroid);
            planar = planar.subtract(normal.scale(planar.dot(normal)));
            double score = planeDist * planeDist * 8.0 + planar.lengthSqr();
            if (score < bestScore) {
                bestScore = score;
                best = verts;
            }
        }
        return best;
    }

    private static Vec3[] quadVertices(BakedQuad quad) {
        int[] data = quad.getVertices();
        Vec3[] verts = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
            verts[i] = new Vec3(
                    Float.intBitsToFloat(data[offset]),
                    Float.intBitsToFloat(data[offset + 1]),
                    Float.intBitsToFloat(data[offset + 2]));
        }
        return verts;
    }

    private static Vec3 quadNormal(Vec3[] verts) {
        return verts[1].subtract(verts[0]).cross(verts[2].subtract(verts[0]));
    }

    private static void renderModelFace(MultiBufferSource buffers, PoseStack.Pose pose, Vec3[] verts) {
        Vec3 normal = quadNormal(verts);
        if (normal.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec3 outset = normal.normalize().scale(OUTSET);
        Vec3[] lifted = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            lifted[i] = verts[i].add(outset);
        }
        VertexConsumer fill = buffers.getBuffer(RenderType.debugQuads());
        addVertex(fill, pose, lifted[0], FILL_R, FILL_G, FILL_B, FILL_A);
        addVertex(fill, pose, lifted[1], FILL_R, FILL_G, FILL_B, FILL_A);
        addVertex(fill, pose, lifted[2], FILL_R, FILL_G, FILL_B, FILL_A);
        addVertex(fill, pose, lifted[3], FILL_R, FILL_G, FILL_B, FILL_A);

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        addLine(lines, pose, lifted[0], lifted[1], FILL_R, FILL_G, FILL_B, LINE_A);
        addLine(lines, pose, lifted[1], lifted[2], FILL_R, FILL_G, FILL_B, LINE_A);
        addLine(lines, pose, lifted[2], lifted[3], FILL_R, FILL_G, FILL_B, LINE_A);
        addLine(lines, pose, lifted[3], lifted[0], FILL_R, FILL_G, FILL_B, LINE_A);
    }

    private static void renderShapeFace(VoxelShape shape, Vec3 localHit, Direction face,
            MultiBufferSource buffers, PoseStack.Pose pose) {
        if (shape.isEmpty()) {
            return;
        }
        AABB box = aabbForHit(shape, localHit);
        renderFaceQuad(buffers.getBuffer(RenderType.debugQuads()), pose, box, face, FILL_R, FILL_G, FILL_B, FILL_A);
        renderFaceOutline(buffers.getBuffer(RenderType.lines()), pose, box, face, FILL_R, FILL_G, FILL_B, LINE_A);
    }

    private static AABB aabbForHit(VoxelShape shape, Vec3 localHit) {
        AABB closest = null;
        double best = Double.MAX_VALUE;
        for (AABB aabb : shape.toAabbs()) {
            if (aabb.inflate(0.02).contains(localHit)) {
                return aabb;
            }
            double cx = Mth.clamp(localHit.x, aabb.minX, aabb.maxX);
            double cy = Mth.clamp(localHit.y, aabb.minY, aabb.maxY);
            double cz = Mth.clamp(localHit.z, aabb.minZ, aabb.maxZ);
            double dist = localHit.distanceToSqr(cx, cy, cz);
            if (dist < best) {
                best = dist;
                closest = aabb;
            }
        }
        return closest != null ? closest : shape.bounds();
    }

    private static void renderFaceQuad(VertexConsumer consumer, PoseStack.Pose pose, AABB box, Direction face,
            float r, float g, float b, float a) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;
        switch (face) {
            case DOWN -> addQuad(consumer, pose, x1, y1 - OUTSET, z1, x2, y1 - OUTSET, z1, x2, y1 - OUTSET, z2, x1, y1 - OUTSET, z2, r, g, b, a);
            case UP -> addQuad(consumer, pose, x1, y2 + OUTSET, z1, x1, y2 + OUTSET, z2, x2, y2 + OUTSET, z2, x2, y2 + OUTSET, z1, r, g, b, a);
            case NORTH -> addQuad(consumer, pose, x1, y1, z1 - OUTSET, x1, y2, z1 - OUTSET, x2, y2, z1 - OUTSET, x2, y1, z1 - OUTSET, r, g, b, a);
            case SOUTH -> addQuad(consumer, pose, x1, y1, z2 + OUTSET, x2, y1, z2 + OUTSET, x2, y2, z2 + OUTSET, x1, y2, z2 + OUTSET, r, g, b, a);
            case WEST -> addQuad(consumer, pose, x1 - OUTSET, y1, z1, x1 - OUTSET, y1, z2, x1 - OUTSET, y2, z2, x1 - OUTSET, y2, z1, r, g, b, a);
            case EAST -> addQuad(consumer, pose, x2 + OUTSET, y1, z1, x2 + OUTSET, y2, z1, x2 + OUTSET, y2, z2, x2 + OUTSET, y1, z2, r, g, b, a);
        }
    }

    private static void renderFaceOutline(VertexConsumer consumer, PoseStack.Pose pose, AABB box, Direction face,
            float r, float g, float b, float a) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;
        switch (face) {
            case DOWN -> {
                addLine(consumer, pose, x1, y1 - OUTSET, z1, x2, y1 - OUTSET, z1, r, g, b, a);
                addLine(consumer, pose, x2, y1 - OUTSET, z1, x2, y1 - OUTSET, z2, r, g, b, a);
                addLine(consumer, pose, x2, y1 - OUTSET, z2, x1, y1 - OUTSET, z2, r, g, b, a);
                addLine(consumer, pose, x1, y1 - OUTSET, z2, x1, y1 - OUTSET, z1, r, g, b, a);
            }
            case UP -> {
                addLine(consumer, pose, x1, y2 + OUTSET, z1, x1, y2 + OUTSET, z2, r, g, b, a);
                addLine(consumer, pose, x1, y2 + OUTSET, z2, x2, y2 + OUTSET, z2, r, g, b, a);
                addLine(consumer, pose, x2, y2 + OUTSET, z2, x2, y2 + OUTSET, z1, r, g, b, a);
                addLine(consumer, pose, x2, y2 + OUTSET, z1, x1, y2 + OUTSET, z1, r, g, b, a);
            }
            case NORTH -> {
                addLine(consumer, pose, x1, y1, z1 - OUTSET, x1, y2, z1 - OUTSET, r, g, b, a);
                addLine(consumer, pose, x1, y2, z1 - OUTSET, x2, y2, z1 - OUTSET, r, g, b, a);
                addLine(consumer, pose, x2, y2, z1 - OUTSET, x2, y1, z1 - OUTSET, r, g, b, a);
                addLine(consumer, pose, x2, y1, z1 - OUTSET, x1, y1, z1 - OUTSET, r, g, b, a);
            }
            case SOUTH -> {
                addLine(consumer, pose, x1, y1, z2 + OUTSET, x2, y1, z2 + OUTSET, r, g, b, a);
                addLine(consumer, pose, x2, y1, z2 + OUTSET, x2, y2, z2 + OUTSET, r, g, b, a);
                addLine(consumer, pose, x2, y2, z2 + OUTSET, x1, y2, z2 + OUTSET, r, g, b, a);
                addLine(consumer, pose, x1, y2, z2 + OUTSET, x1, y1, z2 + OUTSET, r, g, b, a);
            }
            case WEST -> {
                addLine(consumer, pose, x1 - OUTSET, y1, z1, x1 - OUTSET, y1, z2, r, g, b, a);
                addLine(consumer, pose, x1 - OUTSET, y1, z2, x1 - OUTSET, y2, z2, r, g, b, a);
                addLine(consumer, pose, x1 - OUTSET, y2, z2, x1 - OUTSET, y2, z1, r, g, b, a);
                addLine(consumer, pose, x1 - OUTSET, y2, z1, x1 - OUTSET, y1, z1, r, g, b, a);
            }
            case EAST -> {
                addLine(consumer, pose, x2 + OUTSET, y1, z1, x2 + OUTSET, y2, z1, r, g, b, a);
                addLine(consumer, pose, x2 + OUTSET, y2, z1, x2 + OUTSET, y2, z2, r, g, b, a);
                addLine(consumer, pose, x2 + OUTSET, y2, z2, x2 + OUTSET, y1, z2, r, g, b, a);
                addLine(consumer, pose, x2 + OUTSET, y1, z2, x2 + OUTSET, y1, z1, r, g, b, a);
            }
        }
    }

    private static void addQuad(VertexConsumer consumer, PoseStack.Pose pose,
            double x1, double y1, double z1, double x2, double y2, double z2,
            double x3, double y3, double z3, double x4, double y4, double z4,
            float r, float g, float b, float a) {
        addVertex(consumer, pose, new Vec3(x1, y1, z1), r, g, b, a);
        addVertex(consumer, pose, new Vec3(x2, y2, z2), r, g, b, a);
        addVertex(consumer, pose, new Vec3(x3, y3, z3), r, g, b, a);
        addVertex(consumer, pose, new Vec3(x4, y4, z4), r, g, b, a);
    }

    private static void addLine(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b,
            float r, float g, float bCol, float aCol) {
        addLine(consumer, pose, a.x, a.y, a.z, b.x, b.y, b.z, r, g, bCol, aCol);
    }

    private static void addLine(VertexConsumer consumer, PoseStack.Pose pose,
            double x1, double y1, double z1, double x2, double y2, double z2,
            float r, float g, float b, float a) {
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1.0E-5F) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }

    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 v,
            float r, float g, float b, float a) {
        consumer.addVertex(pose, (float) v.x, (float) v.y, (float) v.z).setColor(r, g, b, a);
    }
}
