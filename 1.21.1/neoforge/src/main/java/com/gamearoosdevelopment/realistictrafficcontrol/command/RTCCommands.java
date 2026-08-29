package com.gamearoosdevelopment.realistictrafficcontrol.command;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Brigadier port of the 1.12.2 {@code CommandDispatcher}/{@code CommandConfigReload}. Registers
 * {@code /realistictrafficcontrol configreload}, which re-applies the {@link Config} spec values.
 *
 * <p>Unlike 1.12.2 (Forge {@code Configuration.load()}), NeoForge re-reads the on-disk config
 * automatically and fires {@code ModConfigEvent.Reloading}; this command additionally refreshes the
 * static mirror fields on demand.
 */
public final class RTCCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("realistictrafficcontrol")
                        .then(Commands.literal("configreload")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    Config.refresh();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("[Realistic Traffic Control] Configuration reloaded."),
                                            true);
                                    return 1;
                                })));
    }

    private RTCCommands() {
    }
}
