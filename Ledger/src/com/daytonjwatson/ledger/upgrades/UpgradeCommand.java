package com.daytonjwatson.ledger.upgrades;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpgradeCommand implements CommandExecutor {
	private final UpgradeService upgradeService;

	public UpgradeCommand(UpgradeService upgradeService) {
		this.upgradeService = upgradeService;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.RED + "Players only.");
			return true;
		}
		if (label.equalsIgnoreCase("upgrades")) {
			return listUpgrades(player);
		}
		if (args.length < 2 || !args[0].equalsIgnoreCase("buy")) {
			player.sendMessage(ChatColor.YELLOW + "Usage: /upgrade buy <id>");
			return true;
		}
		String upgradeId = args[1].toLowerCase(Locale.ROOT);
		UpgradeDefinition definition = upgradeService.getDefinition(upgradeId);
		if (definition == null) {
			player.sendMessage(ChatColor.RED + "Unknown upgrade id: " + upgradeId);
			return true;
		}
		int currentLevel = upgradeService.getLevel(player.getUniqueId(), upgradeId);
		long cost = definition.getNextCost(currentLevel);
		int nextLevel = definition.getType() == UpgradeDefinition.Type.LEVEL ? currentLevel + 1 : 1;
		String nextEffect = upgradeService.getNextEffect(definition, player.getUniqueId(), nextLevel);
		UpgradeService.PurchaseResult result = upgradeService.purchaseUpgrade(player, upgradeId);
		if (result.isSuccess()) {
			player.sendMessage(ChatColor.GREEN + result.getMessage());
			return true;
		}
		player.sendMessage(ChatColor.RED + result.getMessage());
		if (definition.getType() == UpgradeDefinition.Type.LEVEL && currentLevel < definition.getMaxLevel()) {
			player.sendMessage(ChatColor.GRAY + "Cost: $" + cost + ". Next effect: " + nextEffect);
		} else if (definition.getType() != UpgradeDefinition.Type.LEVEL && currentLevel == 0) {
			player.sendMessage(ChatColor.GRAY + "Cost: $" + cost + ". Next effect: " + nextEffect);
		}
		return true;
	}

	private boolean listUpgrades(Player player) {
		player.sendMessage(ChatColor.GOLD + "Available upgrades:");
		List<Map.Entry<String, UpgradeDefinition>> entries = upgradeService.getDefinitions().entrySet().stream()
			.sorted(Comparator.comparing(entry -> entry.getValue().getName()))
			.toList();
		for (Map.Entry<String, UpgradeDefinition> entry : entries) {
			UpgradeDefinition definition = entry.getValue();
			int level = upgradeService.getLevel(player.getUniqueId(), definition.getId());
			player.sendMessage(upgradeService.formatUpgradeLine(player, definition));
			if (!definition.getDescription().isBlank()) {
				player.sendMessage(ChatColor.DARK_GRAY + "  " + definition.getDescription());
			}
			if (definition.getType() == UpgradeDefinition.Type.LEVEL && level < definition.getMaxLevel()) {
				long cost = definition.getNextCost(level);
				player.sendMessage(ChatColor.GRAY + "  Next: $" + cost + ". " + upgradeService.getNextEffect(definition, player.getUniqueId(), level + 1));
			} else if (definition.getType() != UpgradeDefinition.Type.LEVEL && level == 0) {
				long cost = definition.getNextCost(level);
				player.sendMessage(ChatColor.GRAY + "  Cost: $" + cost + ". " + upgradeService.getNextEffect(definition, player.getUniqueId(), 1));
			}
		}
		return true;
	}
}
