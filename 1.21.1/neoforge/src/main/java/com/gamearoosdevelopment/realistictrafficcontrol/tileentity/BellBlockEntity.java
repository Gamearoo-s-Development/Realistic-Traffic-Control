package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.IBellBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ILoopableSoundTileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared block entity for every crossing bell / horn. Ported from the 1.12.2 abstract
 * {@code BellBaseTileEntity} (one subclass per sound); here the {@link Holder}&lt;{@link SoundEvent}&gt;
 * is read from the {@link IBellBlock}, so one class + one {@code BlockEntityType} covers all bell types.
 *
 * <p>The looping client sound is started/stopped from {@link #clientTick} via the client-only
 * {@code BellSoundHandler}, mirroring the old {@code handleUpdateTag} sound handshake.
 */
public class BellBlockEntity extends BlockEntity implements ILoopableSoundTileEntity {

    private boolean isRinging = false;
    private boolean soundPlaying = false;

    public BellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BELL.get(), pos, state);
    }

    public Holder<SoundEvent> getSound() {
        if (getBlockState().getBlock() instanceof IBellBlock bell) {
            return bell.getSound();
        }
        return null;
    }

    public boolean getIsRinging() {
        return isRinging;
    }

    public boolean isAffectedByRelayBellStopTimer() {
        return true;
    }

    public void setIsRinging(boolean ringing) {
        if (this.isRinging == ringing) {
            return;
        }
        this.isRinging = ringing;
        setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public boolean isDonePlayingSound() {
        return !soundPlaying;
    }

    /** Client ticker: starts the looping sound when ringing begins and lets it stop when it ends. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, BellBlockEntity be) {
        if (be.isRinging && !be.soundPlaying) {
            com.gamearoosdevelopment.realistictrafficcontrol.client.BellSoundHandler.play(be);
            be.soundPlaying = true;
        } else if (!be.isRinging) {
            be.soundPlaying = false;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isringing", isRinging);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isRinging = tag.getBoolean("isringing");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("isringing", isRinging);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        isRinging = tag.getBoolean("isringing");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        soundPlaying = false;
    }
}
