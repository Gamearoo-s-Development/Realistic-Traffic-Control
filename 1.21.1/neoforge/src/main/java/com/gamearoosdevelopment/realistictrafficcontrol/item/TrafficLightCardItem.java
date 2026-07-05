package com.gamearoosdevelopment.realistictrafficcontrol.item;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.TrafficSensorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CC:Tweaked traffic-light card item (tiers 1-3 + creative). Port of 1.12.2 {@code ItemTrafficLightCard}.
 */
public class TrafficLightCardItem extends Item {

    public TrafficLightCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int getTier(ItemStack stack) {
        Integer tier = stack.get(RTCDataComponents.CARD_TIER.get());
        return tier == null ? 0 : tier;
    }

    public static ItemStack withTier(Item item, int tier) {
        ItemStack stack = new ItemStack(item);
        stack.set(RTCDataComponents.CARD_TIER.get(), tier);
        return stack;
    }

    public static int getMaxTrafficLights(int tier) {
        return switch (tier) {
            case 1 -> Config.trafficLightCardT2Capacity;
            case 2 -> Config.trafficLightCardT3Capacity;
            case 3 -> Integer.MAX_VALUE;
            default -> Config.trafficLightCardT1Capacity;
        };
    }

    public static int getMaxSensors() {
        return 20;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        boolean isTrafficLight = block instanceof BlockBaseTrafficLight;
        boolean isSensor = block instanceof TrafficSensorBlock;
        if (!isTrafficLight && !isSensor) {
            return InteractionResult.PASS;
        }

        ItemStack heldStack = context.getItemInHand();
        var stackTag = getOrCreateCardData(heldStack);
        long id = pos.asLong();

        if (isSensor) {
            return handleSensorPairing(context, pos, heldStack, stackTag, id);
        }
        return handleTrafficLightPairing(context, pos, heldStack, stackTag, id);
    }

    private InteractionResult handleSensorPairing(UseOnContext context, BlockPos pos, ItemStack heldStack,
            net.minecraft.nbt.CompoundTag stackTag, long id) {
        int maxSensors = getMaxSensors();
        HashSet<Long> sensors = new HashSet<>();
        for (int i = 0; i < maxSensors; i++) {
            if (stackTag.contains("sensor" + i)) {
                sensors.add(stackTag.getLong("sensor" + i));
            }
        }

        if (sensors.contains(id)) {
            for (int i = 0; i < maxSensors; i++) {
                if (stackTag.contains("sensor" + i) && stackTag.getLong("sensor" + i) == id) {
                    stackTag.remove("sensor" + i);
                    heldStack.set(RTCDataComponents.CARD_DATA.get(), stackTag);
                    context.getPlayer().displayClientMessage(Component.literal(
                            String.format("Unpaired sensor at [%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ())), false);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        if (sensors.size() >= maxSensors) {
            context.getPlayer().displayClientMessage(Component.literal("Card has reached max sensor capacity."), false);
            return InteractionResult.SUCCESS;
        }

        for (int i = 0; i < maxSensors; i++) {
            if (!stackTag.contains("sensor" + i)) {
                stackTag.putLong("sensor" + i, id);
                heldStack.set(RTCDataComponents.CARD_DATA.get(), stackTag);
                context.getPlayer().displayClientMessage(Component.literal(
                        String.format("Paired sensor at [%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ())), false);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleTrafficLightPairing(UseOnContext context, BlockPos pos, ItemStack heldStack,
            net.minecraft.nbt.CompoundTag stackTag, long id) {
        int maxTrafficLights = getMaxTrafficLights(getTier(heldStack));

        HashSet<String> keysToRemove = new HashSet<>();
        for (String key : stackTag.getAllKeys()) {
            if (key.startsWith("light") && stackTag.getLong(key) == id) {
                keysToRemove.add(key);
            }
        }
        if (!keysToRemove.isEmpty()) {
            for (String key : keysToRemove) {
                stackTag.remove(key);
            }
            heldStack.set(RTCDataComponents.CARD_DATA.get(), stackTag);
            context.getPlayer().displayClientMessage(Component.literal(
                    String.format("Removed traffic light at [%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ())), false);
            return InteractionResult.SUCCESS;
        }

        int totalLights = 0;
        HashSet<Integer> usedSlots = new HashSet<>();
        for (String key : stackTag.getAllKeys()) {
            if (key.startsWith("light")) {
                totalLights++;
                try {
                    usedSlots.add(Integer.parseInt(key.substring(5)));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (totalLights >= maxTrafficLights) {
            context.getPlayer().displayClientMessage(Component.literal(
                    "Card is full! Remove a traffic light or upgrade this card."), false);
        } else {
            int nextSlot = 0;
            while (usedSlots.contains(nextSlot)) {
                nextSlot++;
            }
            stackTag.putLong("light" + nextSlot, id);
            heldStack.set(RTCDataComponents.CARD_DATA.get(), stackTag);
            String msg = String.format("Added traffic light at [%d, %d, %d].", pos.getX(), pos.getY(), pos.getZ());
            if (maxTrafficLights != Integer.MAX_VALUE) {
                msg += String.format(" %d/%d slots remaining.", maxTrafficLights - totalLights - 1, maxTrafficLights);
            }
            context.getPlayer().displayClientMessage(Component.literal(msg), false);
        }
        return InteractionResult.SUCCESS;
    }

    private static net.minecraft.nbt.CompoundTag getOrCreateCardData(ItemStack stack) {
        var tag = stack.get(RTCDataComponents.CARD_DATA.get());
        if (tag == null) {
            tag = new net.minecraft.nbt.CompoundTag();
            stack.set(RTCDataComponents.CARD_DATA.get(), tag);
        }
        return tag;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (getTier(stack) == 3) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }
        var tag = stack.get(RTCDataComponents.CARD_DATA.get());
        int totalUsed = 0;
        if (tag != null) {
            totalUsed = (int) tag.getAllKeys().stream().filter(k -> k.startsWith("light")).count();
        }
        int maxAvailable = getMaxTrafficLights(getTier(stack));
        tooltip.add(Component.literal(totalUsed + "/" + maxAvailable + " slots filled")
                .withStyle(style -> style.withItalic(true).withColor(0xAA00AA)));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public Component getName(ItemStack stack) {
        String suffix = switch (getTier(stack)) {
            case 1 -> "tier2";
            case 2 -> "tier3";
            case 3 -> "creative";
            default -> "tier1";
        };
        return Component.translatable(getDescriptionId() + "." + suffix);
    }
}
