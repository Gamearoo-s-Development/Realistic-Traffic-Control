package com.gamearoosdevelopment.realistictrafficcontrol.menu;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.TrafficLightBulbItem;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Item-backed bulb inventory for a traffic-light frame, replacing the 1.12.2 {@code IItemHandler}
 * capability on the frame {@code ItemStack}. Holds {@code 2 * bulbCount} slots (primary layer first, then
 * secondary layer). Its contents are (de)serialized to the frame's {@code frame_data} component using the
 * same {@code bulbTypes}/{@code secondaryBulbTypes} int-array schema the block entity reads on placement.
 */
public class FrameBulbContainer extends SimpleContainer {

    private final ItemStack frameStack;
    private final int bulbCount;
    private boolean loading;

    public FrameBulbContainer(ItemStack frameStack, int bulbCount) {
        super(bulbCount * 2);
        this.frameStack = frameStack;
        this.bulbCount = bulbCount;
        load();
    }

    public ItemStack getFrameStack() {
        return frameStack;
    }

    private void load() {
        loading = true;
        CompoundTag tag = BaseItemTrafficLightFrame.getData(frameStack);
        int[] primary = tag.getIntArray("bulbTypes");
        int[] secondary = tag.getIntArray("secondaryBulbTypes");
        for (int i = 0; i < bulbCount; i++) {
            if (i < primary.length && primary[i] >= 0) {
                setItem(i, bulb(primary[i]));
            }
            if (i < secondary.length && secondary[i] >= 0) {
                setItem(bulbCount + i, bulb(secondary[i]));
            }
        }
        loading = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!loading) {
            save();
        }
    }

    private void save() {
        CompoundTag tag = BaseItemTrafficLightFrame.getData(frameStack);
        int[] primary = new int[bulbCount];
        int[] secondary = new int[bulbCount];
        for (int i = 0; i < bulbCount; i++) {
            primary[i] = typeOf(getItem(i));
            secondary[i] = typeOf(getItem(bulbCount + i));
        }
        tag.putIntArray("bulbTypes", primary);
        tag.putIntArray("secondaryBulbTypes", secondary);
        frameStack.set(com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents.FRAME_DATA.get(), tag);
    }

    private static int typeOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TrafficLightBulbItem)) {
            return -1;
        }
        return TrafficLightBulbItem.getType(stack);
    }

    private static ItemStack bulb(int index) {
        EnumTrafficLightBulbTypes type = EnumTrafficLightBulbTypes.get(index);
        if (type == null) {
            return ItemStack.EMPTY;
        }
        return TrafficLightBulbItem.of(ModItems.TRAFFIC_LIGHT_BULB.get(), type);
    }
}
