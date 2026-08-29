package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Port of 1.12.2 {@code BlockLightSource}: an invisible, non-collidable, light-emitting, replaceable
 * marker block placed by street lights to illuminate the area.
 */
public class LightSourceBlock extends Block {
    public LightSourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }
}
