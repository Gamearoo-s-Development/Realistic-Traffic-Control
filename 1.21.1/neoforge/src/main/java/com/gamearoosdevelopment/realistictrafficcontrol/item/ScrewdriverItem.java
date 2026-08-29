package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Port of 1.12.2 {@code ItemScrewdriver}. Cycles the "rotation" or "facing" blockstate property of any
 * Realistic Traffic Control block it is used on. Tile-entity driven rotation (IHasRotationProperty) will
 * be reconnected during the block-entity phase.
 */
public class ScrewdriverItem extends Item {
    public ScrewdriverItem(Properties properties) {
        super(properties.durability(128).stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.PASS;
        }

        BlockState state = level.getBlockState(pos);
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!id.getNamespace().equalsIgnoreCase(ModRealisticTrafficControl.MODID)) {
            return InteractionResult.PASS;
        }

        boolean sneaking = player.isShiftKeyDown();

        for (Property<?> property : state.getProperties()) {
            if (property.getName().equalsIgnoreCase("rotation") && property instanceof IntegerProperty intProp) {
                int current = state.getValue(intProp);
                int next = sneaking ? current - 1 : current + 1;
                if (next < 0) {
                    next = 15;
                }
                if (next >= 16) {
                    next = 0;
                }
                level.setBlockAndUpdate(pos, state.setValue(RTCProperties.ROTATION, next));
                damage(context, player);
                return InteractionResult.SUCCESS;
            }

            if (property.getName().equalsIgnoreCase("facing") && property instanceof DirectionProperty dirProp) {
                Direction current = state.getValue(dirProp);
                Direction next = sneaking ? current.getCounterClockWise() : current.getClockWise();
                level.setBlockAndUpdate(pos, state.setValue(dirProp, next));
                damage(context, player);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private void damage(UseOnContext context, Player player) {
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
    }
}
