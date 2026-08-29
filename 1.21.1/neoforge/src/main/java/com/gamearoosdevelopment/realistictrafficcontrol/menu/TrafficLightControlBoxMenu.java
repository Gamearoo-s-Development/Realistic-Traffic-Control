package com.gamearoosdevelopment.realistictrafficcontrol.menu;

import com.gamearoosdevelopment.realistictrafficcontrol.ModMenus;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menu for the traffic-light control box GUI. Holds the block position only; the screen reads
 * {@link TrafficLightControlBoxBlockEntity} state from the client level (matching 1.12.2 {@code GuiScreen}).
 */
public class TrafficLightControlBoxMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public TrafficLightControlBoxMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory, buf.readBlockPos());
    }

    public TrafficLightControlBoxMenu(int id, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenus.TRAFFIC_LIGHT_CONTROL_BOX.get(), id);
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public TrafficLightControlBoxBlockEntity getControlBox(Player player) {
        BlockEntity be = player.level().getBlockEntity(blockPos);
        return be instanceof TrafficLightControlBoxBlockEntity box ? box : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0
                && player.level().getBlockEntity(blockPos) instanceof TrafficLightControlBoxBlockEntity;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
