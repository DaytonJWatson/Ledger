package com.daytonjwatson.ledger.items;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryScanScheduler {
	private final JavaPlugin plugin;
	private final LoreValueService loreValueService;

	public InventoryScanScheduler(JavaPlugin plugin, LoreValueService loreValueService) {
		this.plugin = plugin;
		this.loreValueService = loreValueService;
	}

	public void start() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				loreValueService.updatePlayerInventory(player);
			}
		}, 40L, 80L);
	}
}
