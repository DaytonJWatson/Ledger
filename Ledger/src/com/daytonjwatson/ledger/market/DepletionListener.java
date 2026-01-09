package com.daytonjwatson.ledger.market;

import com.daytonjwatson.ledger.config.PriceTable;
import com.daytonjwatson.ledger.util.ItemKeyUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class DepletionListener implements Listener {
	private final MarketService marketService;

	public DepletionListener(MarketService marketService) {
		this.marketService = marketService;
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Material material = event.getBlock().getType();
		if (material == null || material == Material.AIR) {
			return;
		}
		String key = ItemKeyUtil.toKey(material);
		if (!isTracked(key)) {
			return;
		}
		marketService.recordMining(key, 1.0);
	}

	private boolean isTracked(String key) {
		PriceTable.PriceEntry entry = marketService.getPriceTable().getEntry(key);
		return entry != null && !entry.isUnsellable();
	}
}
