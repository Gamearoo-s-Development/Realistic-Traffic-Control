package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.network.SyncableTileEntityPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Port of the 1.12.2 {@code SyncableTileEntity}: a block entity that can push a client-authored NBT
 * payload back to the server (used by GUIs that edit block-entity state, e.g. the control box). Replaces
 * the old {@code SimpleNetworkWrapper.sendToServer} with a {@link PacketDistributor} payload.
 */
public abstract class SyncableBlockEntity extends BlockEntity {

    protected SyncableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider);

    public abstract void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider);

    /** Client-side helper: serialize the current GUI state and send it to the server. */
    public void performClientToServerSync() {
        if (level == null) {
            return;
        }
        CompoundTag data = getClientToServerUpdateTag(level.registryAccess());
        PacketDistributor.sendToServer(new SyncableTileEntityPayload(worldPosition, data));
    }
}
