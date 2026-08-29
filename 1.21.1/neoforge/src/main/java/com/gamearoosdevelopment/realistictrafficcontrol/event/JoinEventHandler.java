package com.gamearoosdevelopment.realistictrafficcontrol.event;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.network.SignPackCheckPayload;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;

/** Port of 1.12.2 {@code JoinEventHandler}: sends signpack list to joining clients. */
@EventBusSubscriber(modid = ModRealisticTrafficControl.MODID)
public final class JoinEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player,
                    new SignPackCheckPayload(new HashMap<>(ModRealisticTrafficControl.signRepo.getPacksByID())));
        }
    }

    private JoinEventHandler() {
    }
}
