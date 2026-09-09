package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockDigitalSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight5Upper;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.ConnectingPoleBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.HorizontalPoleBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Create-style pairing: a frame/sign sits on the clicked pole face and shares that
 * pole's extra 22.5° so the two blocks turn as one.
 */
public final class PoleAssembly {

    private PoleAssembly() {
    }

    public static Direction mountFace(UseOnContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isHorizontal()) {
            return face;
        }
        if (context instanceof net.minecraft.world.item.context.BlockPlaceContext place) {
            return CustomAngleCalculator.horizontalFacingForPlacement(place);
        }
        return Direction.SOUTH;
    }

    public static int mountCardinal(BlockState state) {
        if (state.hasProperty(RTCProperties.MOUNT_FACE)) {
            return CustomAngleCalculator.getRotationForFacingCardinal(state.getValue(RTCProperties.MOUNT_FACE));
        }
        if (state.hasProperty(RTCProperties.ROTATION)) {
            return CustomAngleCalculator.nearestCardinal(state.getValue(RTCProperties.ROTATION));
        }
        return 0;
    }

    public static List<BlockPos> linked(Level level, BlockPos origin) {
        List<BlockPos> out = new ArrayList<>();
        out.add(origin);
        BlockState originState = level.getBlockState(origin);
        if (!originState.hasProperty(RTCProperties.ROTATION) || !isRtc(originState)) {
            return out;
        }
        int rot = originState.getValue(RTCProperties.ROTATION);
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = origin.relative(direction);
            BlockState other = level.getBlockState(neighbor);
            if (!other.hasProperty(RTCProperties.ROTATION) || !isRtc(other)) {
                continue;
            }
            if (isPole(originState) || isPole(other) || other.getValue(RTCProperties.ROTATION) == rot) {
                out.add(neighbor);
            }
        }
        return out;
    }

    public static void rotateLinked(Level level, BlockPos origin, int delta) {
        for (BlockPos pos : linked(level, origin)) {
            BlockState state = level.getBlockState(pos);
            if (!state.hasProperty(RTCProperties.ROTATION)) {
                continue;
            }
            int next = (state.getValue(RTCProperties.ROTATION) + delta) & 15;
            level.setBlockAndUpdate(pos, state.setValue(RTCProperties.ROTATION, next));
        }
    }

    public static boolean isPole(BlockState state) {
        return isPoleBlock(state.getBlock());
    }

    public static boolean isPoleBlock(net.minecraft.world.level.block.Block block) {
        if (block instanceof ConnectingPoleBlock || block instanceof HorizontalPoleBlock) {
            return true;
        }
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return path.equals("pole") || path.endsWith("_pole") || path.equals("pole_base");
    }

    /** Frames (including hoz) and signs that sit on a pole face. */
    public static boolean isFrameOrSign(BlockState state) {
        return isFrameOrSignBlock(state.getBlock());
    }

    public static boolean isFrameOrSignBlock(net.minecraft.world.level.block.Block block) {
        return block instanceof BlockBaseTrafficLight
                || block instanceof BlockTrafficLight5Upper
                || block instanceof BlockSign
                || block instanceof BlockDigitalSign
                || block instanceof BlockStreetSign;
    }

    /** Target the yellow face highlight can bind to. */
    public static boolean isPoleMountTarget(BlockState state) {
        return isRtc(state) && (isPole(state) || isFrameOrSign(state));
    }

    /** Items that place onto a pole face (frames, signs, poles, hoz bars). */
    public static boolean isPoleMountItem(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame) {
            return true;
        }
        if (item instanceof net.minecraft.world.item.BlockItem blockItem) {
            net.minecraft.world.level.block.Block block = blockItem.getBlock();
            return isPoleBlock(block) || isFrameOrSignBlock(block);
        }
        return false;
    }

    /** Adjacent hoz bar, if this frame/sign is sitting on one. */
    public record AdjacentHoz(BlockPos pos, BlockState state) {
    }

    public static AdjacentHoz adjacentHoz(BlockGetter level, BlockPos pos, BlockState state) {
        if (level == null) {
            return null;
        }
        if (state.hasProperty(RTCProperties.MOUNT_FACE)) {
            Direction toward = state.getValue(RTCProperties.MOUNT_FACE).getOpposite();
            if (toward.getAxis().isHorizontal()) {
                AdjacentHoz found = hozAt(level, pos, toward);
                if (found != null) {
                    return found;
                }
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            AdjacentHoz found = hozAt(level, pos, direction);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static AdjacentHoz hozAt(BlockGetter level, BlockPos pos, Direction towardHoz) {
        BlockPos hozPos = pos.relative(towardHoz);
        BlockState hoz = level.getBlockState(hozPos);
        if (!(hoz.getBlock() instanceof HorizontalPoleBlock)) {
            return null;
        }
        return new AdjacentHoz(hozPos, hoz);
    }

    /**
     * Sit on the clicked support face and copy that support's extra 22.5°.
     * Clicking the top of a hoz hangs the piece off the side you are looking at.
     */
    public record Place(BlockPos pos, int rotation, Direction mount) {
    }

    public static Place placeOnSupport(UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        BlockState clicked = context.getLevel().getBlockState(clickedPos);
        Direction face = context.getClickedFace();
        if (clicked.getBlock() instanceof HorizontalPoleBlock && !face.getAxis().isHorizontal()
                && context.getPlayer() != null) {
            var look = context.getPlayer().getViewVector(1.0F);
            face = Direction.getNearest(look.x, 0.0, look.z);
        }
        BlockPos pos = clicked.canBeReplaced() ? clickedPos : clickedPos.relative(face);
        int rotation;
        Direction mount;
        if (face.getAxis().isHorizontal()) {
            int mountRot = CustomAngleCalculator.getRotationForFacingCardinal(face);
            if (clicked.hasProperty(RTCProperties.ROTATION)) {
                rotation = (mountRot + RTCRotation.deltaFromCardinal(clicked.getValue(RTCProperties.ROTATION))) & 15;
            } else {
                rotation = mountRot;
            }
            mount = face;
        } else {
            rotation = CustomAngleCalculator.rotationForPlacement(context);
            mount = mountFace(context);
        }
        return new Place(pos, rotation, mount);
    }

    private static boolean isRtc(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace()
                .equals(ModRealisticTrafficControl.MODID);
    }
}
