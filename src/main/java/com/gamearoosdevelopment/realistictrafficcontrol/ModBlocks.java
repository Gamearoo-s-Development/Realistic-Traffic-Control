package com.gamearoosdevelopment.realistictrafficcontrol;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.*;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ObjectHolder("realistictrafficcontrol")
public class ModBlocks {
	@ObjectHolder("crossing_gate_base")
	public static BlockCrossingGateBase crossing_gate_base;
	
	@ObjectHolder("stand")
	public static BlockStand stand;
	
	@ObjectHolder("generator")
	public static BlockGenerator generator;
	
	
	@ObjectHolder("crossing_gate_pole")
	public static BlockCrossingGatePole crossing_gate_pole;
	
	@ObjectHolder("sign")
	public static BlockSign sign;
	
	
	
	
	@ObjectHolder("cone")
	public static BlockCone cone;
	@ObjectHolder("channelizer")
	public static BlockChannelizer channelizer;
	@ObjectHolder("drum")
	public static BlockDrum drum;
	@ObjectHolder("street_light_single")
	public static BlockStreetLightSingle street_light_single;
	@ObjectHolder("light_source")
	public static BlockLightSource light_source;
	@ObjectHolder("street_light_double")
	public static BlockStreetLightDouble street_light_double;
	@ObjectHolder("traffic_light")
	public static BlockTrafficLight traffic_light;
	@ObjectHolder("traffic_light_control_box")
	public static BlockTrafficLightControlBox traffic_light_control_box;
	
	@ObjectHolder("type_3_barrier")
	public static BlockType3Barrier type_3_barrier;
	@ObjectHolder("type_3_barrier_right")
	public static BlockType3BarrierRight type_3_barrier_right;

	@ObjectHolder("concrete_barrier")
	public static BlockConcreteBarrier concrete_barrier;
	@ObjectHolder("horizontal_pole")
	public static BlockHorizontalPole horizontal_pole;
	
	@ObjectHolder("traffic_sensor_left")
	public static BlockTrafficSensorLeft traffic_sensor_left;
	@ObjectHolder("traffic_sensor_straight")
	public static BlockTrafficSensorStraight traffic_sensor_straight;
	@ObjectHolder("street_sign")
	public static BlockStreetSign street_sign;
	@ObjectHolder("traffic_light_5")
	public static BlockTrafficLight5 traffic_light_5;
	@ObjectHolder("traffic_light_5_upper")
	public static BlockTrafficLight5Upper traffic_light_5_upper;
	@ObjectHolder("traffic_light_doghouse")
	public static BlockTrafficLightDoghouse traffic_light_doghouse;
	@ObjectHolder("traffic_light_1")
	public static BlockTrafficLight1 traffic_light_1;
	@ObjectHolder("traffic_light_2")
	public static BlockTrafficLight2 traffic_light_2;
	@ObjectHolder("traffic_light_4")
	public static BlockTrafficLight4 traffic_light_4;
	@ObjectHolder("traffic_light_6")
	public static BlockTrafficLight6 traffic_light_6;
	@ObjectHolder("traffic_light_7")
	public static BlockTrafficLight7 traffic_light_7;
	
	@ObjectHolder("pedestrian_button")
	public static BlockPedestrianButton pedestrian_button;
	@ObjectHolder("traffic_sensor_right")
	public static BlockTrafficSensorRight traffic_sensor_right;
	

	@SideOnly(Side.CLIENT)
	public static void initModels(ModelRegistryEvent e)
	{
		crossing_gate_base.initModel();
		crossing_gate_pole.initModel();
		stand.initModel();
		generator.initModel();
		
		
		
		
		sign.initModel();
		
		cone.initModel();
		channelizer.initModel();
		drum.initModel();
		if(!ModRealisticTrafficControl.TC_INSTALLED)
		{
		street_light_single.initModel();
		street_light_double.initModel();
		}
		traffic_light.initModel();
		traffic_light_control_box.initModel();
		if(!ModRealisticTrafficControl.TC_INSTALLED)
		{
		type_3_barrier.initModel();
		type_3_barrier_right.initModel();
		
		concrete_barrier.initModel();
		}
		horizontal_pole.initModel();
		
		traffic_sensor_left.initModel();
		traffic_sensor_straight.initModel();
		if(!ModRealisticTrafficControl.TC_INSTALLED)
		{
		street_sign.initModel();
		}
		traffic_light_5.initModel();
		traffic_light_doghouse.initModel();
		traffic_light_1.initModel();
		traffic_light_2.initModel();
		traffic_light_4.initModel();
		pedestrian_button.initModel();
		traffic_sensor_right.initModel();
		traffic_light_6.initModel();
		traffic_light_7.initModel();
		
	}
}
