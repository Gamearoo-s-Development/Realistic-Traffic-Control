package com.gamearoosdevelopment.realistictrafficcontrol.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/**
 * Port of 1.12.2 {@code ItemCone}: a {@link BlockItem} for cones/channelizers/drums. The 1.12.2 version
 * reported HEAD equipment slot (wearable cone gag); that cosmetic is dropped for the 1.21.1 port.
 */
public class ItemCone extends BlockItem {
    public ItemCone(Block block, Properties properties) {
        super(block, properties);
    }
}
