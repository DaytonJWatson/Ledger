package com.daytonjwatson.ledger.spawn;

import com.daytonjwatson.ledger.spawn.SellService.SellOutcome;
import com.daytonjwatson.ledger.spawn.SellService.SellStatus;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellCommand implements CommandExecutor {
	private final SpawnRegionService spawnRegionService;
	private final SellService sellService;

	public SellCommand(SpawnRegionService spawnRegionService, SellService sellService) {
		this.spawnRegionService = spawnRegionService;
		this.sellService = sellService;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.RED + "Players only.");
			return true;
		}
		if (!spawnRegionService.isInSpawn(player.getLocation())) {
			player.sendMessage(ChatColor.RED + "You can only sell at spawn.");
			return true;
		}
		if (args.length == 0 || args[0].equalsIgnoreCase("inventory")) {
			return sellInventory(player);
		}
		if (args[0].equalsIgnoreCase("hand")) {
			return sellHand(player);
		}
		player.sendMessage(ChatColor.YELLOW + "Usage: /sell [inventory|hand]");
		return true;
	}

	private boolean sellInventory(Player player) {
		SellOutcome outcome = sellService.sellInventory(player);
		if (outcome.getStatus() == SellStatus.NO_SELLABLE) {
			player.sendMessage(ChatColor.RED + "No sellable items.");
			return true;
		}
		player.sendMessage(ChatColor.GREEN + "Sold " + outcome.getSoldCount() + " items for $" + outcome.getTotal() + ".");
		return true;
	}

	private boolean sellHand(Player player) {
		SellOutcome outcome = sellService.sellHand(player);
		if (outcome.getStatus() == SellStatus.NO_ITEM) {
			player.sendMessage(ChatColor.RED + "Hold an item to sell.");
			return true;
		}
		if (outcome.getStatus() == SellStatus.NO_SELLABLE) {
			player.sendMessage(ChatColor.RED + "No sellable items.");
			return true;
		}
		player.sendMessage(ChatColor.GREEN + "Sold for $" + outcome.getTotal() + ".");
		return true;
	}
}
