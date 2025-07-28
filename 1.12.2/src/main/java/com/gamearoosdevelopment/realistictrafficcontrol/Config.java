package com.gamearoosdevelopment.realistictrafficcontrol;

import org.apache.logging.log4j.Level;

import com.gamearoosdevelopment.realistictrafficcontrol.proxy.CommonProxy;


import net.minecraftforge.common.config.Configuration;


public class Config {
	
	public static Configuration config;
	private static final String CATEGORY_GENERAL = "general";
	private static final String CATEGORY_TRAFFIC_LIGHT = "traffic_lights";
	
	private static final String CATEGORY_OC = "open_computers";
	private static final String CATEGORY_CC = "computer_craft";
	
	
	
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
	public static float ccPeripheralEnergyCost = 0.01F;
	
	
	 public static void readConfig() {
	        Configuration cfg = CommonProxy.config;
	        config = cfg;

	        try {
	            cfg.load();
	            initGeneralConfig(cfg);
	        } catch (Exception e) {
	            ModRealisticTrafficControl.logger.log(Level.ERROR, "Problem loading config file!", e);
	        } finally {
	            if (cfg.hasChanged()) {
	                cfg.save();
	            }
	        }

	        // GUI display name mapping
	        cfg.getCategory(CATEGORY_TRAFFIC_LIGHT).setLanguageKey("config.traffic_lights.name");
	        cfg.getCategory(CATEGORY_GENERAL).setLanguageKey("config.general.name");
	        
	        cfg.getCategory(CATEGORY_OC).setLanguageKey("config.open_computers.name");
	        cfg.getCategory(CATEGORY_CC).setLanguageKey("config.computer_craft.name");
	    }

	
	private static void initGeneralConfig(Configuration cfg)
	{
		cfg.addCustomCategoryComment(CATEGORY_GENERAL, "General configuration");
		cfg.addCustomCategoryComment(CATEGORY_TRAFFIC_LIGHT, "Stuff Related To Traffic Lights");
		
		cfg.addCustomCategoryComment(CATEGORY_OC, "Open Computer Stuff");
		
		
		
		
		
		
		sensorClasses = cfg.getStringList("sensorClasses", CATEGORY_TRAFFIC_LIGHT, sensorClasses, "What entity classes will activate the traffic signal sensors?");
		sensorScanHeight = cfg.getInt("sensorScanHeight", CATEGORY_TRAFFIC_LIGHT, sensorScanHeight, 0, 10, "How far up (in blocks) should traffic signal sensors scan for entities? [Min = 0, Max = 10, Default = 5]");
		
		tooltipCharWrapLength = cfg.getInt("tooltipCharWrapLength", CATEGORY_GENERAL, tooltipCharWrapLength, 64, 5412, "How many letters should be rendered in a tooltip before it wraps down to the next line?");
		
		trafficLightCardDrawPerBlock = cfg.getFloat("trafficLightCardDrawPerBlock", CATEGORY_OC, trafficLightCardDrawPerBlock, 0, Float.MAX_VALUE, "How much energy should the traffic control card consume times number of blocks away the traffic light is?");
	
		
	
		
		ccPeripheralEnergyCost = cfg.getFloat("ccPeripheralEnergyCost", CATEGORY_CC, ccPeripheralEnergyCost, 0F, Float.MAX_VALUE,
			    "How much energy (simulated) does a CC peripheral method call consume?");

		

		
		trafficLightCardT1Capacity = cfg.getInt("trafficLightCardT1Capacity", CATEGORY_OC, trafficLightCardT1Capacity, 1, Integer.MAX_VALUE, "How many traffic lights should be allowed to be paired with a Tier 1 Traffic Light Card?");
		trafficLightCardT2Capacity = cfg.getInt("trafficLightCardT2Capacity", CATEGORY_OC, trafficLightCardT2Capacity, 1, Integer.MAX_VALUE, "How many traffic lights should be allowed to be paired with a Tier 2 Traffic Light Card?");
		trafficLightCardT3Capacity = cfg.getInt("trafficLightCardT3Capacity", CATEGORY_OC, trafficLightCardT3Capacity, 1, Integer.MAX_VALUE, "How many traffic lights should be allowed to be paired with a Tier 3 Traffic Light Card?");
		trafficLightCardDrawPerBlock = cfg.getFloat("trafficLightCardDrawPerBlock", CATEGORY_OC, trafficLightCardDrawPerBlock, 0, Float.MAX_VALUE, "How much energy should the traffic control card consume times number of blocks away the traffic light is?");
	}
	
	
	
	
	
	
}


