package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;

import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Shared block-state properties. In 1.21.1 a {@link net.minecraft.world.level.block.state.properties.Property}
 * instance can be reused across many blocks, so the mod's ubiquitous 0-15 "rotation" step property lives
 * here instead of being redeclared per class like it was in 1.12.2.
 */
public final class RTCProperties {
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 15);
    /** Face of the pole this frame/sign sits on (Create-style pair with the support). */
    public static final DirectionProperty MOUNT_FACE = BlockStateProperties.HORIZONTAL_FACING;
    /** Concrete barrier dye (0 = white, 1–15 = colored concrete). */
    public static final IntegerProperty BARRIER_DYE = IntegerProperty.create("dye", 0, 15);
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty COVER = BooleanProperty.create("cover");
    public static final BooleanProperty POLE = BooleanProperty.create("pole");
    public static final BooleanProperty VALIDHORIZONTALBAR = BooleanProperty.create("validhorizontalbar");
    public static final BooleanProperty VALIDBACKBAR = BooleanProperty.create("validbackbar");
    public static final BooleanProperty ISHALFHEIGHT = BooleanProperty.create("ishalfheight");
    public static final BooleanProperty ISFURTHESTLEFT = BooleanProperty.create("isfurthestleft");
    public static final BooleanProperty ISFURTHESTRIGHT = BooleanProperty.create("isfurthestright");
    public static final BooleanProperty WOOD = BooleanProperty.create("wood");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final EnumProperty<CrossingLampState> LAMP_STATE =
            EnumProperty.create("state", CrossingLampState.class);

    public static BlockState rotate16(BlockState state, Rotation rotation) {
        if (!state.hasProperty(ROTATION)) {
            return state;
        }
        int steps = switch (rotation) {
            case CLOCKWISE_90 -> 4;
            case CLOCKWISE_180 -> 8;
            case COUNTERCLOCKWISE_90 -> 12;
            default -> 0;
        };
        state = state.setValue(ROTATION, (state.getValue(ROTATION) + steps) & 15);
        if (state.hasProperty(MOUNT_FACE)) {
            state = state.setValue(MOUNT_FACE, rotation.rotate(state.getValue(MOUNT_FACE)));
        }
        return state;
    }

    public static BlockState mirror16(BlockState state, Mirror mirror) {
        if (!state.hasProperty(ROTATION)) {
            return state;
        }
        int current = state.getValue(ROTATION);
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(ROTATION, (16 - current) & 15);
            case FRONT_BACK -> state.setValue(ROTATION, (8 - current) & 15);
            default -> state;
        };
    }

    public static BlockState rotateFacing(BlockState state, Rotation rotation, DirectionProperty facing) {
        return state.setValue(facing, rotation.rotate(state.getValue(facing)));
    }

    public static BlockState mirrorFacing(BlockState state, Mirror mirror, DirectionProperty facing) {
        return rotateFacing(state, mirror.getRotation(state.getValue(facing)), facing);
    }

    private RTCProperties() {
    }
}
