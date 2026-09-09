package com.gamearoosdevelopment.realistictrafficcontrol.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Voxel helpers. 1.12.2 {@code AxisAlignedBB} constructors tolerated min &gt; max on an axis; 1.21
 * {@link Block#box} and {@link Shapes#box} do not.
 *
 * <p>Y-rotation matches {@code RotatedBlockModelWrapper}: poles spin around the block center, frames
 * and bars orbit the clicked pole. Boxes are sliced before rotating so diagonal models keep a thin
 * hitbox instead of one fat AABB. Pick uses the full rotated model; collision is clipped to the cell.
 */
public final class RTCShapes {

    /** Two pixels in block space — small enough to follow 22.5° bars, cheap to union. */
    private static final double SLICE = 2.0 / 16.0;
    private static final ConcurrentMap<RotKey, VoxelShape> CACHE = new ConcurrentHashMap<>();

    private RTCShapes() {
    }

    /** Block-space coords (0–16), matching {@link Block#box}. */
    public static VoxelShape blockBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    /** Normalized coords (0–1), matching {@link Shapes#box}. */
    public static VoxelShape unitBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Shapes.box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    /**
     * Rotates a collision shape around Y the same way {@code RotatedBlockModelWrapper} spins
     * the baked model, so angled poles and bars keep a matching hitbox.
     */
    public static VoxelShape rotateY(VoxelShape shape, int rotation) {
        return rotateCached(shape, rotation, false, 0);
    }

    /** Same as {@link #rotateY} but frames/signs orbit the pole after the nearest 90°. */
    public static VoxelShape rotateYPoleMounted(VoxelShape shape, int rotation) {
        return rotateYPoleMounted(shape, rotation, CustomAngleCalculator.nearestCardinal(rotation));
    }

    public static VoxelShape rotateYPoleMounted(VoxelShape shape, int rotation, int mountCardinal) {
        return rotateCached(shape, rotation, true, mountCardinal, 0, 0);
    }

    private static VoxelShape rotateCached(VoxelShape shape, int rotation, boolean poleMounted, int mountCardinal) {
        return rotateCached(shape, rotation, poleMounted, mountCardinal, 0, 0);
    }

    private static VoxelShape rotateCached(VoxelShape shape, int rotation, boolean poleMounted, int mountCardinal,
            int originDx, int originDz) {
        if (shape.isEmpty()) {
            return shape;
        }
        int rot = rotation & 15;
        int mount = mountCardinal & 15;
        if (!poleMounted && rot == 0 && originDx == 0 && originDz == 0) {
            return shape;
        }
        if (poleMounted && rot == 0 && mount == 0 && originDx == 0 && originDz == 0) {
            return shape;
        }
        RotKey key = new RotKey(identity(shape), rot, mount, poleMounted, originDx, originDz);
        return CACHE.computeIfAbsent(key, ignored ->
                rotateUncached(shape, rot, poleMounted, mount, originDx, originDz));
    }

    private static VoxelShape rotateUncached(VoxelShape shape, int rotation, boolean poleMounted, int mountCardinal) {
        return rotateUncached(shape, rotation, poleMounted, mountCardinal, 0, 0);
    }

    private static VoxelShape rotateUncached(VoxelShape shape, int rotation, boolean poleMounted, int mountCardinal,
            int originDx, int originDz) {
        boolean slice = needsSlices(rotation, poleMounted, mountCardinal);
        VoxelShape[] result = { Shapes.empty() };
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            if (slice) {
                forEachSlice(minX, minZ, maxX, maxZ, (x1, z1, x2, z2) ->
                        result[0] = Shapes.or(result[0],
                                rotateAabbY(x1, minY, z1, x2, maxY, z2, rotation, poleMounted, mountCardinal,
                                        originDx, originDz)));
            } else {
                result[0] = Shapes.or(result[0],
                        rotateAabbY(minX, minY, minZ, maxX, maxY, maxZ, rotation, poleMounted, mountCardinal,
                                originDx, originDz));
            }
        });
        VoxelShape rotated = result[0].optimize();
        return rotated.isEmpty() ? shape : rotated;
    }

    private static boolean needsSlices(int rotation, boolean poleMounted, int mountCardinal) {
        if (poleMounted) {
            int extra = RTCRotation.wrapDelta((rotation & 15) - (mountCardinal & 15));
            return extra != 0 || (mountCardinal & 3) != 0;
        }
        return (rotation & 3) != 0;
    }

    private interface SliceConsumer {
        void accept(double minX, double minZ, double maxX, double maxZ);
    }

    private static void forEachSlice(double minX, double minZ, double maxX, double maxZ, SliceConsumer consumer) {
        int nx = Math.max(1, (int) Math.ceil((maxX - minX) / SLICE - 1.0E-6));
        int nz = Math.max(1, (int) Math.ceil((maxZ - minZ) / SLICE - 1.0E-6));
        double stepX = (maxX - minX) / nx;
        double stepZ = (maxZ - minZ) / nz;
        for (int ix = 0; ix < nx; ix++) {
            double x1 = minX + stepX * ix;
            double x2 = ix == nx - 1 ? maxX : minX + stepX * (ix + 1);
            for (int iz = 0; iz < nz; iz++) {
                double z1 = minZ + stepZ * iz;
                double z2 = iz == nz - 1 ? maxZ : minZ + stepZ * (iz + 1);
                consumer.accept(x1, z1, x2, z2);
            }
        }
    }

    private static VoxelShape rotateAabbY(double minX, double minY, double minZ, double maxX, double maxY,
            double maxZ, int rotation, boolean poleMounted, int mountCardinal) {
        return rotateAabbY(minX, minY, minZ, maxX, maxY, maxZ, rotation, poleMounted, mountCardinal, 0, 0);
    }

    private static VoxelShape rotateAabbY(double minX, double minY, double minZ, double maxX, double maxY,
            double maxZ, int rotation, boolean poleMounted, int mountCardinal, int originDx, int originDz) {
        double[] xs = { minX, minX, maxX, maxX };
        double[] zs = { minZ, maxZ, minZ, maxZ };
        double minXr = Double.POSITIVE_INFINITY;
        double maxXr = Double.NEGATIVE_INFINITY;
        double minZr = Double.POSITIVE_INFINITY;
        double maxZr = Double.NEGATIVE_INFINITY;
        float[] xz = new float[2];
        for (int i = 0; i < 4; i++) {
            xz[0] = (float) xs[i];
            xz[1] = (float) zs[i];
            if (poleMounted) {
                if (originDx != 0 || originDz != 0) {
                    RTCRotation.rotateParentedXZ(xz, rotation, mountCardinal, originDx, originDz);
                } else {
                    RTCRotation.rotatePoleMountedXZ(xz, rotation, mountCardinal);
                }
            } else {
                RTCRotation.rotateAround(xz, 0.5F, 0.5F,
                        (float) Math.toRadians(RTCRotation.placementRotationDegrees(rotation)));
            }
            minXr = Math.min(minXr, xz[0]);
            maxXr = Math.max(maxXr, xz[0]);
            minZr = Math.min(minZr, xz[1]);
            maxZr = Math.max(maxZr, xz[1]);
        }
        if (!(minXr < maxXr) || !(minY < maxY) || !(minZr < maxZr)) {
            return Shapes.empty();
        }
        return Shapes.box(minXr, minY, minZr, maxXr, maxY, maxZr);
    }

    /** Create-sized core so a cell is still clickable if rotation clips the plate away. */
    private static final VoxelShape CELL_CORE = Block.box(6, 0, 6, 10, 16, 10);

    /**
     * In-cell pick shape: rotate like the model, then clip to the block so vanilla DDA
     * can hit this cell the same way it hits a Create shaft.
     */
    public static VoxelShape interact(VoxelShape inCell, int rotation, int mountCardinal) {
        VoxelShape clipped = clipToBlock(rotateYPoleMounted(inCell, rotation, mountCardinal));
        if (!clipped.isEmpty()) {
            return clipped;
        }
        return clipOrCore(rotateY(inCell, rotation));
    }

    public static VoxelShape clipOrCore(VoxelShape shape) {
        VoxelShape clipped = clipToBlock(shape);
        return clipped.isEmpty() ? CELL_CORE : clipped;
    }

    /** Keeps a non-empty in-block hitbox; out-of-cell model overhang is visual-only. */
    public static VoxelShape clipToBlock(VoxelShape shape) {
        if (shape.isEmpty()) {
            return shape;
        }
        VoxelShape[] result = { Shapes.empty() };
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double x1 = Math.max(0, minX);
            double y1 = Math.max(0, minY);
            double z1 = Math.max(0, minZ);
            double x2 = Math.min(1, maxX);
            double y2 = Math.min(1, maxY);
            double z2 = Math.min(1, maxZ);
            if (x1 < x2 && y1 < y2 && z1 < z2) {
                result[0] = Shapes.or(result[0], Shapes.box(x1, y1, z1, x2, y2, z2));
            }
        });
        return result[0];
    }

    private static int identity(VoxelShape shape) {
        int[] hash = { System.identityHashCode(shape) };
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            hash[0] = 31 * hash[0] + Double.hashCode(minX);
            hash[0] = 31 * hash[0] + Double.hashCode(minY);
            hash[0] = 31 * hash[0] + Double.hashCode(minZ);
            hash[0] = 31 * hash[0] + Double.hashCode(maxX);
            hash[0] = 31 * hash[0] + Double.hashCode(maxY);
            hash[0] = 31 * hash[0] + Double.hashCode(maxZ);
        });
        return hash[0];
    }

    private record RotKey(int src, int rotation, int mount, boolean poleMounted, int originDx, int originDz) {
    }
}
