package com.gamearoosdevelopment.realistictrafficcontrol.compat.ponder;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only Create Ponder hook. Loaded only when Create is present.
 */
@OnlyIn(Dist.CLIENT)
public final class RTCPonderClient {

    private static boolean registered;

    private RTCPonderClient() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PonderIndex.addPlugin(new RTCPonderPlugin());
    }

    public static void reload() {
        register();
        PonderIndex.reload();
    }
}
