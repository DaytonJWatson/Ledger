package com.daytonjwatson.ledger.market;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.config.PriceTable;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class DepletionListener implements Listener {
	private final MarketService marketService;
	private final PriceTable priceTable;

	public DepletionListener(ConfigManager configManager, MarketService marketService) {
		this.marketService = marketService;
		this.priceTable = new PriceTable(configManager.getPrices());
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Material material = event.getBlock().getType();
		if (material == null || material == Material.AIR) {
			return;
		}
		String key = material.name();
		if (!isTracked(key, material)) {
			return;
		}
		marketService.recordMining(key, 1.0);
	}

	private boolean isTracked(String key, Material material) {
		PriceTable.PriceEntry entry = priceTable.getEntry(key);
		if (entry != null) {
			return true;
		}
		MarketItemTag tag = MarketItemTag.fromMaterial(material);
		return priceTable.getEntry(tag.toTagKey()) != null;
	}
}
