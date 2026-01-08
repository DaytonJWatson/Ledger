package com.daytonjwatson.ledger.market;

import com.daytonjwatson.ledger.config.ConfigManager;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ScarcityWindowService {
	private final ConfigManager configManager;

	public ScarcityWindowService(ConfigManager configManager) {
		this.configManager = configManager;
	}

	public double getWindowMultiplier(Player player, String key, WindowContext context) {
		if (player == null) {
			return 1.0;
		}
		MarketItemTag tag = MarketItemTag.fromKey(key);
		World world = player.getWorld();
		double multiplier = 1.0;
		if (tag == MarketItemTag.MOB && isNight(world)) {
			multiplier *= configManager.getConfig().getDouble("market.windows.nightMultiplier", 1.25);
		}
		if (tag == MarketItemTag.CROP && world.hasStorm()) {
			multiplier *= configManager.getConfig().getDouble("market.windows.rainMultiplier", 1.20);
		}
		if (tag == MarketItemTag.ORE && player.getLocation().getBlockY() <= getDepthThreshold()) {
			multiplier *= configManager.getConfig().getDouble("market.windows.depthMultiplier", 1.30);
		}
		return multiplier;
	}

	private int getDepthThreshold() {
		return configManager.getConfig().getInt("market.windows.depthY", 32);
	}

	private boolean isNight(World world) {
		long time = world.getTime();
		return time >= 13000 && time <= 23000;
	}

	public enum WindowContext {
		MARKET,
		MOB
	}
}
