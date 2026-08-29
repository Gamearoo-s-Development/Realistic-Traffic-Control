package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockLampBase;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.crossing.ICrossingLampBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code CrossingLampsTileEntity}. */
public class CrossingLampsBlockEntity extends SyncableBlockEntity implements ICrossingLampBlockEntity {

    private CrossingLampState state = CrossingLampState.Off;
    private int nwBulbRotation = 0;
    private int neBulbRotation = 0;
    private int swBulbRotation = 0;
    private int seBulbRotation = 0;

    public CrossingLampsBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CROSSING_LAMPS.get(), pos, blockState);
    }

    public CrossingLampState getState() {
        return state;
    }

    @Override
    public void setState(CrossingLampState state) {
        if (this.state == state) {
            return;
        }
        this.state = state;
        setChanged();
        if (level != null) {
            BlockState newState = getBlockState().setValue(RTCProperties.LAMP_STATE, state);
            level.setBlockAndUpdate(worldPosition, newState);
        }
    }

    public int getNwBulbRotation() {
        return nwBulbRotation;
    }

    public void setNwBulbRotation(int nwBulbRotation) {
        this.nwBulbRotation = nwBulbRotation;
        setChanged();
    }

    public int getNeBulbRotation() {
        return neBulbRotation;
    }

    public void setNeBulbRotation(int neBulbRotation) {
        this.neBulbRotation = neBulbRotation;
        setChanged();
    }

    public int getSwBulbRotation() {
        return swBulbRotation;
    }

    public void setSwBulbRotation(int swBulbRotation) {
        this.swBulbRotation = swBulbRotation;
        setChanged();
    }

    public int getSeBulbRotation() {
        return seBulbRotation;
    }

    public void setSeBulbRotation(int seBulbRotation) {
        this.seBulbRotation = seBulbRotation;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("state", state.getId());
        tag.putInt("nw_bulb_rotation", nwBulbRotation);
        tag.putInt("ne_bulb_rotation", neBulbRotation);
        tag.putInt("sw_bulb_rotation", swBulbRotation);
        tag.putInt("se_bulb_rotation", seBulbRotation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state = CrossingLampState.byId(tag.getInt("state"));
        nwBulbRotation = tag.getInt("nw_bulb_rotation");
        neBulbRotation = tag.getInt("ne_bulb_rotation");
        swBulbRotation = tag.getInt("sw_bulb_rotation");
        seBulbRotation = tag.getInt("se_bulb_rotation");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
        if (level != null) {
            BlockState blockState = getBlockState();
            level.sendBlockUpdated(worldPosition, blockState, blockState, 3);
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    @Override
    public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("neRot", neBulbRotation);
        tag.putInt("nwRot", nwBulbRotation);
        tag.putInt("swRot", swBulbRotation);
        tag.putInt("seRot", seBulbRotation);
        return tag;
    }

    @Override
    public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        setNeBulbRotation(tag.getInt("neRot"));
        setNwBulbRotation(tag.getInt("nwRot"));
        setSwBulbRotation(tag.getInt("swRot"));
        setSeBulbRotation(tag.getInt("seRot"));
        if (level != null) {
            BlockState blockState = getBlockState();
            level.sendBlockUpdated(worldPosition, blockState, blockState, 3);
        }
    }

    public static boolean isLampBlock(BlockState state) {
        return state.getBlock() instanceof BlockLampBase;
    }
}
