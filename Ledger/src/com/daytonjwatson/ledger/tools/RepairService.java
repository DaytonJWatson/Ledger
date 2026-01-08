package com.daytonjwatson.ledger.tools;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.economy.MoneyService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class RepairService {
	private final MoneyService moneyService;
	private final ToolMetaService toolMetaService;
	private final double repairBase;
	private final double repairCountFactor;
	private final double repairCap;
	private final double globalBase;
	private final ToolVendorService toolVendorService;

	public RepairService(ConfigManager configManager, MoneyService moneyService, ToolMetaService toolMetaService) {
		this.moneyService = moneyService;
		this.toolMetaService = toolMetaService;
		this.repairBase = configManager.getConfig().getDouble("tools.repairBase", 0.10);
		this.repairCountFactor = configManager.getConfig().getDouble("tools.repairCountFactor", 0.04);
		this.repairCap = configManager.getConfig().getDouble("tools.repairCap", 0.60);
		this.globalBase = configManager.getConfig().getDouble("tools.globalBase", 8000.0);
		this.toolVendorService = new ToolVendorService(configManager, moneyService);
	}

	public long getRepairCost(ItemStack item) {
		ToolVendorService.ToolTier tier = ToolParser.getTier(item.getType());
		ToolVendorService.ToolType type = ToolParser.getType(item.getType());
		if (tier == null || type == null) {
			return -1;
		}
		int repairCount = toolMetaService.getRepairCount(item);
		double remaining = getRemainingDurability(item);
		if (remaining <= 0.0) {
			return -1;
		}
		long buyPrice = toolVendorService.getBuyPrice(type, tier, ToolVendorService.ToolVariant.STANDARD);
		double cost = buyPrice * repairBase * (1.0 - remaining) * (1.0 + repairCountFactor * repairCount);
		double cap = buyPrice * repairCap;
		return Math.round(Math.min(cost, cap));
	}

	public boolean repair(Player player, ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}
		long cost = getRepairCost(item);
		if (cost < 0) {
			return false;
		}
		if (!moneyService.removeBanked(player, cost)) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta instanceof Damageable damageable) {
			damageable.setDamage(0);
			item.setItemMeta(damageable);
		}
		toolMetaService.incrementRepairCount(item);
		return true;
	}

	private double getRemainingDurability(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (!(meta instanceof Damageable damageable)) {
			return 1.0;
		}
		int max = item.getType().getMaxDurability();
		if (max <= 0) {
			return 1.0;
		}
		return Math.max(0.0, 1.0 - ((double) damageable.getDamage() / max));
	}
}
