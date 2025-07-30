package com.gamearoosdevelopment.realistictrafficcontrol;

import com.gamearoosdevelopment.realistictrafficcontrol.item.*;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

@ObjectHolder("realistictrafficcontrol")
public class ModItems {
	
	@ObjectHolder("crossing_relay_tuner")
	public static ItemCrossingRelayTuner crossing_relay_tuner;
	@ObjectHolder("cover_hook")
	public static ItemCoverHook cover_hook;
	@ObjectHolder("traffic_light_bulb")
	public static ItemTrafficLightBulb traffic_light_bulb;
	@ObjectHolder("traffic_light_frame")
	public static ItemTrafficLightFrame traffic_light_frame;
	@ObjectHolder("traffic_light_hoz_frame")
	public static ItemTrafficLightHozFrame traffic_light_hoz_frame;
	@ObjectHolder("street_sign")
	public static ItemStreetSign street_sign;
	@ObjectHolder("traffic_light_5_frame")
	public static ItemTrafficLight5Frame traffic_light_5_frame;
	@ObjectHolder("traffic_light_5_hoz_frame")
	public static ItemTrafficLight5HozFrame traffic_light_5_hoz_frame;
	@ObjectHolder("traffic_light_doghouse_frame")
	public static ItemTrafficLightDoghouseFrame traffic_light_doghouse_frame;
	@ObjectHolder("traffic_light_1_frame")
	public static ItemTrafficLight1Frame traffic_light_1_frame;
	@ObjectHolder("traffic_light_2_frame")
	public static ItemTrafficLight2Frame traffic_light_2_frame;
	@ObjectHolder("traffic_light_2_hoz_frame")
	public static ItemTrafficLight2HozFrame traffic_light_2_hoz_frame;
	
	@ObjectHolder("traffic_light_4_frame")
	public static ItemTrafficLight4Frame traffic_light_4_frame;
	@ObjectHolder("traffic_light_4_hoz_frame")
	public static ItemTrafficLight4HozFrame traffic_light_4_hoz_frame;
	@ObjectHolder("traffic_light_6_frame")
	public static BaseItemTrafficLightFrame traffic_light_6_frame;
	@ObjectHolder("traffic_light_8_frame")
	public static BaseItemTrafficLightFrame traffic_light_8_frame;
	@ObjectHolder("traffic_light_7_frame")
	public static BaseItemTrafficLightFrame traffic_light_7_frame;
	@ObjectHolder("traffic_light_card")
	public static ItemTrafficLightCard traffic_light_card;
	
	@ObjectHolder("screwdriver")
	public static ItemScrewdriver screwdriver;

	public static void initModels(ModelRegistryEvent e)
	{
		
		crossing_relay_tuner.initModel();
		cover_hook.initModel();
		traffic_light_bulb.initModel();
		traffic_light_frame.initModel();
		traffic_light_hoz_frame.initModel();
		if(!ModRealisticTrafficControl.TC_INSTALLED)
		{
		street_sign.initModel();
		}
		traffic_light_5_frame.initModel();
		traffic_light_5_hoz_frame.initModel();
		traffic_light_doghouse_frame.initModel();
		traffic_light_1_frame.initModel();
		traffic_light_2_frame.initModel();
		traffic_light_2_hoz_frame.initModel();
		traffic_light_4_frame.initModel();
		traffic_light_4_hoz_frame.initModel();
		traffic_light_6_frame.initModel();
		traffic_light_8_frame.initModel();
		traffic_light_7_frame.initModel();
		screwdriver.initModel();
		if (ModRealisticTrafficControl.OC_INSTALLED)
		{
			traffic_light_card.initModel();
		}
	}
}
