package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockConcreteBarrier;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ConcreteBarrierBlockItem extends BlockItem {

    private static final String[] DYE_LANG = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    public ConcreteBarrierBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static int getDye(ItemStack stack) {
        Integer dye = stack.get(RTCDataComponents.BARRIER_DYE.get());
        return dye == null ? 0 : Mth.clamp(dye, 0, 15);
    }

    public static ItemStack withDye(Item item, int dye) {
        ItemStack stack = new ItemStack(item);
        stack.set(RTCDataComponents.BARRIER_DYE.get(), Mth.clamp(dye, 0, 15));
        return stack;
    }

    public static ItemStack withDye(ItemStack template, int dye) {
        ItemStack stack = template.copyWithCount(1);
        stack.set(RTCDataComponents.BARRIER_DYE.get(), Mth.clamp(dye, 0, 15));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        int dye = getDye(stack);
        return Component.translatable("realistictrafficcontrol.concrete_barrier." + DYE_LANG[dye] + ".name");
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        return state.setValue(BlockConcreteBarrier.DYE, getDye(context.getItemInHand()));
    }
}
