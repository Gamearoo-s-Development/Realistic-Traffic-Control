package com.gamearoosdevelopment.realistictrafficcontrol.gui;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Pixel-perfect frame GUI layouts ported from the 1.12.2 {@code *FrameContainer} classes.
 */
public enum FrameGuiType {
    STANDARD_3(200, "traffic_light_frame_gui.png", 3,
            slot(CheckboxOrientation.RIGHT, 0, 79, 13), slot(CheckboxOrientation.RIGHT, 1, 79, 44),
            slot(CheckboxOrientation.RIGHT, 2, 79, 76)),
    HORIZONTAL_3(200, "traffic_light_hoz_frame_gui.png", 3,
            slot(CheckboxOrientation.ABOVE, 0, 60, 30), slot(CheckboxOrientation.RIGHT, 1, 95, 30),
            slot(CheckboxOrientation.RIGHT, 2, 130, 30)),
    STANDARD_1(200, "traffic_light_1_frame_gui.png", 1,
            slot(CheckboxOrientation.RIGHT, 0, 79, 44)),
    STANDARD_2(200, "traffic_light_2_frame_gui.png", 2,
            slot(CheckboxOrientation.RIGHT, 0, 79, 13), slot(CheckboxOrientation.RIGHT, 1, 79, 44)),
    HORIZONTAL_2(200, "traffic_light_2_hoz_frame_gui.png", 2,
            slot(CheckboxOrientation.ABOVE, 0, 60, 30), slot(CheckboxOrientation.RIGHT, 1, 95, 30)),
    STANDARD_4(263, "traffic_light_4_frame_gui.png", 4,
            slot(CheckboxOrientation.RIGHT, 0, 79, 44), slot(CheckboxOrientation.RIGHT, 1, 79, 75),
            slot(CheckboxOrientation.RIGHT, 2, 79, 106), slot(CheckboxOrientation.RIGHT, 3, 79, 139)),
    HORIZONTAL_4(263, "traffic_light_4_hoz_frame_gui.png", 4,
            slot(CheckboxOrientation.LEFT, 0, 30, 90), slot(CheckboxOrientation.ABOVE, 1, 62, 90),
            slot(CheckboxOrientation.BELOW, 2, 95, 90), slot(CheckboxOrientation.RIGHT, 3, 125, 90)),
    STANDARD_5(263, "traffic_light_5_frame_gui.png", 5,
            slot(CheckboxOrientation.RIGHT, 0, 79, 12), slot(CheckboxOrientation.RIGHT, 1, 79, 44),
            slot(CheckboxOrientation.RIGHT, 2, 79, 76), slot(CheckboxOrientation.RIGHT, 3, 79, 107),
            slot(CheckboxOrientation.RIGHT, 4, 79, 139)),
    HORIZONTAL_5(263, "traffic_light_5_hoz_frame_gui.png", 5,
            slot(CheckboxOrientation.RIGHT, 0, 15, 76), slot(CheckboxOrientation.RIGHT, 1, 45, 76),
            slot(CheckboxOrientation.RIGHT, 2, 79, 76), slot(CheckboxOrientation.RIGHT, 3, 110, 76),
            slot(CheckboxOrientation.RIGHT, 4, 140, 76)),
    DOGHOUSE_5(210, "traffic_light_doghouse_frame_gui.png", 5,
            slot(CheckboxOrientation.RIGHT, 0, 79, 10), slot(CheckboxOrientation.LEFT, 1, 48, 54),
            slot(CheckboxOrientation.LEFT, 2, 48, 86), slot(CheckboxOrientation.RIGHT, 3, 110, 54),
            slot(CheckboxOrientation.RIGHT, 4, 110, 86)),
    T_4(210, "traffic_light_6_frame_gui.png", 4,
            slot(CheckboxOrientation.LEFT, 0, 59, 11), slot(CheckboxOrientation.RIGHT, 1, 110, 11),
            slot(CheckboxOrientation.RIGHT, 2, 83, 52), slot(CheckboxOrientation.RIGHT, 3, 83, 95)),
    HAWK_3(210, "traffic_light_7_frame_gui.png", 3,
            slot(CheckboxOrientation.LEFT, 0, 59, 11), slot(CheckboxOrientation.RIGHT, 1, 110, 11),
            slot(CheckboxOrientation.RIGHT, 2, 83, 52)),
    UPSIDE_DOWN_T_4(210, "traffic_light_8_frame_gui.png", 4,
            slot(CheckboxOrientation.RIGHT, 0, 76, 11), slot(CheckboxOrientation.RIGHT, 1, 76, 48),
            slot(CheckboxOrientation.RIGHT, 2, 55, 85), slot(CheckboxOrientation.RIGHT, 3, 98, 85));

    public static final int WIDTH = 174;

    private final int height;
    private final String textureName;
    private final int bulbCount;
    private final List<SlotLayout> slots;

    FrameGuiType(int height, String textureName, int bulbCount, SlotLayout... slots) {
        this.height = height;
        this.textureName = textureName;
        this.bulbCount = bulbCount;
        this.slots = ImmutableList.copyOf(slots);
    }

    public int getHeight() {
        return height;
    }

    public String getTextureName() {
        return textureName;
    }

    public int getBulbCount() {
        return bulbCount;
    }

    public List<SlotLayout> getSlots() {
        return slots;
    }

    public int playerInventoryTopY() {
        return 119 + (height - 200);
    }

    public int hotbarY() {
        return 177 + (height - 200);
    }

    private static SlotLayout slot(CheckboxOrientation orientation, int index, int x, int y) {
        return new SlotLayout(orientation, index, x, y);
    }

    public enum CheckboxOrientation {
        LEFT, RIGHT, ABOVE, BELOW
    }

    public record SlotLayout(CheckboxOrientation checkboxOrientation, int slotIndex, int primaryX, int primaryY) {
        public int secondaryX() {
            return primaryX + 20;
        }

        public int secondaryY() {
            return primaryY;
        }
    }
}
