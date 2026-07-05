package com.gamearoosdevelopment.realistictrafficcontrol.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Voxel helpers. 1.12.2 {@code AxisAlignedBB} constructors tolerated min &gt; max on an axis; 1.21
 * {@link Block#box} and {@link Shapes#box} do not.
 */
public final class RTCShapes {

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
}
