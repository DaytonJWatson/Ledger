package com.daytonjwatson.ledger.economy;

import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import org.bukkit.entity.Player;

public class BankService {
	private final SpawnRegionService spawnRegionService;
	private final MoneyService moneyService;

	public BankService(SpawnRegionService spawnRegionService, MoneyService moneyService) {
		this.spawnRegionService = spawnRegionService;
		this.moneyService = moneyService;
	}

	public boolean deposit(Player player, long amount) {
		if (!spawnRegionService.isInSpawn(player.getLocation())) {
			return false;
		}
		long carried = moneyService.getCarried(player.getUniqueId());
		long depositAmount = amount >= 0 ? Math.min(amount, carried) : carried;
		if (depositAmount <= 0) {
			return false;
		}
		moneyService.removeCarried(player, depositAmount);
		moneyService.addBanked(player, depositAmount);
		return true;
	}
}
