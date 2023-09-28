package com.gamearoosdevelopment.realistictrafficcontrol.proxy;

import java.io.File;
import java.nio.channels.CompletionHandler;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModSounds;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockChannelizer;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockConcreteBarrier;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCone;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGateBase;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGatePole;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockDrum;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockHorizontalPole;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockLightSource;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockPedestrianButton;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetLightDouble;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetLightSingle;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight1;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight1Cover;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight2;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight2Cover;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight4;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight4Cover;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight5;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight5Cover;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight5Upper;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight6;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight6Cover;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight7;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight7Cover;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLightControlBox;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLightCover;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLightDogHouseCover;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLightDoghouse;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorLeft;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorRight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorStraight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockType3Barrier;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockType3BarrierRight;

import com.gamearoosdevelopment.realistictrafficcontrol.gui.GuiProxy;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemCone;

import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemCrossingRelayTuner;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemStreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLight1Frame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLight2Frame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLight4Frame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLight5Frame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLight6Frame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLight7Frame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLightBulb;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLightCard;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLightControlBox;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLightDoghouseFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.network.PacketHandler;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignRepository;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.ConcreteBarrierTileEntity;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.PedestrianButtonTileEntity;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetLightDoubleTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetLightSingleTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSignTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight1TileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight2TileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight4TileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight5TileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight6TileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLight7TileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightDoghouseTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.Type3BarrierTileEntity;

import com.gamearoosdevelopment.realistictrafficcontrol.util.OpenComputersHelper;


import net.minecraft.block.Block;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStand;

