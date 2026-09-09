package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

final class GuiTypingGuard {
    private GuiTypingGuard() {
    }

    static boolean shouldConsumeInventoryKey(int keyCode, int scanCode, boolean typingContext) {
        if (!typingContext) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return false;
        }
        return minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
    }
}
