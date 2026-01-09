package com.daytonjwatson.ledger.mobs;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.config.PriceTable;
import com.daytonjwatson.ledger.market.MarketState;
import com.daytonjwatson.ledger.market.ScarcityWindowService;
import com.daytonjwatson.ledger.upgrades.UpgradeService;
import org.bukkit.entity.Player;

import java.util.Locale;

public class MobPayoutService {
	private final ConfigManager configManager;
	private final MarketState marketState;
	private PriceTable priceTable;
	private final ScarcityWindowService scarcityWindowService;
	private final UpgradeService upgradeService;

	public MobPayoutService(ConfigManager configManager, MarketState marketState, ScarcityWindowService scarcityWindowService, UpgradeService upgradeService) {
		this.configManager = configManager;
		this.marketState = marketState;
		this.scarcityWindowService = scarcityWindowService;
		this.upgradeService = upgradeService;
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
		return Math.max(0.0, applySpecializationMultiplier(player, clamped));
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

	private double applySpecializationMultiplier(Player player, double payout) {
		if (player == null) {
			return payout;
		}
		String specialization = upgradeService.getSpecializationChoice(player.getUniqueId());
		if (specialization == null || specialization.isBlank()) {
			return payout;
		}
		int level = getSpecializationLevel(player.getUniqueId(), specialization);
		if (level <= 0) {
			return payout;
		}
		if ("HUNTER".equalsIgnoreCase(specialization)) {
			return payout * (1.0 + (0.03 * level));
		}
		return payout * (1.0 - (0.015 * level));
	}

	private int getSpecializationLevel(java.util.UUID uuid, String specialization) {
		if (specialization == null) {
			return 0;
		}
		return switch (specialization.toUpperCase(Locale.ROOT)) {
			case "MINER" -> upgradeService.getLevel(uuid, "spec_miner");
			case "FARMER" -> upgradeService.getLevel(uuid, "spec_farmer");
			case "HUNTER" -> upgradeService.getLevel(uuid, "spec_hunter");
			default -> 0;
		};
	}
}
