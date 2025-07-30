package com.gamearoosdevelopment.realistictrafficcontrol;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

public class ModSounds {
	public static SoundEvent gateEvent;
	public static SoundEvent safetranType3Event;
	public static SoundEvent safetranMechanicalEvent;
	public static SoundEvent wchEvent;
	public static SoundEvent pedButton;
	public static SoundEvent wigWag;
	public static SoundEvent wch_mechanical_bell;
	public static SoundEvent screwdriver;
	
	public static void initSounds()
	{
		
		
		gateEvent = new SoundEvent(new ResourceLocation(ModRealisticTrafficControl.MODID + ":gate")).setRegistryName("realistictrafficcontrol:gate");
		safetranType3Event = new SoundEvent(new ResourceLocation(ModRealisticTrafficControl.MODID + ":safetran_type_3")).setRegistryName("realistictrafficcontrol:safetran_type_3");
		safetranMechanicalEvent = new SoundEvent(new ResourceLocation(ModRealisticTrafficControl.MODID + ":safetran_mechanical")).setRegistryName("realistictrafficcontrol:safetran_mechanical");
		wchEvent = new SoundEvent(new ResourceLocation(ModRealisticTrafficControl.MODID + ":wch")).setRegistryName("realistictrafficcontrol:wch");
		pedButton = new SoundEvent(new ResourceLocation(ModRealisticTrafficControl.MODID + ":ped_button")).setRegistryName("realistictrafficcontrol:ped_button");
		wigWag = new SoundEvent(new ResourceLocation(ModRealisticTrafficControl.MODID + ":wigwag")).setRegistryName("realistictrafficcontrol:wigwag");
		wch_mechanical_bell = new SoundEvent(new ResourceLocation(ModRealisticTrafficControl.MODID + ":wch_mechanical_bell")).setRegistryName("realistictrafficcontrol:wch_mechanical_bell");
		screwdriver = new SoundEvent(new ResourceLocation(ModRealisticTrafficControl.MODID + ":screwdriver")).setRegistryName("realistictrafficcontrol:screwdriver");
	}
}
