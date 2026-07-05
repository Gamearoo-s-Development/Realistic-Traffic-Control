package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.RelayBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port of 1.12.2 {@code CrossingRelaySettingsGui}: configure bell stop time on crossing relays.
 */
public class CrossingRelaySettingsScreen extends Screen {

    private final Level level;
    private final BlockPos clickedRelayPos;
    private EditBox bellStopSeconds;

    public CrossingRelaySettingsScreen(Level level, BlockPos clickedRelayPos) {
        super(Component.translatable("realistictrafficcontrol.gui.relay.title"));
        this.level = level;
        this.clickedRelayPos = clickedRelayPos;
    }

    private RelayBlockEntity relayForSettings() {
        if (level == null) {
            return null;
        }
        BlockEntity tile = level.getBlockEntity(clickedRelayPos);
        if (!(tile instanceof RelayBlockEntity atClick)) {
            return null;
        }
        RelayBlockEntity master = atClick.getMaster(level);
        return master != null ? master : atClick;
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int cy = height / 2;
        bellStopSeconds = new EditBox(font, cx - 40, cy, 80, 20, Component.empty());
        RelayBlockEntity relay = relayForSettings();
        int initial = relay != null ? relay.getRelayBellStopAfterSeconds() : 0;
        bellStopSeconds.setValue(String.valueOf(initial));
        bellStopSeconds.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addRenderableWidget(bellStopSeconds);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(cx - 50, cy + 28, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;
        Component title = Component.translatable("realistictrafficcontrol.gui.relay.title");
        graphics.drawString(font, title, cx - font.width(title) / 2, cy - 36, 0xFFFFFF);
        Component label = Component.translatable("realistictrafficcontrol.gui.relay.bellstop");
        graphics.drawString(font, label, cx - font.width(label) / 2, cy - 18, 0xA0A0A0);
        bellStopSeconds.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        int sec = 0;
        try {
            sec = Integer.parseInt(bellStopSeconds.getValue().trim());
        } catch (NumberFormatException ignored) {
            sec = 0;
        }
        sec = Math.clamp(sec, 0, 3600);
        RelayBlockEntity relay = relayForSettings();
        if (relay != null) {
            relay.setRelayBellStopAfterSeconds(sec);
            relay.performClientToServerSync();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
