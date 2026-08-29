package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Shared block-state properties. In 1.21.1 a {@link net.minecraft.world.level.block.state.properties.Property}
 * instance can be reused across many blocks, so the mod's ubiquitous 0-15 "rotation" step property lives
 * here instead of being redeclared per class like it was in 1.12.2.
 */
public final class RTCProperties {
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 15);
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

    private RTCProperties() {
    }
}
