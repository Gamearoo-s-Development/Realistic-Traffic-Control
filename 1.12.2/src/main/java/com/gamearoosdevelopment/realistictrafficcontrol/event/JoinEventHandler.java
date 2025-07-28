package com.gamearoosdevelopment.realistictrafficcontrol.event;

import java.util.HashMap;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketHandler;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketSignPackCheck;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

@EventBusSubscriber
public class JoinEventHandler {
	@SubscribeEvent
	public static void join(PlayerLoggedInEvent e)
	{
		PacketSignPackCheck packCheck = new PacketSignPackCheck();
		packCheck.signPacks = new HashMap<>(ModRealisticTrafficControl.instance.signRepo.getPacksByID());
		PacketHandler.INSTANCE.sendTo(packCheck, (EntityPlayerMP)e.player);
	}
}
