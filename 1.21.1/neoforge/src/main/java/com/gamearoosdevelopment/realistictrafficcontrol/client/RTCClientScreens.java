package com.gamearoosdevelopment.realistictrafficcontrol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RTCClientScreens {

    private RTCClientScreens() {
    }

    public static void openCrossingRelaySettings(Level level, BlockPos pos) {
        Minecraft.getInstance().setScreen(new CrossingRelaySettingsScreen(level, pos));
    }
}
