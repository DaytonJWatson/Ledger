package com.daytonjwatson.ledger.gui;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand implements CommandExecutor {
	private final GuiManager guiManager;
	private final MenuId menuId;

	public MenuCommand(GuiManager guiManager, MenuId menuId) {
		this.guiManager = guiManager;
		this.menuId = menuId;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.RED + "Players only.");
			return true;
		}
		guiManager.open(menuId, player);
		return true;
	}
}
