package com.daytonjwatson.ledger.spawn;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.mobs.MobPayoutService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class AnimalSellService {
	private final MobPayoutService mobPayoutService;
	private final MoneyService moneyService;
	private final String worldName;
	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxY;
	private final int maxZ;

	public AnimalSellService(ConfigManager configManager, MobPayoutService mobPayoutService, MoneyService moneyService) {
		this.mobPayoutService = mobPayoutService;
		this.moneyService = moneyService;
		this.worldName = configManager.getSpawn().getString("world", "world");
		this.minX = Math.min(configManager.getSpawn().getInt("animalPen.min.x"), configManager.getSpawn().getInt("animalPen.max.x"));
		this.minY = Math.min(configManager.getSpawn().getInt("animalPen.min.y"), configManager.getSpawn().getInt("animalPen.max.y"));
		this.minZ = Math.min(configManager.getSpawn().getInt("animalPen.min.z"), configManager.getSpawn().getInt("animalPen.max.z"));
		this.maxX = Math.max(configManager.getSpawn().getInt("animalPen.min.x"), configManager.getSpawn().getInt("animalPen.max.x"));
		this.maxY = Math.max(configManager.getSpawn().getInt("animalPen.min.y"), configManager.getSpawn().getInt("animalPen.max.y"));
		this.maxZ = Math.max(configManager.getSpawn().getInt("animalPen.min.z"), configManager.getSpawn().getInt("animalPen.max.z"));
	}

	public boolean isInPen(Location location) {
		if (location == null) {
			return false;
		}
		World world = location.getWorld();
		if (world == null || !world.getName().equalsIgnoreCase(worldName)) {
			return false;
		}
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}

	public boolean sellAnimal(Player player, EntityType type) {
		String key = "entity:" + type.name();
		double payout = mobPayoutService.getPayout(player, key);
		if (payout <= 0.0) {
			return false;
		}
		long earned = Math.round(payout);
		if (earned <= 0) {
			return false;
		}
		mobPayoutService.recordKill(key);
		moneyService.addCarried(player, earned);
		player.sendMessage(ChatColor.GREEN + "Animal sold for $" + earned + ".");
		return true;
	}
}
