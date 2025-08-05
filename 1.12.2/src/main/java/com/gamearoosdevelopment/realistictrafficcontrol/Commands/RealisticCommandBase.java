package com.gamearoosdevelopment.realistictrafficcontrol.Commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import java.util.*;

public abstract class RealisticCommandBase extends CommandBase {

	private static final Map<String, RealisticCommandBase> subcommands = new HashMap<>();

	public static void registerSubCommand(RealisticCommandBase command) {
		subcommands.put(command.getName().toLowerCase(), command);
	}

	@Override
	public String getName() {
		return "realistictrafficcontrol";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		if (!subcommands.isEmpty()) {
			StringBuilder builder = new StringBuilder("Subcommands:\n");
			for (RealisticCommandBase cmd : subcommands.values()) {
				builder.append(" - ").append(cmd.getSubUsage()).append("\n");
			}
			return builder.toString().trim();
		}
		return "/realistictrafficcontrol <subcommand>";
	}

	
	public abstract String getSubUsage(); // avoids conflict with CommandBase


	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0) {
			throw new CommandException("Missing subcommand. Try /realistictrafficcontrol <subcommand>");
		}

		String sub = args[0].toLowerCase();
		RealisticCommandBase cmd = subcommands.get(sub);

		if (cmd == null) {
			throw new CommandException("Unknown subcommand: " + sub);
		}

		cmd.executeSub(server, sender, Arrays.copyOfRange(args, 1, args.length));
	}

	// Subcommands must override this instead
	public abstract void executeSub(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException;
}
