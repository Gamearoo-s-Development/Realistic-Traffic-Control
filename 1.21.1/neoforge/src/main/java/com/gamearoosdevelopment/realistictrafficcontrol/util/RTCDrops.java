package com.gamearoosdevelopment.realistictrafficcontrol.util;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Self-drop helper for RTC blocks that have no loot table. */
public final class RTCDrops {
    private RTCDrops() {
    }

    public static List<ItemStack> self(BlockState state) {
        return self(state.getBlock());
    }

    public static List<ItemStack> self(Block block) {
        Item item = block.asItem();
        return item == Items.AIR ? List.of() : List.of(new ItemStack(item));
    }
}
