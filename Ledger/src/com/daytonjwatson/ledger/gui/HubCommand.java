package com.daytonjwatson.ledger.gui;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HubCommand implements CommandExecutor {
	private final GuiManager guiManager;

	public HubCommand(GuiManager guiManager) {
		this.guiManager = guiManager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.RED + "Players only.");
			return true;
		}
		guiManager.open(MenuId.HUB, player);
		return true;
	}
}
