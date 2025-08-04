package com.gamearoosdevelopment.realistictrafficcontrol.Commands;

import java.io.File;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.proxy.CommonProxy;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Configuration;

public class CommandConfigReload extends RealisticCommandBase {
	public static Configuration config;
	@Override
	public String getName() {
		return "configreload";
	}

	@Override
	public String getSubUsage() {
		return "/realistictrafficcontrol configreload";
	}

	@Override
	public void executeSub(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		// Your config reload logic here
		if (CommonProxy.config != null) {
			CommonProxy.config.load();       // reload from disk
			Config.readConfig();             // re-parse and re-apply
			sender.sendMessage(new TextComponentString("Configuration reloaded."));
		} else {
			sender.sendMessage(new TextComponentString("Config not loaded or available."));
		}
		
	}
}
