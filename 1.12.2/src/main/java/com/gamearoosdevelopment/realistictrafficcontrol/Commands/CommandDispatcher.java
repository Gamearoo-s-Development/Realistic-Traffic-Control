package com.gamearoosdevelopment.realistictrafficcontrol.Commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CommandDispatcher extends RealisticCommandBase {

	@Override
	public String getName() {
		return "realistictrafficcontrol";
	}

	@Override
	public String getSubUsage() {
		return "/realistictrafficcontrol <subcommand>";
	}

	@Override
	public void executeSub(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		// Should never be directly called — handled in RealisticCommandBase
	}
}
