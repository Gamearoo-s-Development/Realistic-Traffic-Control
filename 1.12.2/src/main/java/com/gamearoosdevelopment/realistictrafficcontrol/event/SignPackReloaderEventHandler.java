package com.gamearoosdevelopment.realistictrafficcontrol.event;

import org.lwjgl.input.Keyboard;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import com.gamearoosdevelopment.realistictrafficcontrol.proxy.ClientProxy;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(Side.CLIENT)
public class SignPackReloaderEventHandler {
	@SubscribeEvent
	public static void onKeyPress(RenderTickEvent e)
	{
		if (e.phase == Phase.END && Keyboard.isKeyDown(Keyboard.KEY_F3) && Keyboard.isKeyDown(ClientProxy.hotReloadSignPacksKey.getKeyCode()))
		
		{
			try
			{
				ModRealisticTrafficControl.instance.signRepo.reload();
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[" + ModRealisticTrafficControl.MODNAME + "] Sign Packs Reloaded!"));
				Minecraft.getMinecraft().player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
			}
			catch(Exception exception)
			{
				Minecraft.getMinecraft().player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.4F);
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString(TextFormatting.RED + "Something went wrong! Check the console for details."));
				ModRealisticTrafficControl.logger.error(exception);
			}
		}
	}
}
