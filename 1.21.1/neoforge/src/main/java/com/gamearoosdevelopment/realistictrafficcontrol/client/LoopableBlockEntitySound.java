package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.util.ILoopableSoundTileEntity;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-side looping positioned sound tied to a block entity's lifetime. Port of the 1.12.2
 * {@code LoopableTileEntitySound} ({@code MovingSound}/{@code ITickableSound} -&gt;
 * {@link AbstractTickableSoundInstance}). Stops itself once the block entity is removed or reports it is
 * done playing.
 */
public class LoopableBlockEntitySound extends AbstractTickableSoundInstance {

    private final ILoopableSoundTileEntity tileEntity;

    public LoopableBlockEntitySound(SoundEvent sound, ILoopableSoundTileEntity tileEntity, BlockPos pos,
            float volume, float pitch) {
        super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.tileEntity = tileEntity;
        this.looping = true;
        this.delay = 0;
        this.volume = volume;
        this.pitch = pitch;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        if (tileEntity instanceof BlockEntity be && be.isRemoved()) {
            stop();
            return;
        }
        if (tileEntity.isDonePlayingSound()) {
            stop();
        }
    }
}