@EventBusSubscriber
public class CommonProxy {
	public static Configuration config;
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> e)
	{
		e.getRegistry().register(new BlockCrossingGateBase());
		e.getRegistry().register(new BlockCrossingGatePole());
		e.getRegistry().register(new BlockStand());
		e.getRegistry().register(new BlockTrafficLightCover());
		e.getRegistry().register(new BlockTrafficLight1Cover());
		e.getRegistry().register(new BlockTrafficLight2Cover());
		e.getRegistry().register(new BlockTrafficLight4Cover());
		e.getRegistry().register(new BlockTrafficLight5Cover());
		e.getRegistry().register(new BlockTrafficLight6Cover());
		e.getRegistry().register(new BlockTrafficLight7Cover());
		e.getRegistry().register(new BlockTrafficLightDogHouseCover());

		
	
		e.getRegistry().register(new BlockSign());
		e.getRegistry().register(new BlockCone());
		e.getRegistry().register(new BlockChannelizer());
		e.getRegistry().register(new BlockDrum());
		e.getRegistry().register(new BlockStreetLightSingle());
		e.getRegistry().register(new BlockLightSource());
		e.getRegistry().register(new BlockStreetLightDouble());
		e.getRegistry().register(new BlockTrafficLight());
		e.getRegistry().register(new BlockTrafficLightControlBox());
	
		e.getRegistry().register(new BlockType3Barrier());
		e.getRegistry().register(new BlockType3BarrierRight());
	
		e.getRegistry().register(new BlockConcreteBarrier());
		e.getRegistry().register(new BlockHorizontalPole());
	
		e.getRegistry().register(new BlockTrafficSensorLeft());
		e.getRegistry().register(new BlockTrafficSensorStraight());
		e.getRegistry().register(new BlockStreetSign());
		e.getRegistry().register(new BlockTrafficLight5());
		e.getRegistry().register(new BlockTrafficLight5Upper());
		e.getRegistry().register(new BlockTrafficLightDoghouse());
		e.getRegistry().register(new BlockTrafficLight1());
		e.getRegistry().register(new BlockTrafficLight2());
		e.getRegistry().register(new BlockTrafficLight4());
		e.getRegistry().register(new BlockTrafficLight6());
		e.getRegistry().register(new BlockTrafficLight7());
		e.getRegistry().register(new BlockPedestrianButton());
		e.getRegistry().register(new BlockTrafficSensorRight());

		
		
		GameRegistry.registerTileEntity(SignTileEntity.class, ModRealisticTrafficControl.MODID + "_sign");
		GameRegistry.registerTileEntity(StreetLightSingleTileEntity.class, ModRealisticTrafficControl.MODID + "_streetsignsingle");
		GameRegistry.registerTileEntity(StreetLightDoubleTileEntity.class, ModRealisticTrafficControl.MODID + "_streetlightdouble");
		GameRegistry.registerTileEntity(TrafficLightTileEntity.class, ModRealisticTrafficControl.MODID + "_trafficlight");
		GameRegistry.registerTileEntity(TrafficLightControlBoxTileEntity.class, ModRealisticTrafficControl.MODID + "_trafficlightcontrolbox");
		
		GameRegistry.registerTileEntity(Type3BarrierTileEntity.class, ModRealisticTrafficControl.MODID + "_type3barrier");
		GameRegistry.registerTileEntity(ConcreteBarrierTileEntity.class, ModRealisticTrafficControl.MODID + "_concretebarrier");
		
		GameRegistry.registerTileEntity(StreetSignTileEntity.class, ModRealisticTrafficControl.MODID + "_streetsign");
		GameRegistry.registerTileEntity(TrafficLight5TileEntity.class, ModRealisticTrafficControl.MODID + "_trafficlight5");
		GameRegistry.registerTileEntity(TrafficLightDoghouseTileEntity.class, ModRealisticTrafficControl.MODID + "_trafficlightdoghouse");
		
		GameRegistry.registerTileEntity(TrafficLight1TileEntity.class, ModRealisticTrafficControl.MODID + "_trafficlight1");
		GameRegistry.registerTileEntity(TrafficLight2TileEntity.class, ModRealisticTrafficControl.MODID + "_trafficlight2");
		GameRegistry.registerTileEntity(TrafficLight4TileEntity.class, ModRealisticTrafficControl.MODID + "_trafficlight4");
		GameRegistry.registerTileEntity(TrafficLight6TileEntity.class, ModRealisticTrafficControl.MODID + "_trafficlight6");
		GameRegistry.registerTileEntity(TrafficLight7TileEntity.class, ModRealisticTrafficControl.MODID + "_trafficlight7");
		GameRegistry.registerTileEntity(PedestrianButtonTileEntity.class, ModRealisticTrafficControl.MODID + "_pedestrianbutton");
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> e)
	{
	
		e.getRegistry().register(new ItemCrossingRelayTuner());
		e.getRegistry().register(new ItemTrafficLightBulb());
		e.getRegistry().register(new ItemTrafficLightFrame());
		e.getRegistry().register(new ItemTrafficLight5Frame());
		e.getRegistry().register(new ItemTrafficLightDoghouseFrame());
		e.getRegistry().register(new ItemTrafficLight1Frame());
		e.getRegistry().register(new ItemTrafficLight2Frame());
		e.getRegistry().register(new ItemTrafficLight4Frame());
		e.getRegistry().register(new ItemTrafficLight6Frame());
		e.getRegistry().register(new ItemTrafficLight7Frame());
		if(ModRealisticTrafficControl.OC_INSTALLED)
		{
			e.getRegistry().register(new ItemTrafficLightCard());
		}

		e.getRegistry().register(new ItemBlock(ModBlocks.crossing_gate_base).setRegistryName(ModBlocks.crossing_gate_base.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.crossing_gate_pole).setRegistryName(ModBlocks.crossing_gate_pole.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.stand).setRegistryName(ModBlocks.stand.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_light_cover).setRegistryName(ModBlocks.traffic_light_cover.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_light1_cover).setRegistryName(ModBlocks.traffic_light1_cover.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_light2_cover).setRegistryName(ModBlocks.traffic_light2_cover.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_light4_cover).setRegistryName(ModBlocks.traffic_light4_cover.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_light5_cover).setRegistryName(ModBlocks.traffic_light5_cover.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_light6_cover).setRegistryName(ModBlocks.traffic_light6_cover.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_light7_cover).setRegistryName(ModBlocks.traffic_light7_cover.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_lightdoghouse_cover).setRegistryName(ModBlocks.traffic_lightdoghouse_cover.getRegistryName()));
	
		e.getRegistry().register(new ItemBlock(ModBlocks.sign).setRegistryName(ModBlocks.sign.getRegistryName()));
		e.getRegistry().register(new ItemCone(ModBlocks.cone).setRegistryName(ModBlocks.cone.getRegistryName()));
		e.getRegistry().register(new ItemTrafficLightControlBox(ModBlocks.traffic_light_control_box).setRegistryName(ModBlocks.traffic_light_control_box.getRegistryName()));
		e.getRegistry().register(new ItemCone(ModBlocks.channelizer).setRegistryName(ModBlocks.channelizer.getRegistryName()));
		e.getRegistry().register(new ItemCone(ModBlocks.drum).setRegistryName(ModBlocks.drum.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.street_light_single).setRegistryName(ModBlocks.street_light_single.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.street_light_double).setRegistryName(ModBlocks.street_light_double.getRegistryName()));

	
		e.getRegistry().register(new ItemBlock(ModBlocks.type_3_barrier).setRegistryName(ModBlocks.type_3_barrier.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.type_3_barrier_right).setRegistryName(ModBlocks.type_3_barrier_right.getRegistryName()));
		
		e.getRegistry().register(new ItemBlock(ModBlocks.concrete_barrier)
		{
			public boolean getHasSubtypes() { return true; }

			public String getUnlocalizedName(net.minecraft.item.ItemStack stack)
			{
				String unlocalizedName = ModRealisticTrafficControl.MODID + ".concrete_barrier.";
				int meta = stack.getMetadata();

				EnumDyeColor color = EnumDyeColor.byMetadata(meta);
				unlocalizedName += color.getName();

				return unlocalizedName;
			}


		}.setRegistryName(ModBlocks.concrete_barrier.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.horizontal_pole).setRegistryName(ModBlocks.horizontal_pole.getRegistryName()));
	
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_sensor_left).setRegistryName(ModBlocks.traffic_sensor_left.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_sensor_straight).setRegistryName(ModBlocks.traffic_sensor_straight.getRegistryName()));
		e.getRegistry().register(new ItemStreetSign(ModBlocks.street_sign).setRegistryName(ModBlocks.street_sign.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.pedestrian_button).setRegistryName(ModBlocks.pedestrian_button.getRegistryName()));
		e.getRegistry().register(new ItemBlock(ModBlocks.traffic_sensor_right).setRegistryName(ModBlocks.traffic_sensor_right.getRegistryName()));
	}

	@SubscribeEvent
	public static void registerSounds(RegistryEvent.Register<SoundEvent> e)
	{
		e.getRegistry().register(ModSounds.gateEvent);
		e.getRegistry().register(ModSounds.safetranType3Event);
		e.getRegistry().register(ModSounds.safetranMechanicalEvent);
		e.getRegistry().register(ModSounds.wchEvent);
		e.getRegistry().register(ModSounds.pedButton);
	}

	public void preInit(FMLPreInitializationEvent e)
	{
		File directory = e.getModConfigurationDirectory();
		config = new Configuration(new File(directory.getPath(), "realistictrafficcontrol.cfg"));
		 Config.readConfig();
		 
		

		ModSounds.initSounds();
		PacketHandler.registerMessages("rrealtc");
	}

	public void init(FMLInitializationEvent e)
	{
		NetworkRegistry.INSTANCE.registerGuiHandler(ModRealisticTrafficControl.instance, new GuiProxy());

		Consumer<String> signRepoSplashUpdate;
		IntConsumer signRepoSplashStepsUpdate;

		if (FMLCommonHandler.instance().getSide() == Side.CLIENT)
		{
			signRepoSplashUpdate = getClientSplashUpdate();
			signRepoSplashStepsUpdate = getClientStepsUpdate();
		}
		else
		{
			signRepoSplashUpdate = getServerSplashUpdate();
			signRepoSplashStepsUpdate = getServerStepsUpdate();
		}

		ModRealisticTrafficControl.instance.signRepo = new SignRepository();
		ModRealisticTrafficControl.instance.signRepo.init(signRepoSplashUpdate, signRepoSplashStepsUpdate);

		if (FMLCommonHandler.instance().getSide() == Side.CLIENT)
		{
			endSignInit();
		}

		if (ModRealisticTrafficControl.OC_INSTALLED)
		{
			OpenComputersHelper.addOCDriver();
		}
	}

	@SideOnly(Side.CLIENT)
	ProgressBar signLoadProgress;

	@SideOnly(Side.CLIENT)
	private Consumer<String> getClientSplashUpdate()
	{
		return splash ->
		{
			if (signLoadProgress == null)
			{
				return;
			}

			if (signLoadProgress.getStep() >= signLoadProgress.getSteps())
			{
				return;
			}

			signLoadProgress.step(splash);
		};
	}

	@SideOnly(Side.SERVER)
	private Consumer<String> getServerSplashUpdate()
	{
		return splash ->
		{
			ModRealisticTrafficControl.logger.info(splash);
		};
	}

	@SideOnly(Side.CLIENT)
	private IntConsumer getClientStepsUpdate()
	{
		return steps ->
		{
			if (signLoadProgress != null)
			{
				while(signLoadProgress.getStep() < signLoadProgress.getSteps()) // Finish out progress bar
				{
					signLoadProgress.step("");
				}
				
				ProgressManager.pop(signLoadProgress);
			}

			signLoadProgress = ProgressManager.push("Loading Signs", steps);
		};
	}

	@SideOnly(Side.SERVER)
	private IntConsumer getServerStepsUpdate()
	{
		return steps -> {};
	}

	@SideOnly(Side.CLIENT)
	private void endSignInit()
	{
		if (signLoadProgress != null)
		{
			ProgressManager.pop(signLoadProgress);
		}
	}


	public void postInit(FMLPostInitializationEvent e)
	{

	}
}
