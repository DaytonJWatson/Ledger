package com.daytonjwatson.ledger.market;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.config.PriceTable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MarketService {
	private final ConfigManager configManager;
	private final MarketState marketState;
	private final PriceTable priceTable;
	private final Map<String, Double> priceCache = new HashMap<>();
	private long marketVersion = 0;
	private long priceCacheVersion = -1;

	public MarketService(ConfigManager configManager, MarketState marketState) {
		this.configManager = configManager;
		this.marketState = marketState;
		this.priceTable = new PriceTable(configManager.getPrices());
	}

	public double getSellPrice(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return 0.0;
		}
		return getSellPrice(item.getType().name());
	}

	public double getSellPrice(String key) {
		if (key == null) {
			return 0.0;
		}
		String normalized = key.toUpperCase();
		PriceTable.PriceEntry entry = priceTable.getEntry(normalized);
		if (entry == null || entry.getBase() <= 0.0) {
			return 0.0;
		}
		if (priceCacheVersion != marketVersion) {
			priceCache.clear();
			priceCacheVersion = marketVersion;
		}
		return priceCache.computeIfAbsent(normalized, ignored -> computePrice(entry, normalized));
	}

	public double sell(ItemStack item, int quantity) {
		if (item == null || item.getType() == Material.AIR || quantity <= 0) {
			return 0.0;
		}
		double price = getSellPrice(item.getType().name());
		if (price <= 0.0) {
			return 0.0;
		}
		applySale(item.getType().name(), quantity);
		return price * quantity;
	}

	public void applySale(String key, int quantity) {
		MarketState.ItemState state = marketState.getOrCreateItem(key);
		decay(state);
		state.setSoldAccumulator(state.getSoldAccumulator() + quantity);
		state.setLastUpdate(System.currentTimeMillis());
		marketVersion++;
	}

	private double computePrice(PriceTable.PriceEntry entry, String key) {
		MarketState.ItemState state = marketState.getOrCreateItem(key);
		decay(state);
		double supplyFactor = 1.0 / Math.pow(1.0 + (state.getSoldAccumulator() / entry.getCap()), entry.getSigma());
		double minFactor = entry.getMinFactor();
		double maxFactor = entry.getMaxFactor();
		double raw = entry.getBase() * supplyFactor;
		double clamped = clamp(raw, entry.getBase() * minFactor, entry.getBase() * maxFactor);
		return Math.max(0.0, clamped);
	}

	private void decay(MarketState.ItemState state) {
		long now = System.currentTimeMillis();
		double hours = Math.min(168.0, Math.max(0.0, (now - state.getLastUpdate()) / 3600000.0));
		if (hours <= 0.0) {
			return;
		}
		double halfLife = configManager.getConfig().getDouble("market.halfLifeHours", 72.0);
		double lambda = Math.log(2) / halfLife;
		state.setSoldAccumulator(state.getSoldAccumulator() * Math.exp(-lambda * hours));
		state.setLastUpdate(now);
	}

	private double clamp(double value, double min, double max) {
		return Math.min(max, Math.max(min, value));
	}
}
