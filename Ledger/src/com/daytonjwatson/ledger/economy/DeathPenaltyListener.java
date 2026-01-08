package com.daytonjwatson.ledger.economy;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.upgrades.UpgradeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathPenaltyListener implements Listener {
	private final ConfigManager configManager;
	private final MoneyService moneyService;
	private final UpgradeService upgradeService;

	public DeathPenaltyListener(ConfigManager configManager, MoneyService moneyService, UpgradeService upgradeService) {
		this.configManager = configManager;
		this.moneyService = moneyService;
		this.upgradeService = upgradeService;
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		double baseLoss = configManager.getConfig().getDouble("economy.loss.base", 0.30);
		double floor = configManager.getConfig().getDouble("economy.loss.floor", 0.10);
		int insuranceLevel = upgradeService.getLevel(event.getEntity().getUniqueId(), "insurance");
		double adjusted = baseLoss - (0.02 * insuranceLevel);
		double effectiveLoss = Math.max(floor, adjusted);
		moneyService.applyDeathLoss(event.getEntity(), effectiveLoss);
	}
}
