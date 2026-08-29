package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.world.item.Item;

/**
 * Implemented by every traffic-light frame block so the shared block entity can discover how many bulb
 * slots it has and which frame item it drops, without needing a distinct block-entity class per type.
 */
public interface ITrafficLightBlock {
    int getBulbCount();

    Item getFrameItem();
}
