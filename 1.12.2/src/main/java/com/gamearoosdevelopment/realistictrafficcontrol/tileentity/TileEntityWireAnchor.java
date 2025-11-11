package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class TileEntityWireAnchor extends TileEntity {
    // Support up to 4 connections per anchor
    public BlockPos[] connections = new BlockPos[4];

    // Add a connection (returns true if added, false if already present or full)
    public boolean addConnection(BlockPos pos) {
        if (pos == null) return false;
        if (pos.equals(getPos())) return false; // avoid self-connection
        for (BlockPos p : connections) {
            if (p != null && p.equals(pos)) return false; // already connected
        }
        for (int i = 0; i < connections.length; i++) {
            if (connections[i] == null) {
                connections[i] = pos;
                markDirty();
                notifyUpdate();
                return true;
            }
        }
        return false; // no space
    }

    // Remove a specific connection
    public boolean removeConnection(BlockPos pos) {
        if (pos == null) return false;
        for (int i = 0; i < connections.length; i++) {
            if (connections[i] != null && connections[i].equals(pos)) {
                connections[i] = null;
                markDirty();
                notifyUpdate();
                return true;
            }
        }
        return false;
    }

    // Backwards-compatible setter: clears others and sets index 0
    public void setConnectedTo(BlockPos pos) {
        for (int i = 0; i < connections.length; i++) connections[i] = null;
        if (pos != null) connections[0] = pos;
        markDirty();
        notifyUpdate();
    }

    private void notifyUpdate() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 3);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        // write array entries as ConnectedTo0..3
        for (int i = 0; i < connections.length; i++) {
            if (connections[i] != null) compound.setLong("ConnectedTo" + i, connections[i].toLong());
        }
        // legacy key for compatibility
        if (connections[0] != null) compound.setLong("ConnectedTo", connections[0].toLong());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        // legacy key support
        if (compound.hasKey("ConnectedTo")) {
            connections[0] = BlockPos.fromLong(compound.getLong("ConnectedTo"));
        }
        for (int i = 0; i < connections.length; i++) {
            String key = "ConnectedTo" + i;
            if (compound.hasKey(key)) connections[i] = BlockPos.fromLong(compound.getLong(key));
            else if (connections[i] == null) connections[i] = null;
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(super.getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }


    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), 1, writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

}