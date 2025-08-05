package com.gamearoosdevelopment.realistictrafficcontrol.util;

import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

@Mod.EventBusSubscriber(modid = "realistictrafficcontrol")
public class ConfigReload {
	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if(event.getModID().equals(ModRealisticTrafficControl.MODID)) {
			Config.config.save();
			
		}
	}
	
}
