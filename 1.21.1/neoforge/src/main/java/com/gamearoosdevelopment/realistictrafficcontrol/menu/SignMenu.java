package com.gamearoosdevelopment.realistictrafficcontrol.menu;

import com.gamearoosdevelopment.realistictrafficcontrol.ModMenus;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Menu holder for the sign configuration screen. */
public class SignMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public SignMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory, buf.readBlockPos());
    }

    public SignMenu(int id, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenus.SIGN.get(), id);
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public SignBlockEntity getSign(Player player) {
        BlockEntity be = player.level().getBlockEntity(blockPos);
        return be instanceof SignBlockEntity sign ? sign : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0
                && player.level().getBlockEntity(blockPos) instanceof SignBlockEntity;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
