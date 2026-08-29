package com.gamearoosdevelopment.realistictrafficcontrol.menu;

import com.gamearoosdevelopment.realistictrafficcontrol.ModMenus;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.Type3BarrierBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Menu holder for the type-3 barrier configuration screen. */
public class Type3BarrierMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public Type3BarrierMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory, buf.readBlockPos());
    }

    public Type3BarrierMenu(int id, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenus.TYPE_3_BARRIER.get(), id);
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public Type3BarrierBlockEntity getBarrier(Player player) {
        BlockEntity be = player.level().getBlockEntity(blockPos);
        return be instanceof Type3BarrierBlockEntity barrier ? barrier : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0
                && player.level().getBlockEntity(blockPos) instanceof Type3BarrierBlockEntity;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
