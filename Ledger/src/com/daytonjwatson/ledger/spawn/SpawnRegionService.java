package com.daytonjwatson.ledger.spawn;

import com.daytonjwatson.ledger.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class SpawnRegionService {
	private final String worldName;
	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxY;
	private final int maxZ;

	public SpawnRegionService(ConfigManager configManager) {
		this.worldName = configManager.getSpawn().getString("world", "world");
		this.minX = Math.min(configManager.getSpawn().getInt("min.x"), configManager.getSpawn().getInt("max.x"));
		this.minY = Math.min(configManager.getSpawn().getInt("min.y"), configManager.getSpawn().getInt("max.y"));
		this.minZ = Math.min(configManager.getSpawn().getInt("min.z"), configManager.getSpawn().getInt("max.z"));
		this.maxX = Math.max(configManager.getSpawn().getInt("min.x"), configManager.getSpawn().getInt("max.x"));
		this.maxY = Math.max(configManager.getSpawn().getInt("min.y"), configManager.getSpawn().getInt("max.y"));
		this.maxZ = Math.max(configManager.getSpawn().getInt("min.z"), configManager.getSpawn().getInt("max.z"));
	}

	public boolean isInSpawn(Location location) {
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

	public World getWorld() {
		return Bukkit.getWorld(worldName);
	}
}
