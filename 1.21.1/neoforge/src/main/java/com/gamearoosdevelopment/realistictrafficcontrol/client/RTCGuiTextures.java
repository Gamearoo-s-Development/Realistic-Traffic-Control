package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** GUI helpers for drawing mod textures outside atlases. */
public final class RTCGuiTextures {

    private RTCGuiTextures() {
    }

    public static void blitBlock(GuiGraphics graphics, String textureName, int x, int y) {
        ResourceLocation texture = switch (textureName) {
            case "redstone_torch" -> ResourceLocation.withDefaultNamespace("textures/block/redstone_torch.png");
            case "redstone_torch_off" -> ResourceLocation.withDefaultNamespace("textures/block/redstone_torch_off.png");
            default -> ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID,
                    "textures/block/" + textureName + ".png");
        };
        blit16(graphics, texture, x, y);
    }

    public static void blit16(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    }

    public static void blitFullscreenGui(GuiGraphics graphics, ResourceLocation texture, int screenWidth, int screenHeight) {
        graphics.blit(texture, 0, 0, screenWidth, screenHeight, 0.0f, 0.0f, 16, 16, 16, 16);
    }
}
