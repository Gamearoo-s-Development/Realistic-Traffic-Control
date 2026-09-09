package com.gamearoosdevelopment.realistictrafficcontrol.compat.ponder;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RTCPonderPlugin implements PonderPlugin {

    static final ResourceLocation TAG =
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "traffic_control");

    static final ResourceLocation[] ITEMS = {
            id("pole"),
            id("horizontal_pole"),
            id("traffic_light_frame"),
            id("traffic_light_hoz_frame"),
            id("traffic_light_1_frame"),
            id("traffic_light_2_frame"),
            id("traffic_light_2_hoz_frame"),
            id("traffic_light_4_frame"),
            id("traffic_light_4_hoz_frame"),
            id("traffic_light_5_frame"),
            id("traffic_light_5_hoz_frame"),
            id("traffic_light_doghouse_frame"),
            id("traffic_light_6_frame"),
            id("traffic_light_7_frame"),
            id("traffic_light_8_frame"),
            id("sign"),
            id("digital_sign"),
            id("street_sign"),
            id("screwdriver")
    };

    @Override
    public String getModId() {
        return ModRealisticTrafficControl.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        RTCPonderScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(TAG)
                .addToIndex()
                .item(com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks.POLE.get())
                .title("Traffic Control")
                .description("Poles, frames, signs, and 16-step mounting")
                .register();
        var tag = helper.addToTag(TAG);
        for (ResourceLocation item : ITEMS) {
            tag.add(item);
        }
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, path);
    }
}
