package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.HorizontalPoleBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Create shaft-style extend: only when vanilla is already looking at a hoz, walk that
 * run and place at the first empty cell. Pole and ground clicks stay normal placement.
 */
public class HorizontalPoleBlockItem extends BlockItem {

    public HorizontalPoleBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        BlockPos hozPos = clickedBlock(context);
        BlockState hoz = context.getLevel().getBlockState(hozPos);
        if (!(hoz.getBlock() instanceof HorizontalPoleBlock)) {
            return context;
        }
        Vec3 look = context.getPlayer() != null
                ? context.getPlayer().getViewVector(1.0F)
                : context.getClickLocation();
        HorizontalPoleBlock.Extend extend = HorizontalPoleBlock.findExtend(
                context.getLevel(), hozPos, hoz, context.getClickedFace(),
                context.getClickLocation(), look);
        if (extend == null) {
            return context;
        }
        return BlockPlaceContext.at(context, extend.last(), extend.along());
    }

    /** The block vanilla actually hit. {@code getClickedPos()} is the neighbor placement cell. */
    private static BlockPos clickedBlock(BlockPlaceContext context) {
        return context.replacingClickedOnBlock()
                ? context.getClickedPos()
                : context.getClickedPos().relative(context.getClickedFace().getOpposite());
    }
}
