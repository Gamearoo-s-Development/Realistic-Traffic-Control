package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Ensures wig-wag BER submodels are baked (not referenced from block blockstates). */
public final class WigWagClientModels {

    private static final String[] ARM_MODELS = {
            "wig_wag_arm_mount",
            "wig_wag_arm",
            "wig_wag_arm_lamp_off",
            "wig_wag_arm_lamp_on",
            "vertical_wig_wag_arm_mount",
            "vertical_wig_wag_arm",
            "vertical_wig_wag_arm_lamp_off",
            "vertical_wig_wag_arm_lamp_on"
    };

    private WigWagClientModels() {
    }

    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        for (String path : ARM_MODELS) {
            event.register(ModelResourceLocation.standalone(modelLocation(path)));
        }
    }

    public static ResourceLocation modelLocation(String modelPath) {
        return ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "block/" + modelPath);
    }

    public static ModelResourceLocation armModel(boolean vertical, boolean lampOn) {
        String base = vertical ? "vertical_wig_wag_arm" : "wig_wag_arm";
        String suffix = lampOn ? "_lamp_on" : "_lamp_off";
        return ModelResourceLocation.standalone(modelLocation(base + suffix));
    }

    public static ModelResourceLocation mountModel(boolean vertical) {
        String path = vertical ? "vertical_wig_wag_arm_mount" : "wig_wag_arm_mount";
        return ModelResourceLocation.standalone(modelLocation(path));
    }
}
