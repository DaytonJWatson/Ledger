package com.daytonjwatson.ledger.gui;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.config.PriceBandTable;
import com.daytonjwatson.ledger.config.PriceGenerator;
import com.daytonjwatson.ledger.config.PriceTable;
import com.daytonjwatson.ledger.market.ItemTagService;
import com.daytonjwatson.ledger.market.MarketService;
import com.daytonjwatson.ledger.market.PriceBandTag;
import com.daytonjwatson.ledger.mobs.MobPayoutService;
import com.daytonjwatson.ledger.util.ItemKeyUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class HubCommand implements CommandExecutor {
	private final GuiManager guiManager;
	private final ConfigManager configManager;
	private final MarketService marketService;
	private final MobPayoutService mobPayoutService;

	public HubCommand(GuiManager guiManager, ConfigManager configManager, MarketService marketService, MobPayoutService mobPayoutService) {
		this.guiManager = guiManager;
		this.configManager = configManager;
		this.marketService = marketService;
		this.mobPayoutService = mobPayoutService;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			if (!(sender instanceof Player player)) {
				sender.sendMessage(ChatColor.RED + "Players only.");
				return true;
			}
			guiManager.open(MenuId.HUB, player);
			return true;
		}

		String subcommand = args[0].toLowerCase();
		return switch (subcommand) {
			case "genprices" -> handleGenerate(sender);
			case "reloadprices" -> handleReload(sender);
			case "price" -> handlePriceLookup(sender, args);
			case "gensample" -> handleSample(sender);
			default -> {
				sender.sendMessage(ChatColor.YELLOW + "Usage: /ledger [genprices|reloadprices|price|gensample]");
				yield true;
			}
		};
	}

	private boolean handleGenerate(CommandSender sender) {
		if (!isAdmin(sender)) {
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}
		File pricesFile = new File(configManager.getDataFolder(), "prices.yml");
		PriceBandTable bandTable = new PriceBandTable(configManager.getPriceBands());
		PriceGenerator generator = new PriceGenerator(new ItemTagService(), bandTable);
		try {
			generator.write(pricesFile, configManager.getPrices());
		} catch (IOException e) {
			sender.sendMessage(ChatColor.RED + "Failed to generate prices.yml: " + e.getMessage());
			return true;
		}
		configManager.reloadPrices();
		marketService.reloadPrices();
		mobPayoutService.reloadPrices();
		sender.sendMessage(ChatColor.GREEN + "Generated prices.yml.");
		return true;
	}

	private boolean handleReload(CommandSender sender) {
		if (!isAdmin(sender)) {
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}
		configManager.reloadPrices();
		marketService.reloadPrices();
		mobPayoutService.reloadPrices();
		sender.sendMessage(ChatColor.GREEN + "Reloaded price configuration.");
		return true;
	}

	private boolean handlePriceLookup(CommandSender sender, String[] args) {
		Material material = null;
		if (args.length >= 2) {
			material = Material.matchMaterial(args[1]);
		}
		if (material == null && sender instanceof Player player) {
			material = player.getInventory().getItemInMainHand().getType();
		}
		if (material == null || material == Material.AIR) {
			sender.sendMessage(ChatColor.RED + "Provide a material or hold one.");
			return true;
		}
		String key = ItemKeyUtil.toKey(material);
		PriceTable.PriceEntry entry = marketService.getPriceTable().getEntry(key);
		if (entry == null) {
			sender.sendMessage(ChatColor.RED + "No price entry for " + key + ".");
			return true;
		}
		double current = marketService.getSellPrice(key);
		String tag = entry.getTag() == null || entry.getTag().isEmpty() ? new ItemTagService().getBaseTag(material).name() : entry.getTag();
		sender.sendMessage(ChatColor.YELLOW + "Price: " + key);
		sender.sendMessage(ChatColor.GRAY + "Tag: " + ChatColor.WHITE + tag);
		sender.sendMessage(ChatColor.GRAY + "Base: " + ChatColor.GOLD + entry.getBase());
		sender.sendMessage(ChatColor.GRAY + "Cap: " + ChatColor.WHITE + entry.getCap());
		sender.sendMessage(ChatColor.GRAY + "Sigma: " + ChatColor.WHITE + entry.getSigma());
		sender.sendMessage(ChatColor.GRAY + "Current: " + ChatColor.GREEN + String.format("%.2f", current));
		return true;
	}

	private boolean handleSample(CommandSender sender) {
		if (!isAdmin(sender)) {
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}
		PriceGenerator generator = new PriceGenerator(new ItemTagService(), new PriceBandTable(configManager.getPriceBands()));
		Map<PriceBandTag, PriceGenerator.PriceSummary> summary = new TreeMap<>(generator.summarize(configManager.getPrices()));
		for (Map.Entry<PriceBandTag, PriceGenerator.PriceSummary> entry : summary.entrySet()) {
			PriceGenerator.PriceSummary stats = entry.getValue();
			if (stats.getCount() == 0) {
				continue;
			}
			sender.sendMessage(ChatColor.GRAY + entry.getKey().name() + " -> "
				+ "min " + String.format("%.2f", stats.getMin())
				+ ", max " + String.format("%.2f", stats.getMax())
				+ ", avg " + String.format("%.2f", stats.getAverage())
				+ " (" + stats.getCount() + ")");
		}
		return true;
	}

	private boolean isAdmin(CommandSender sender) {
		return sender.isOp() || sender.hasPermission("ledger.admin");
	}
}
