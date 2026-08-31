package com.gamearoosdevelopment.realistictrafficcontrol.event;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.scanner.Scanner;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Port of 1.12.2 {@code WorldEventHandler}: creates per-dimension {@link Scanner} instances when a
 * supported railroad mod is installed and ticks them at end of world tick.
 */
@EventBusSubscriber(modid = ModRealisticTrafficControl.MODID)
public final class ScannerWorldEvents {

    private ScannerWorldEvents() {
    }

    @SubscribeEvent
    public static void onLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !hasRailroadCompat()) {
            return;
        }
        try {
            Scanner scanner = new Scanner(serverLevel);
            Scanner.scannersByWorld.put(serverLevel.dimension(), scanner);
        } catch (Exception ex) {
            ModRealisticTrafficControl.LOGGER.error("Could not start Scanner for dimension {}", serverLevel.dimension(), ex);
        }
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && hasRailroadCompat()) {
            Scanner.scannersByWorld.remove(serverLevel.dimension());
        }
    }

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
                || !hasRailroadCompat()
                || event.getLevel().isClientSide()) {
            return;
        }
        Scanner scanner = Scanner.scannersByWorld.get(serverLevel.dimension());
        if (scanner != null) {
            scanner.tick(serverLevel);
        }
    }

    private static boolean hasRailroadCompat() {
        return ModRealisticTrafficControl.IR_INSTALLED || ModRealisticTrafficControl.CREATE_INSTALLED;
    }
}
