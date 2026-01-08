package com.daytonjwatson.ledger.tools;

import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ToolVendorCommand implements CommandExecutor {
	private final SpawnRegionService spawnRegionService;
	private final ToolVendorService toolVendorService;
	private final RepairService repairService;

	public ToolVendorCommand(SpawnRegionService spawnRegionService, ToolVendorService toolVendorService, RepairService repairService) {
		this.spawnRegionService = spawnRegionService;
		this.toolVendorService = toolVendorService;
		this.repairService = repairService;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.RED + "Players only.");
			return true;
		}
		if (!spawnRegionService.isInSpawn(player.getLocation())) {
			player.sendMessage(ChatColor.RED + "You can only use the tool vendor at spawn.");
			return true;
		}
		if (args.length == 0) {
			player.sendMessage(ChatColor.YELLOW + "Usage: /tool buy <type> <tier> [variant] | /tool repair");
			return true;
		}
		if (args[0].equalsIgnoreCase("repair")) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (!repairService.repair(player, item)) {
				player.sendMessage(ChatColor.RED + "Unable to repair. Hold a damaged tool and ensure banked funds.");
				return true;
			}
			player.sendMessage(ChatColor.GREEN + "Tool repaired.");
			return true;
		}
		if (args[0].equalsIgnoreCase("buy") && args.length >= 3) {
			ToolVendorService.ToolType type = parseType(args[1]);
			ToolVendorService.ToolTier tier = parseTier(args[2]);
			ToolVendorService.ToolVariant variant = args.length >= 4 ? parseVariant(args[3]) : ToolVendorService.ToolVariant.STANDARD;
			if (type == null || tier == null || variant == null) {
				player.sendMessage(ChatColor.RED + "Invalid tool selection.");
				return true;
			}
			if (!toolVendorService.isTierUnlocked(player, tier)) {
				player.sendMessage(ChatColor.RED + "That vendor tier is locked. Purchase the upgrade first.");
				return true;
			}
			if (!toolVendorService.purchaseTool(player, type, tier, variant)) {
				player.sendMessage(ChatColor.RED + "Not enough banked money.");
				return true;
			}
			player.sendMessage(ChatColor.GREEN + "Tool purchased.");
			return true;
		}
		player.sendMessage(ChatColor.YELLOW + "Usage: /tool buy <type> <tier> [variant] | /tool repair");
		return true;
	}

	private ToolVendorService.ToolType parseType(String input) {
		for (ToolVendorService.ToolType type : ToolVendorService.ToolType.values()) {
			if (type.name().equalsIgnoreCase(input)) {
				return type;
			}
		}
		return null;
	}

	private ToolVendorService.ToolTier parseTier(String input) {
		for (ToolVendorService.ToolTier tier : ToolVendorService.ToolTier.values()) {
			if (tier.name().equalsIgnoreCase(input)) {
				return tier;
			}
		}
		return null;
	}

	private ToolVendorService.ToolVariant parseVariant(String input) {
		for (ToolVendorService.ToolVariant variant : ToolVendorService.ToolVariant.values()) {
			if (variant.name().equalsIgnoreCase(input)) {
				return variant;
			}
		}
		return null;
	}
}
