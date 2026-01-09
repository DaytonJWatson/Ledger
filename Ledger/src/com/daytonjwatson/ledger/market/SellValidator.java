package com.daytonjwatson.ledger.market;

import com.daytonjwatson.ledger.config.PriceTable;
import com.daytonjwatson.ledger.tools.ToolMetaService;
import com.daytonjwatson.ledger.util.ItemKeyUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SellValidator {
	private final MarketService marketService;
	private final ToolMetaService toolMetaService;

	public SellValidator(MarketService marketService, ToolMetaService toolMetaService) {
		this.marketService = marketService;
		this.toolMetaService = toolMetaService;
	}

	public SellResult validate(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return SellResult.reject("Empty");
		}
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			if (hasEnchants(meta) && !isWhitelistedTool(item)) {
				return SellResult.reject("Enchanted");
			}
			if (meta.hasDisplayName()) {
				return SellResult.reject("Custom Name");
			}
			if (hasCustomLore(meta)) {
				return SellResult.reject("Custom Lore");
			}
			if (meta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof ShulkerBox box) {
				if (!box.getInventory().isEmpty()) {
					return SellResult.reject("Container Contents");
				}
			}
			if (meta instanceof BundleMeta bundleMeta && !bundleMeta.getItems().isEmpty()) {
				return SellResult.reject("Container Contents");
			}
		}
		String key = ItemKeyUtil.toKey(item.getType());
		PriceTable.PriceEntry entry = marketService.getPriceTable().getEntry(key);
		if (entry == null || entry.isUnsellable() || entry.getBase() <= 0.0) {
			return SellResult.reject("Unsellable");
		}
		return SellResult.allowed();
	}

	private boolean hasEnchants(ItemMeta meta) {
		return meta.hasEnchants();
	}

	private boolean hasCustomLore(ItemMeta meta) {
		List<String> lore = meta.getLore();
		if (lore == null || lore.isEmpty()) {
			return false;
		}
		for (String line : lore) {
			String stripped = ChatColor.stripColor(line);
			if (stripped == null) {
				return true;
			}
			if (!stripped.startsWith("Value:")) {
				return true;
			}
		}
		return false;
	}

	private boolean isWhitelistedTool(ItemStack item) {
		if (toolMetaService == null) {
			return false;
		}
		return toolMetaService.hasRepairData(item);
	}

	public record SellResult(boolean sellable, String reason) {
		public static SellResult allowed() {
			return new SellResult(true, "");
		}

		public static SellResult reject(String reason) {
			return new SellResult(false, reason);
		}
	}
}
