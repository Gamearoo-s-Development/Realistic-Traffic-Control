package com.gamearoosdevelopment.realistictrafficcontrol.menu;

import com.gamearoosdevelopment.realistictrafficcontrol.ModMenus;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignControllerBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class DisplayMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    public DisplayMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public DisplayMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.DISPLAY.get(), id);
        this.pos = pos;
    }
    public BlockPos getBlockPos() { return pos; }
    public BlockEntity getDisplay(Player player) {
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof MessageBoardBlockEntity || be instanceof DigitalSignControllerBlockEntity ? be : null;
    }
    @Override public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getCenter()) <= 64 && getDisplay(player) != null;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
