package com.gamearoosdevelopment.realistictrafficcontrol;

import org.apache.logging.log4j.Logger;

import com.gamearoosdevelopment.realistictrafficcontrol.proxy.CommonProxy;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignRepository;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = "realistictrafficcontrol", version = ModRealisticTrafficControl.VERSION, name = "Realistic Traffic Control", useMetadata = true)
public class ModRealisticTrafficControl {
	public static final String MODID = "realistictrafficcontrol";
	public static final String VERSION = "0.1.0";
	public static final String MODNAME = "Realistic Traffic Control";
	public static boolean IR_INSTALLED = false;
	public static boolean OC_INSTALLED = false;
	public static CreativeTabs CREATIVE_TAB = new CreativeTabs("Realistc Traffic Control") {

		@Override
		public ItemStack getTabIconItem() {
			// TODO Auto-generated method stub
			return new ItemStack(ModBlocks.traffic_light_control_box);
		}
	};
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
		OC_INSTALLED = Loader.isModLoaded("opencomputers");
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
	
	
	
}
