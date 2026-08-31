package com.gamearoosdevelopment.realistictrafficcontrol.oc;

import li.cil.oc.api.Driver;

/** OpenComputers-only registration entry point. Never load this class unless OpenComputers is present. */
public final class OpenComputersIntegration {
    private OpenComputersIntegration() {
    }

    public static void register() {
        Driver.add(new TrafficLightCardDriver());
    }
}
