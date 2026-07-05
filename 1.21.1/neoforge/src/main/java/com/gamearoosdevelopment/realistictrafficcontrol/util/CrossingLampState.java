package com.gamearoosdevelopment.realistictrafficcontrol.util;

import net.minecraft.util.StringRepresentable;

/**
 * Port of 1.12.2 {@code BlockLampBase.EnumState}: flash phase for crossing lamps and gate lights
 * driven by {@link com.gamearoosdevelopment.realistictrafficcontrol.tileentity.RelayBlockEntity}.
 */
public enum CrossingLampState implements StringRepresentable {
    Off(0, "off"),
    Flash1(1, "flash1"),
    Flash2(2, "flash2");

    private final int id;
    private final String name;

    CrossingLampState(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getSerializedName() {
        return name;
    }

    public static CrossingLampState byId(int id) {
        for (CrossingLampState state : values()) {
            if (state.id == id) {
                return state;
            }
        }
        return Off;
    }
}
