package com.gamearoosdevelopment.realistictrafficcontrol;

import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignRepository;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Main entry point for Realistic Traffic Control on NeoForge 1.21.1.
 */
@Mod(ModRealisticTrafficControl.MODID)
public class ModRealisticTrafficControl {
    public static final String MODID = "realistictrafficcontrol";
    public static final String MODNAME = "Realistic Traffic Control";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Optifine's max render distance is 32 chunks. (32 x 16) ^ 2 = 262144. */
    public static final double MAX_RENDER_DISTANCE = 262144;

    public static boolean IR_INSTALLED = false;
    public static boolean CREATE_INSTALLED = false;
    public static boolean CC_INSTALLED = false;
    public static boolean OC_INSTALLED = false;

    /** Sign pack repository (initialized on common setup). */
    public static SignRepository signRepo = new SignRepository();

    public ModRealisticTrafficControl(IEventBus modBus, ModContainer modContainer) {
        IR_INSTALLED = ModList.get().isLoaded("immersiverailroading");
        CREATE_INSTALLED = ModList.get().isLoaded("create");
        CC_INSTALLED = ModList.get().isLoaded("computercraft");
        OC_INSTALLED = ModList.get().isLoaded("opencomputers");

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        RTCDataComponents.COMPONENTS.register(modBus);

        modBus.addListener(com.gamearoosdevelopment.realistictrafficcontrol.network.RTCNetworking::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.gamearoosdevelopment.realistictrafficcontrol.command.RTCCommands::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modBus.addListener(this::onConfigLoad);
        modBus.addListener(this::onConfigReload);
        modBus.addListener(this::onCommonSetup);

        LOGGER.info("Realistic Traffic Control loading (IR={}, Create={}, CC={}, OC={})",
                IR_INSTALLED, CREATE_INSTALLED, CC_INSTALLED, OC_INSTALLED);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            signRepo.init(str -> {}, steps -> {});
            if (CC_INSTALLED) {
                com.gamearoosdevelopment.realistictrafficcontrol.cc.TrafficLightPeripheralProvider.register();
            }
            if (OC_INSTALLED) {
                com.gamearoosdevelopment.realistictrafficcontrol.compat.OpenComputersCompat.register();
            }
        });
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            Config.refresh();
        }
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            Config.refresh();
        }
    }
}
