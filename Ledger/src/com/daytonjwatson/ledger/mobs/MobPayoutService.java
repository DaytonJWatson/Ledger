package com.daytonjwatson.ledger.mobs;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.config.PriceTable;
import com.daytonjwatson.ledger.market.MarketState;
import com.daytonjwatson.ledger.market.ScarcityWindowService;
import org.bukkit.entity.Player;

public class MobPayoutService {
	private final ConfigManager configManager;
	private final MarketState marketState;
	private PriceTable priceTable;
	private final ScarcityWindowService scarcityWindowService;

	public MobPayoutService(ConfigManager configManager, MarketState marketState, ScarcityWindowService scarcityWindowService) {
		this.configManager = configManager;
		this.marketState = marketState;
		this.scarcityWindowService = scarcityWindowService;
		this.priceTable = new PriceTable(configManager.getPrices(), configManager.getOverrides());
	}

	public void reloadPrices() {
		this.priceTable = new PriceTable(configManager.getPrices(), configManager.getOverrides());
	}

	public double getPayout(Player player, String mobKey) {
		PriceTable.PriceEntry entry = priceTable.getEntry(mobKey);
		if (entry == null) {
			return 0.0;
		}
		MarketState.MobState state = marketState.getOrCreateMob(mobKey);
		decay(state);
		double supplyFactor = 1.0 / Math.pow(1.0 + (state.getKillAccumulator() / entry.getCap()), entry.getSigma());
		double windowMultiplier = scarcityWindowService.getWindowMultiplier(player, mobKey, ScarcityWindowService.WindowContext.MOB);
		double raw = entry.getBase() * supplyFactor * windowMultiplier;
		double clamped = clamp(raw, entry.getBase() * entry.getMinFactor(), entry.getBase() * entry.getMaxFactor() * windowMultiplier);
		return Math.max(0.0, clamped);
	}

	public void recordKill(String mobKey) {
		MarketState.MobState state = marketState.getOrCreateMob(mobKey);
		decay(state);
		state.setKillAccumulator(state.getKillAccumulator() + 1.0);
		state.setLastUpdate(System.currentTimeMillis());
	}

	private void decay(MarketState.MobState state) {
		long now = System.currentTimeMillis();
		double hours = Math.min(168.0, Math.max(0.0, (now - state.getLastUpdate()) / 3600000.0));
		if (hours <= 0.0) {
			return;
		}
		double halfLife = configManager.getConfig().getDouble("mob.halfLifeHours", 48.0);
		double lambda = Math.log(2) / halfLife;
		state.setKillAccumulator(state.getKillAccumulator() * Math.exp(-lambda * hours));
		state.setLastUpdate(now);
	}

	private double clamp(double value, double min, double max) {
		return Math.min(max, Math.max(min, value));
	}
}
