package com.gamearoosdevelopment.realistictrafficcontrol.menu;

import com.gamearoosdevelopment.realistictrafficcontrol.ModMenus;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockLampBase;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingLampsBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Menu for the crossing-lamp bulb configuration GUI. */
public class CrossingLampsMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public CrossingLampsMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory, buf.readBlockPos());
    }

    public CrossingLampsMenu(int id, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenus.CROSSING_LAMPS.get(), id);
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public CrossingLampsBlockEntity getLamps(Player player) {
        BlockEntity be = player.level().getBlockEntity(blockPos);
        return be instanceof CrossingLampsBlockEntity lamps ? lamps : null;
    }

    public String getModelPrefix(Player player) {
        BlockState state = player.level().getBlockState(blockPos);
        if (state.getBlock() instanceof BlockLampBase lamp) {
            return lamp.getLampRegistryName();
        }
        return "crossing_gate_lamps";
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0
                && player.level().getBlockEntity(blockPos) instanceof CrossingLampsBlockEntity;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
