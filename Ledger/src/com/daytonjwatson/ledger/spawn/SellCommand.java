package com.daytonjwatson.ledger.spawn;

import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.market.MarketService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SellCommand implements CommandExecutor {
	private final SpawnRegionService spawnRegionService;
	private final MarketService marketService;
	private final MoneyService moneyService;

	public SellCommand(SpawnRegionService spawnRegionService, MarketService marketService, MoneyService moneyService) {
		this.spawnRegionService = spawnRegionService;
		this.marketService = marketService;
		this.moneyService = moneyService;
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
		ItemStack[] items = player.getInventory().getContents();
		Map<Material, Integer> sellableCounts = new HashMap<>();
		for (ItemStack item : items) {
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			if (marketService.getSellPrice(item) <= 0.0) {
				continue;
			}
			sellableCounts.merge(item.getType(), item.getAmount(), Integer::sum);
		}
		int distinctTypes = sellableCounts.size();
		long total = 0;
		int soldCount = 0;
		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			double value = marketService.getSellPrice(player, item, distinctTypes);
			if (value <= 0.0) {
				continue;
			}
			total += Math.round(value * item.getAmount());
			soldCount += item.getAmount();
			marketService.applySale(item.getType().name(), item.getAmount());
			items[i] = null;
		}
		player.getInventory().setContents(items);
		if (total <= 0) {
			player.sendMessage(ChatColor.RED + "No sellable items.");
			return true;
		}
		moneyService.addCarried(player, total);
		player.sendMessage(ChatColor.GREEN + "Sold " + soldCount + " items for $" + total + ".");
		return true;
	}

	private boolean sellHand(Player player) {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item == null || item.getType() == Material.AIR) {
			player.sendMessage(ChatColor.RED + "Hold an item to sell.");
			return true;
		}
		double value = marketService.getSellPrice(player, item);
		if (value <= 0.0) {
			player.sendMessage(ChatColor.RED + "That item cannot be sold.");
			return true;
		}
		long total = Math.round(value * item.getAmount());
		marketService.applySale(item.getType().name(), item.getAmount());
		player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
		moneyService.addCarried(player, total);
		player.sendMessage(ChatColor.GREEN + "Sold for $" + total + ".");
		return true;
	}
}
