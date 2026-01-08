package com.daytonjwatson.ledger.economy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BankCommand implements CommandExecutor {
	private final BankService bankService;

	public BankCommand(BankService bankService) {
		this.bankService = bankService;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.RED + "Players only.");
			return true;
		}
		if (args.length < 1 || !args[0].equalsIgnoreCase("deposit")) {
			player.sendMessage(ChatColor.YELLOW + "Usage: /bank deposit <amount|all>");
			return true;
		}
		long amount = -1;
		if (args.length >= 2 && !args[1].equalsIgnoreCase("all")) {
			try {
				amount = Long.parseLong(args[1]);
			} catch (NumberFormatException ex) {
				player.sendMessage(ChatColor.RED + "Invalid amount.");
				return true;
			}
		}
		if (!bankService.deposit(player, amount)) {
			player.sendMessage(ChatColor.RED + "You can only deposit at spawn with carried money.");
			return true;
		}
		player.sendMessage(ChatColor.GREEN + "Deposit complete.");
		return true;
	}
}
