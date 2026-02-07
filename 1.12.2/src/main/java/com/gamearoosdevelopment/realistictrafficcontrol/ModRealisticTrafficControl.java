package com.gamearoosdevelopment.realistictrafficcontrol;

import org.apache.logging.log4j.Logger;

import com.gamearoosdevelopment.realistictrafficcontrol.Commands.CommandConfigReload;
import com.gamearoosdevelopment.realistictrafficcontrol.Commands.CommandDispatcher;
import com.gamearoosdevelopment.realistictrafficcontrol.Commands.RealisticCommandBase;
import com.gamearoosdevelopment.realistictrafficcontrol.network.ModNetworkHandler;
import com.gamearoosdevelopment.realistictrafficcontrol.proxy.CommonProxy;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignRepository;
import com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory.TabB;
import com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory.TabBulbs;
import com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory.TabCones;
import com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory.TabFrames;
import com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory.TabOC;
import com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory.TabPoles;
import com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory.TabSensor;
import com.gamearoosdevelopment.realistictrafficcontrol.Iinvatory.TabTools;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = "realistictrafficcontrol", version = ModRealisticTrafficControl.VERSION, name = "Realistic Traffic Control", useMetadata = true,  guiFactory = "com.gamearoosdevelopment.realistictrafficcontrol.client.ModGuiFactory")
public class ModRealisticTrafficControl {
	public static final String MODID = "realistictrafficcontrol";
	public static final String VERSION = "3.2.0";
	public static final String MODNAME = "Realistic Traffic Control";
	public static boolean IR_INSTALLED = false;
	public static boolean OC_INSTALLED = false;
	public static boolean CC_INSTALLED = false;
	public static boolean TC_INSTALLED = false;

	public static final CreativeTabs TOOLS_TAB = new TabTools("tools_tab");
	public static final CreativeTabs FRAMES_TAB = new TabFrames("frames_tab");
	public static final CreativeTabs BULBS_TAB = new TabBulbs("bulbs_tab");
	public static final CreativeTabs POLES_TAB = new TabPoles("poles_tab");
	public static final CreativeTabs OC_TAB = new TabOC("oc_tab");
	public static final CreativeTabs CONES_TAB = new TabCones("cones_tab");
	public static final CreativeTabs BARRIERS_TAB = new TabB("b_tab");
	public static final CreativeTabs SENSORS_TAB = new TabSensor("sensor_tab");
	public static final double MAX_RENDER_DISTANCE = 262144; // Optifine's max render distance is 32 chunks.  (32 x 16) ^ 2 = 262144

	@SidedProxy(clientSide = "com.gamearoosdevelopment.realistictrafficcontrol.proxy.ClientProxy", serverSide = "com.gamearoosdevelopment.realistictrafficcontrol.proxy.ServerProxy")
	public static CommonProxy proxy;

	@Mod.Instance
	public static ModRealisticTrafficControl instance;

	public static Logger logger;
	
	public SignRepository signRepo;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e)
	{
		CC_INSTALLED = Loader.isModLoaded("computercraft");
		OC_INSTALLED = Loader.isModLoaded("opencomputers");
		//TC_INSTALLED = Loader.isModLoaded("trafficcontrol");
		 ModNetworkHandler.registerPackets();
		logger = e.getModLog();
		proxy.preInit(e);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent e)
	{
		
		proxy.init(e);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent e)
	{
		proxy.postInit(e);

		IR_INSTALLED = Loader.isModLoaded("immersiverailroading");
	}
	
	@Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent event) {
	event.registerServerCommand(new CommandDispatcher());
	RealisticCommandBase.registerSubCommand(new CommandConfigReload());
	}
	
	
	
}
