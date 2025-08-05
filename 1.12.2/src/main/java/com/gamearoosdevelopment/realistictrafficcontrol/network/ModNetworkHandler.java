package com.gamearoosdevelopment.realistictrafficcontrol.network;

import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraft.util.ResourceLocation;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

public class ModNetworkHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("rtc");

    private static int packetId = 0;

    public static void registerPackets() {
        INSTANCE.registerMessage(
            PacketToggleNightFlash.Handler.class,
            PacketToggleNightFlash.class,
            packetId++,
            Side.SERVER
        );
    }
}
