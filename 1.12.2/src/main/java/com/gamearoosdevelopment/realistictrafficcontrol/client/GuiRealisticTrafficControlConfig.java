
package com.gamearoosdevelopment.realistictrafficcontrol.client;

import java.util.ArrayList;
import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.Config;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

public class GuiRealisticTrafficControlConfig extends GuiConfig {

    public GuiRealisticTrafficControlConfig(GuiScreen parent) {
        super(parent, getConfigElements(), ModRealisticTrafficControl.MODID, false, false, "Realistic Traffic Control Config");
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        for (String categoryName : Config.config.getCategoryNames()) {
            list.add(new ConfigElement(Config.config.getCategory(categoryName)));
        }
        return list;
    }
}
