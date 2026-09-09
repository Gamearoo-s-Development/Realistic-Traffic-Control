package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Create shaft-style stack: holding a pole and clicking the same pole type walks
 * up or down that column and places the next matching piece.
 */
public class PoleBlockItem extends BlockItem {

    private static final int EXTEND_RANGE = 32;

    public PoleBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        BlockPos polePos = clickedBlock(context);
        BlockState pole = context.getLevel().getBlockState(polePos);
        if (pole.getBlock() != getBlock()) {
            return context;
        }
        Vec3 look = context.getPlayer() != null
                ? context.getPlayer().getViewVector(1.0F)
                : context.getClickLocation();
        Direction along = extendAlong(context.getClickedFace(), look);
        int attached = attachedPoles(context.getLevel(), polePos, along, pole);
        if (attached >= EXTEND_RANGE) {
            along = along.getOpposite();
            attached = attachedPoles(context.getLevel(), polePos, along, pole);
            if (attached >= EXTEND_RANGE) {
                return context;
            }
        }
        BlockPos last = polePos.relative(along, attached);
        if (!context.getLevel().getBlockState(last.relative(along)).canBeReplaced()) {
            along = along.getOpposite();
            attached = attachedPoles(context.getLevel(), polePos, along, pole);
            last = polePos.relative(along, attached);
            if (attached >= EXTEND_RANGE
                    || !context.getLevel().getBlockState(last.relative(along)).canBeReplaced()) {
                return context;
            }
        }
        return BlockPlaceContext.at(context, last, along);
    }

    private static Direction extendAlong(Direction clickedFace, Vec3 look) {
        if (clickedFace.getAxis() == Direction.Axis.Y) {
            return clickedFace;
        }
        return look.y >= 0 ? Direction.UP : Direction.DOWN;
    }

    private static int attachedPoles(BlockGetter level, BlockPos origin, Direction along, BlockState originState) {
        int count = 0;
        BlockPos cursor = origin.relative(along);
        while (matchesRun(originState, level.getBlockState(cursor)) && count < EXTEND_RANGE) {
            count++;
            cursor = cursor.relative(along);
        }
        return count;
    }

    private static boolean matchesRun(BlockState origin, BlockState other) {
        if (other.getBlock() != origin.getBlock()) {
            return false;
        }
        if (origin.hasProperty(RTCProperties.ROTATION) && other.hasProperty(RTCProperties.ROTATION)) {
            return other.getValue(RTCProperties.ROTATION).intValue()
                    == origin.getValue(RTCProperties.ROTATION).intValue();
        }
        return true;
    }

    private static BlockPos clickedBlock(BlockPlaceContext context) {
        return context.replacingClickedOnBlock()
                ? context.getClickedPos()
                : context.getClickedPos().relative(context.getClickedFace().getOpposite());
    }
}
