package com.daytonjwatson.ledger.economy;

import com.daytonjwatson.ledger.config.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathPenaltyListener implements Listener {
	private final ConfigManager configManager;
	private final MoneyService moneyService;

	public DeathPenaltyListener(ConfigManager configManager, MoneyService moneyService) {
		this.configManager = configManager;
		this.moneyService = moneyService;
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		moneyService.applyDeathLoss(event.getEntity());
	}
}
