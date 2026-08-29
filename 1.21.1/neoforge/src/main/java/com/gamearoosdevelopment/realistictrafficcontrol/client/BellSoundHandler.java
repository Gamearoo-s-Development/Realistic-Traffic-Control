package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BellBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/**
 * Client-only helper that spins up a {@link LoopableBlockEntitySound} for a ringing bell. Isolated in the
 * client package so the server never touches {@link Minecraft}; only invoked from the bell's client ticker.
 */
public final class BellSoundHandler {

    public static void play(BellBlockEntity be) {
        Holder<SoundEvent> sound = be.getSound();
        if (sound == null) {
            return;
        }
        LoopableBlockEntitySound instance =
                new LoopableBlockEntitySound(sound.value(), be, be.getBlockPos(), 1.0F, 1.0F);
        Minecraft.getInstance().getSoundManager().play(instance);
    }

    private BellSoundHandler() {
    }
}
