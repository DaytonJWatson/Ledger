package com.daytonjwatson.ledger.economy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {
	private final MoneyService moneyService;

	public BalanceCommand(MoneyService moneyService) {
		this.moneyService = moneyService;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.RED + "Players only.");
			return true;
		}
		long carried = moneyService.getCarried(player.getUniqueId());
		long banked = moneyService.getBanked(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "Carried: $" + carried + " | Banked: $" + banked);
		return true;
	}
}
