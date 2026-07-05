package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code StreetSignTileEntity}. */
public class StreetSignBlockEntity extends SyncableBlockEntity {

    public static final int MAX_STREET_SIGNS = 4;
    private final StreetSign[] streetSigns = new StreetSign[MAX_STREET_SIGNS];

    public StreetSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STREET_SIGN.get(), pos, state);
    }

    public boolean addStreetSign(StreetSign sign) {
        for (int i = 0; i < streetSigns.length; i++) {
            if (streetSigns[i] == null) {
                streetSigns[i] = sign;
                setChanged();
                notifyBlockUpdate();
                return true;
            }
        }
        return false;
    }

    public StreetSign getStreetSign(int index) {
        return streetSigns[index];
    }

    public int getOccupiedCount() {
        int count = 0;
        for (StreetSign sign : streetSigns) {
            if (sign != null) {
                count++;
            }
        }
        return count;
    }

    private void notifyBlockUpdate() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private void readSigns(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < MAX_STREET_SIGNS; i++) {
            streetSigns[i] = null;
            String key = "street_sign" + i;
            if (tag.contains(key)) {
                StreetSign sign = new StreetSign();
                sign.load(tag.getCompound(key), registries);
                streetSigns[i] = sign;
            }
        }
    }

    private void writeSigns(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < streetSigns.length; i++) {
            if (streetSigns[i] != null) {
                tag.put("street_sign" + i, streetSigns[i].save(registries));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeSigns(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readSigns(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readSigns(tag, registries);
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        writeSigns(tag, provider);
        return tag;
    }

    @Override
    public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readSigns(tag, provider);
        setChanged();
        notifyBlockUpdate();
    }

}
