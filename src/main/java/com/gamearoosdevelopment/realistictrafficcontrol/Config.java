package com.gamearoosdevelopment.realistictrafficcontrol;

import org.apache.logging.log4j.Level;

import com.gamearoosdevelopment.realistictrafficcontrol.proxy.CommonProxy;


import net.minecraftforge.common.config.Configuration;


public class Config {
	
	
	private static final String CATEGORY_GENERAL = "general";
	private static final String CATEGORY_TRAFFIC_LIGHT = "traffic_light";
	
	public static int islandTimeout = 20;
	public static int borderTimeout = 150;
	public static int borderTick = 10; 
	public static int parallelScans = 1;
	public static int tooltipCharWrapLength = 256;
	public static String[] sensorClasses = new String[] 
			{
				"minecrafttransportsimulator.vehicles.main.EntityVehicleD_Moving",
				"com.mrcrayfish.vehicle.entity.EntityVehicle",
				"com.flansmod.common.driveables.EntityDriveable",
				"net.fexcraft.mod.fvtm.sys.legacy.LandVehicle",
				"net.fexcraft.mod.fvtm.sys.uni12.ULandVehicle",
				"net.minecraft.entity.passive.EntityPig",
				"net.minecraft.entity.passive.EntityHorse",
				"net.minecraft.entity.passive.EntityDonkey",
				"net.minecraft.entity.passive.EntityMule",
				"net.minecraft.entity.passive.EntitySkeletonHorse",
				"net.minecraft.entity.passive.EntityZombieHorse",
				"de.maxhenkel.car.entity.car.base.EntityVehicleBase"
			};
	public static int sensorScanHeight = 5;
	public static int trafficLightCardT1Capacity = 20;
	public static int trafficLightCardT2Capacity = 144;
	public static int trafficLightCardT3Capacity = 384;
	public static float trafficLightCardDrawPerBlock = 0.01F;
	
	public static void readConfig()
	{
		Configuration cfg = CommonProxy.config;
		try
		{
			cfg.load();
			initGeneralConfig(cfg);
		}
		catch(Exception e)
		{
			ModRealisticTrafficControl.logger.log(Level.ERROR, "Problem loading config file!", e);
		}
		finally
		{
			if (cfg.hasChanged())
			{
				cfg.save();
			}
		}
	}
	
	private static void initGeneralConfig(Configuration cfg)
	{
		cfg.addCustomCategoryComment(CATEGORY_GENERAL, "General configuration");
		
		sensorClasses = cfg.getStringList("sensorClasses", CATEGORY_TRAFFIC_LIGHT, sensorClasses, "What entity classes will activate the traffic signal sensors?");
		sensorScanHeight = cfg.getInt("sensorScanHeight", CATEGORY_TRAFFIC_LIGHT, sensorScanHeight, 0, 10, "How far up (in blocks) should traffic signal sensors scan for entities? [Min = 0, Max = 10, Default = 5]");
		
		tooltipCharWrapLength = cfg.getInt("tooltipCharWrapLength", CATEGORY_GENERAL, tooltipCharWrapLength, 64, 5412, "How many letters should be rendered in a tooltip before it wraps down to the next line?");
		
		trafficLightCardDrawPerBlock = cfg.getFloat("trafficLightCardDrawPerBlock", CATEGORY_TRAFFIC_LIGHT, trafficLightCardDrawPerBlock, 0, Float.MAX_VALUE, "How much energy should the traffic control card consume times number of blocks away the traffic light is?");
	}
	
	
	
	
	
	
}


