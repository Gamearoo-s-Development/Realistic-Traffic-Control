package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModSounds;
import com.gamearoosdevelopment.realistictrafficcontrol.crossing.IWigWagBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code VerticalWigWagTileEntity}. */
public class VerticalWigWagBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity
        implements IWigWagBlockEntity {

    private int rotation = 0;
    private AnimationMode mode = AnimationMode.SwingPositive;
    private boolean active = false;

    public VerticalWigWagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VERTICAL_WIG_WAG.get(), pos, state);
    }

    private enum AnimationMode {
        SwingNegative,
        SwingPositive
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, VerticalWigWagBlockEntity te) {
        if (state.getBlock() != ModBlocks.VERTICAL_WIG_WAG.get()) {
            return;
        }

        if (!te.isActive() && te.rotation != 0) {
            if (te.rotation < 0 && te.mode != AnimationMode.SwingPositive) {
                te.mode = AnimationMode.SwingPositive;
            }
            if (te.rotation > 0 && te.mode != AnimationMode.SwingNegative) {
                te.mode = AnimationMode.SwingNegative;
            }
        }

        if (!te.isActive() && te.rotation == 0) {
            return;
        }

        if (te.isActive()) {
            if ((te.rotation == 16 && te.mode == AnimationMode.SwingPositive)
                    || (te.rotation == -16 && te.mode == AnimationMode.SwingNegative)) {
                level.playLocalSound(pos, ModSounds.WIGWAG.get(), SoundSource.BLOCKS, 4.0F, 1.0F, false);
            }
            if (te.rotation > 30) {
                te.mode = AnimationMode.SwingNegative;
            }
            if (te.rotation < -30) {
                te.mode = AnimationMode.SwingPositive;
            }
        }

        switch (te.mode) {
            case SwingNegative -> te.rotation -= 4;
            case SwingPositive -> te.rotation += 4;
        }
    }

    public int getRotation() {
        return rotation;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState().setValue(
                    com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties.ACTIVE, active);
            level.setBlockAndUpdate(worldPosition, state);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("active", active);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        active = tag.getBoolean("active");
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
    }
}
