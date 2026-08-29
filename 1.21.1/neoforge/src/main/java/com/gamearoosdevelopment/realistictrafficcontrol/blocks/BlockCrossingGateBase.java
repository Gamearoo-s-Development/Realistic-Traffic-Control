package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of 1.12.2 {@code BlockCrossingGateBase}: simple rotated crossing-gate mechanism base (no block entity).
 */
public class BlockCrossingGateBase extends RotatedBlock {

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public BlockCrossingGateBase(Properties properties) {
        super(properties, SHAPE);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
