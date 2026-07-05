package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code TileEntityWireAnchor}: up to four wire connections per anchor. */
public class WireAnchorBlockEntity extends BlockEntity {
    public final BlockPos[] connections = new BlockPos[4];

    public WireAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRE_ANCHOR.get(), pos, state);
    }

    public boolean addConnection(BlockPos target) {
        if (target == null || target.equals(worldPosition)) {
            return false;
        }
        for (BlockPos existing : connections) {
            if (target.equals(existing)) {
                return false;
            }
        }
        for (int i = 0; i < connections.length; i++) {
            if (connections[i] == null) {
                connections[i] = target.immutable();
                setChanged();
                notifyUpdate();
                return true;
            }
        }
        return false;
    }

    public boolean removeConnection(BlockPos target) {
        if (target == null) {
            return false;
        }
        for (int i = 0; i < connections.length; i++) {
            if (target.equals(connections[i])) {
                connections[i] = null;
                setChanged();
                notifyUpdate();
                return true;
            }
        }
        return false;
    }

    private void notifyUpdate() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < connections.length; i++) {
            if (connections[i] != null) {
                tag.putLong("ConnectedTo" + i, connections[i].asLong());
            }
        }
        if (connections[0] != null) {
            tag.putLong("ConnectedTo", connections[0].asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ConnectedTo")) {
            connections[0] = BlockPos.of(tag.getLong("ConnectedTo"));
        }
        for (int i = 0; i < connections.length; i++) {
            String key = "ConnectedTo" + i;
            if (tag.contains(key)) {
                connections[i] = BlockPos.of(tag.getLong(key));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
