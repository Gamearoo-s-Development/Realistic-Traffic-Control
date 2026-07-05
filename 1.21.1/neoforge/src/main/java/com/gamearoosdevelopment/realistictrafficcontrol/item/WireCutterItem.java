package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.WireAnchorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Port of 1.12.2 {@code ItemWireCutter} with wire-anchor pairing logic. */
public class WireCutterItem extends Item {

    private static final String TAG_FIRST_POS = "FirstWireAnchor";

    public WireCutterItem(Properties properties) {
        super(properties.durability(40).stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof WireAnchorBlockEntity)) {
            return InteractionResult.PASS;
        }

        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = data.copyTag();

        if (!tag.contains(TAG_FIRST_POS)) {
            tag.putLong(TAG_FIRST_POS, pos.asLong());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (level.isClientSide) {
                player.displayClientMessage(Component.literal("First anchor set."), true);
            }
        } else {
            BlockPos firstPos = BlockPos.of(tag.getLong(TAG_FIRST_POS));
            BlockEntity firstTE = level.getBlockEntity(firstPos);
            BlockEntity secondTE = level.getBlockEntity(pos);
            if (firstTE instanceof WireAnchorBlockEntity a && secondTE instanceof WireAnchorBlockEntity b) {
                if (player.isShiftKeyDown()) {
                    boolean removedA = a.removeConnection(pos);
                    boolean removedB = b.removeConnection(firstPos);
                    if (level.isClientSide) {
                        if (removedA || removedB) {
                            player.displayClientMessage(Component.literal("Connection removed."), true);
                        } else {
                            player.displayClientMessage(Component.literal("No connection found to remove."), true);
                        }
                    }
                } else {
                    boolean addedA = a.addConnection(pos);
                    boolean addedB = b.addConnection(firstPos);
                    if (addedA && !addedB) {
                        a.removeConnection(pos);
                        addedA = false;
                    } else if (!addedA && addedB) {
                        b.removeConnection(firstPos);
                        addedB = false;
                    }
                    if (level.isClientSide) {
                        if (addedA && addedB) {
                            player.displayClientMessage(Component.literal("Anchors connected."), true);
                        } else {
                            player.displayClientMessage(Component.literal(
                                    "Failed to connect: anchor full or already connected."), true);
                        }
                    }
                }
            }
            tag.remove(TAG_FIRST_POS);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
