package com.gamearoosdevelopment.realistictrafficcontrol.item;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Port of the 1.12.2 traffic-light bulb item, which used 35 vanilla metadata variants. In 1.21.1 there is
 * a single item whose variant is carried in the {@link RTCDataComponents#BULB_TYPE} data component; the
 * frame menu reads it to configure a placed traffic light.
 */
public class TrafficLightBulbItem extends Item {

    public TrafficLightBulbItem(Properties properties) {
        super(properties);
    }

    public static int getType(ItemStack stack) {
        Integer type = stack.get(RTCDataComponents.BULB_TYPE.get());
        return type == null ? 0 : type;
    }

    public static ItemStack of(Item item, EnumTrafficLightBulbTypes type) {
        ItemStack stack = new ItemStack(item);
        stack.set(RTCDataComponents.BULB_TYPE.get(), type.getIndex());
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EnumTrafficLightBulbTypes type = EnumTrafficLightBulbTypes.get(getType(stack));
        tooltip.add(Component.literal(type == null ? "Unknown" : type.name()));
    }

    @Override
    public Component getName(ItemStack stack) {
        EnumTrafficLightBulbTypes type = EnumTrafficLightBulbTypes.get(getType(stack));
        String label = type == null ? "Bulb" : type.name();
        return Component.translatable(getDescriptionId()).append(" (" + label + ")");
    }
}
