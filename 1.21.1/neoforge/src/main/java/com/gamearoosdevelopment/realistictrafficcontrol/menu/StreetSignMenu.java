package com.gamearoosdevelopment.realistictrafficcontrol.menu;

import com.gamearoosdevelopment.realistictrafficcontrol.ModMenus;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSignBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Menu holder for the street-sign configuration screen. */
public class StreetSignMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public StreetSignMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory, buf.readBlockPos());
    }

    public StreetSignMenu(int id, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenus.STREET_SIGN.get(), id);
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public StreetSignBlockEntity getStreetSignEntity(Player player) {
        BlockEntity be = player.level().getBlockEntity(blockPos);
        return be instanceof StreetSignBlockEntity streetSign ? streetSign : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0
                && player.level().getBlockEntity(blockPos) instanceof StreetSignBlockEntity;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
