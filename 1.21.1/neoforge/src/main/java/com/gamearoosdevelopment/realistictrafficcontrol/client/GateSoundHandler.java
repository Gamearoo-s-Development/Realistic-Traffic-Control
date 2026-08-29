package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModSounds;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingGateGateBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client-only looping gate sound while the arm is moving. */
public final class GateSoundHandler {

    public static void play(CrossingGateGateBlockEntity be) {
        LoopableBlockEntitySound instance =
                new LoopableBlockEntitySound(ModSounds.GATE.get(), be, be.getBlockPos(), 0.3F, 1.0F);
        Minecraft.getInstance().getSoundManager().play(instance);
    }

    private GateSoundHandler() {
    }
}
