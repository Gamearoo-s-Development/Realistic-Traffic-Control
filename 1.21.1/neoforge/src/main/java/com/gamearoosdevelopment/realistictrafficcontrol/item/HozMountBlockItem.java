package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.HorizontalPoleBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Signs hang off the clicked hoz face. Top/bottom clicks use look yaw so the plate
 * sits on the bar instead of spawning above it.
 */
public class HozMountBlockItem extends BlockItem {

    public HozMountBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        BlockPos clickedPos = context.replacingClickedOnBlock()
                ? context.getClickedPos()
                : context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState clicked = context.getLevel().getBlockState(clickedPos);
        if (!(clicked.getBlock() instanceof HorizontalPoleBlock)) {
            return context;
        }
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal() && context.getPlayer() != null) {
            var look = context.getPlayer().getViewVector(1.0F);
            face = Direction.getNearest(look.x, 0.0, look.z);
        }
        return BlockPlaceContext.at(context, clickedPos, face);
    }
}
