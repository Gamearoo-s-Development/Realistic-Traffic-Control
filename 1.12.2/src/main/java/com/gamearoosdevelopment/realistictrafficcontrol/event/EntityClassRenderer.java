package com.gamearoosdevelopment.realistictrafficcontrol.event;

import java.util.ArrayList;

import com.gamearoosdevelopment.realistictrafficcontrol.proxy.ClientProxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(Side.CLIENT)
public class EntityClassRenderer {
	public static boolean performEntityClassRender = false;
	@SubscribeEvent
	public static void renderEntity(RenderGameOverlayEvent.Post e)
	{
		if (!performEntityClassRender || Minecraft.getMinecraft().objectMouseOver.typeOfHit != RayTraceResult.Type.ENTITY)
		{
			return;
		}
		
		Entity entityHit = Minecraft.getMinecraft().objectMouseOver.entityHit;
		if (entityHit == null)
		{
			return;
		}
		
		ArrayList<String> classes = new ArrayList<String>();
		Class<?> nextClass = entityHit.getClass();
		
		while(nextClass != null)
		{
			classes.add(nextClass.getName());
			nextClass = nextClass.getSuperclass();
		}
		
		FontRenderer renderer = Minecraft.getMinecraft().fontRenderer;
		for(int i = 0; i < classes.size(); i++)
		{
			int width = renderer.getStringWidth(classes.get(i));
			renderer.drawString(classes.get(i), (e.getResolution().getScaledWidth() / 2) - (width / 2), renderer.FONT_HEIGHT * i + (2 * i), 0xFFFFFF);
		}
	}
	
	@SubscribeEvent
	public static void keyPress(KeyInputEvent e)
	{
		if (ClientProxy.entityClassRendererKey.isPressed())
		{
			performEntityClassRender = !performEntityClassRender;
		}
	}
}
