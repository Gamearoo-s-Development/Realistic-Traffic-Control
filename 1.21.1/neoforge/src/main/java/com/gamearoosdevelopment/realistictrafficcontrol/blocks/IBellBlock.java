package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/**
 * Implemented by every crossing bell / horn block so the shared bell block entity can discover which
 * looping sound to play without a distinct block-entity class per bell type.
 */
public interface IBellBlock {
    Holder<SoundEvent> getSound();
}
