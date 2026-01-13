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
        INSTANCE.registerMessage(
                PacketToggleMain.Handler.class,
                PacketToggleMain.class,
                
                packetId++,
                Side.SERVER
            );
		INSTANCE.registerMessage(
				PacketToggleHawkBeacon.Handler.class,
				PacketToggleHawkBeacon.class,
				packetId++,
				Side.SERVER
		);
        INSTANCE.registerMessage(
                PacketToggleSplitDirections.Handler.class,
                PacketToggleSplitDirections.class,
                packetId++,
                Side.SERVER
        );
		INSTANCE.registerMessage(
				PacketToggleSplitAxis.Handler.class,
				PacketToggleSplitAxis.class,
				packetId++,
				Side.SERVER
		);
    }
}
