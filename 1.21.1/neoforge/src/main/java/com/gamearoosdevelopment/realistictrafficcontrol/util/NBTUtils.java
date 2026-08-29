package com.gamearoosdevelopment.realistictrafficcontrol.util;

import net.minecraft.nbt.CompoundTag;

public class NBTUtils {
    public static float getFloatOrDefault(CompoundTag tag, String key, float defaultValue) {
        if (tag.contains(key)) {
            return tag.getFloat(key);
        }

        return defaultValue;
    }

    public static int getIntOrDefault(CompoundTag tag, String key, int defaultValue) {
        if (tag.contains(key)) {
            return tag.getInt(key);
        }

        return defaultValue;
    }
}
