package com.gamearoosdevelopment.realistictrafficcontrol.menu;

import com.gamearoosdevelopment.realistictrafficcontrol.ModMenus;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.FrameGuiType;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.FrameGuiType.SlotLayout;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.TrafficLightBulbItem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Port of the 1.12.2 {@code BaseTrafficLightFrameContainer}. Slot positions and inventory layout match
 * the original per-frame GUI containers exactly.
 */
public class TrafficLightFrameMenu extends AbstractContainerMenu {

    private final FrameBulbContainer bulbs;
    private final FrameGuiType layout;
    private final int frameSlotStartIndex;

    public TrafficLightFrameMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory, playerInventory.player.getMainHandItem(), FrameGuiType.values()[buf.readVarInt()]);
    }

    public TrafficLightFrameMenu(int id, Inventory playerInventory, ItemStack frameStack, FrameGuiType layout) {
        super(ModMenus.TRAFFIC_LIGHT_FRAME.get(), id);
        this.layout = layout;
        this.bulbs = new FrameBulbContainer(frameStack, layout.getBulbCount());

        int invTop = layout.playerInventoryTopY();
        int hotbarY = layout.hotbarY();

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 7 + i * 18, hotbarY));
        }
        for (int i = 9; i < 36; i++) {
            int row = (i - 9) / 9;
            int col = i % 9;
            addSlot(new Slot(playerInventory, i, 7 + col * 18, invTop + row * 18));
        }

        frameSlotStartIndex = slots.size();
        for (SlotLayout slotLayout : layout.getSlots()) {
            addSlot(new BulbSlot(bulbs, slotLayout.slotIndex(), slotLayout.primaryX(), slotLayout.primaryY()));
            addSlot(new BulbSlot(bulbs, layout.getBulbCount() + slotLayout.slotIndex(), slotLayout.secondaryX(),
                    slotLayout.secondaryY()));
        }
    }

    public FrameGuiType getLayout() {
        return layout;
    }

    public ItemStack getFrameStack() {
        return bulbs.getFrameStack();
    }

    public int getFrameSlotStartIndex() {
        return frameSlotStartIndex;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().getItem() instanceof BaseItemTrafficLightFrame;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index >= frameSlotStartIndex) {
            if (!moveItemStackTo(stack, 0, frameSlotStartIndex, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, frameSlotStartIndex, slots.size(), false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    private static class BulbSlot extends Slot {
        BulbSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof TrafficLightBulbItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
