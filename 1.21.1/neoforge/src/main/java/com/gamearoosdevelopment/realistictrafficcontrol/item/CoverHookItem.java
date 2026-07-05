package com.gamearoosdevelopment.realistictrafficcontrol.item;

import net.minecraft.world.item.Item;

/**
 * Port of 1.12.2 {@code ItemCoverHook}. A durable tool; the actual "toggle cover" interaction is
 * handled by the traffic-light blocks when this item is used on them (see the traffic-light block port).
 */
public class CoverHookItem extends Item {
    public CoverHookItem(Properties properties) {
        super(properties.durability(40).stacksTo(1));
    }
}
