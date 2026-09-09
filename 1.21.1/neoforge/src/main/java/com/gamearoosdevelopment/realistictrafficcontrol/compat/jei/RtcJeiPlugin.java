package com.gamearoosdevelopment.realistictrafficcontrol.compat.jei;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.client.TrafficLightFrameScreen;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class RtcJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            ModRealisticTrafficControl.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(TrafficLightFrameScreen.class,
                new IGuiContainerHandler<TrafficLightFrameScreen>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(TrafficLightFrameScreen screen) {
                        return screen.getJeiExtraAreas();
                    }
                });
    }
}
