package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Data for one street-sign panel on a {@link StreetSignBlockEntity}. */
public class StreetSign {
    private int rotation;
    private StreetSignColors color = StreetSignColors.Green;
    private String text = "";
    private boolean isNew = true;

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public StreetSignColors getColor() {
        return color;
    }

    public void setColor(StreetSignColors color) {
        this.color = color != null ? color : StreetSignColors.Green;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public int getTextColor() {
        return switch (getColor()) {
            case Yellow -> 0x000000;
            default -> 0xFFFFFF;
        };
    }

    public enum StreetSignColors {
        Green(0, 1, 1), Red(1, 1, 2), Blue(2, 1, 3), Yellow(3, 1, 4);

        private final int index;
        private final int col;
        private final int row;

        StreetSignColors(int index, int col, int row) {
            this.index = index;
            this.col = col;
            this.row = row;
        }

        public int getIndex() {
            return index;
        }

        public int getCol() {
            return col;
        }

        public int getRow() {
            return row;
        }

        public static StreetSignColors getByIndex(int index) {
            for (StreetSignColors c : values()) {
                if (c.getIndex() == index) {
                    return c;
                }
            }
            return Green;
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("rotation", CustomAngleCalculator.rotationToMeta(getRotation()));
        tag.putInt("color", getColor().getIndex());
        tag.putString("text", getText());
        tag.putBoolean("isNew", getIsNew());
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        String rotationKey = tag.contains("facing") ? "facing" : "rotation";
        setRotation(CustomAngleCalculator.metaToRotation(tag.getInt(rotationKey)));
        setColor(StreetSignColors.getByIndex(tag.getInt("color")));
        setText(tag.getString("text"));
        setIsNew(tag.getBoolean("isNew"));
    }
}
