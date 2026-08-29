package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Ensures crossing-lamp BER submodels are baked (1.21 only loads models referenced from blockstates/items). */
public final class CrossingLampClientModels {

    private static final String[] QUADRANTS = { "ne", "nw", "se", "sw" };
    private static final String[] LAMP_PREFIXES = { "ped_crossing_lamps", "crossing_gate_lamps", "overhead_lamps" };

    private CrossingLampClientModels() {
    }

    public static void registerAdditional(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        for (String prefix : LAMP_PREFIXES) {
            for (String quadrant : QUADRANTS) {
                event.register(ModelResourceLocation.standalone(modelLocation(prefix + "_" + quadrant + "_lamp")));
                event.register(ModelResourceLocation.standalone(modelLocation(prefix + "_" + quadrant + "_lamp_lit")));
                if (!"ped_crossing_lamps".equals(prefix)) {
                    event.register(ModelResourceLocation.standalone(
                            modelLocation(prefix + "_" + quadrant + "_support")));
                }
            }
        }
        event.register(ModelResourceLocation.standalone(modelLocation("crossing_gate_light")));
        event.register(ModelResourceLocation.standalone(modelLocation("crossing_gate_light_on")));
        event.register(ModelResourceLocation.standalone(modelLocation("crossing_gate_pole_ext")));
        event.register(ModelResourceLocation.standalone(modelLocation("ped_crossing_light")));
        event.register(ModelResourceLocation.standalone(modelLocation("ped_crossing_light_flash1")));
        event.register(ModelResourceLocation.standalone(modelLocation("ped_crossing_light_flash2")));
        event.register(ModelResourceLocation.standalone(modelLocation("crossing_gate_lamps_empty")));
        event.register(ModelResourceLocation.standalone(modelLocation("crossing_gate_lamps_empty_flash1")));
        event.register(ModelResourceLocation.standalone(modelLocation("crossing_gate_lamps_empty_flash2")));
    }

    /** Map 1.12-style {@code rotation=X,state=Y} variant keys onto standalone baked lamp models. */
    public static void aliasLampStateVariants(Map<ModelResourceLocation, BakedModel> models) {
        for (String prefix : LAMP_PREFIXES) {
            for (String quadrant : QUADRANTS) {
                String path = prefix + "_" + quadrant + "_lamp";
                ModelResourceLocation standalone = ModelResourceLocation.standalone(modelLocation(path));
                BakedModel baked = models.get(standalone);
                if (baked == null) {
                    continue;
                }
                BakedModel lit = models.get(ModelResourceLocation.standalone(modelLocation(path + "_lit")));
                for (CrossingLampState flashState : CrossingLampState.values()) {
                    for (int rotation = 0; rotation <= 16; rotation++) {
                        String variant = "rotation=" + rotation + ",state=" + flashState.getSerializedName();
                        ModelResourceLocation variantLoc = new ModelResourceLocation(
                                ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, path), variant);
                        BakedModel use = baked;
                        if (flashState == CrossingLampState.Flash1
                                && ("ne".equals(quadrant) || "se".equals(quadrant)) && lit != null) {
                            use = lit;
                        } else if (flashState == CrossingLampState.Flash2
                                && ("nw".equals(quadrant) || "sw".equals(quadrant)) && lit != null) {
                            use = lit;
                        }
                        models.putIfAbsent(variantLoc, use);
                    }
                }
            }
        }
    }

    public static ResourceLocation modelLocation(String modelPath) {
        return ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "block/" + modelPath);
    }

    public static ModelResourceLocation lampModel(String modelName, int bulbRotation, String stateName) {
        String variant = "rotation=" + bulbRotation + ",state=" + stateName;
        return new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, modelName), variant);
    }
}
