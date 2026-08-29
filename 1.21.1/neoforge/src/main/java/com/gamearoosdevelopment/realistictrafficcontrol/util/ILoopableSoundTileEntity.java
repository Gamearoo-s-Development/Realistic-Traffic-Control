package com.gamearoosdevelopment.realistictrafficcontrol.util;

/**
 * Implemented by block entities that drive a looping positioned sound (bells / horns). Ported verbatim
 * from the 1.12.2 interface of the same name; the client sound instance polls
 * {@link #isDonePlayingSound()} to know when to stop.
 */
public interface ILoopableSoundTileEntity {
    boolean isDonePlayingSound();
}
