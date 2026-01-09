package com.daytonjwatson.ledger.farming;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.util.AtomicFileWriter;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoilFatigueService {
	private static final String ROOT = "soilFatigue.entries";
	private static final String FATIGUE_LORE_PREFIX = "Soil fatigue:";
	private final JavaPlugin plugin;
	private final ConfigManager configManager;
	private final NamespacedKey fatigueKey;
	private final Map<SoilKey, SoilEntry> entries = new HashMap<>();
	private final File fatigueFile;

	public SoilFatigueService(JavaPlugin plugin, ConfigManager configManager) {
		this.plugin = plugin;
		this.configManager = configManager;
		this.fatigueKey = new NamespacedKey(plugin, "soil_fatigue_multiplier");
		this.fatigueFile = new File(plugin.getDataFolder(), "soil-fatigue.yml");
	}

	public void load() {
		if (!fatigueFile.exists()) {
			return;
		}
		try {
			String content = Files.readString(fatigueFile.toPath(), StandardCharsets.UTF_8);
			YamlConfiguration yaml = new YamlConfiguration();
			yaml.loadFromString(content);
			ConfigurationSection root = yaml.getConfigurationSection(ROOT);
			if (root == null) {
				return;
			}
			for (String key : root.getKeys(false)) {
				ConfigurationSection entry = root.getConfigurationSection(key);
				if (entry == null) {
					continue;
				}
				SoilKey soilKey = SoilKey.parse(key);
				if (soilKey == null) {
					continue;
				}
				SoilEntry state = new SoilEntry();
				state.fatigue = entry.getDouble("f", 0.0);
				state.lastUpdate = entry.getLong("lastUpdate", System.currentTimeMillis());
				state.lastTouched = entry.getLong("lastTouched", state.lastUpdate);
				entries.put(soilKey, state);
			}
			prune(System.currentTimeMillis());
		} catch (IOException | InvalidConfigurationException e) {
			plugin.getLogger().warning("Failed to load soil-fatigue.yml: " + e.getMessage());
		}
	}

	public void save() {
		long now = System.currentTimeMillis();
		prune(now);
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("generatedAt", now);
		ConfigurationSection root = yaml.createSection(ROOT);
		for (Map.Entry<SoilKey, SoilEntry> entry : entries.entrySet()) {
			ConfigurationSection section = root.createSection(entry.getKey().toKey());
			section.set("f", entry.getValue().fatigue);
			section.set("lastUpdate", entry.getValue().lastUpdate);
			section.set("lastTouched", entry.getValue().lastTouched);
		}
		try {
			AtomicFileWriter.writeAtomically(fatigueFile, yaml.saveToString().getBytes());
		} catch (IOException e) {
			plugin.getLogger().warning("Failed to save soil-fatigue.yml: " + e.getMessage());
		}
	}

	public double recordHarvest(Block farmlandBlock) {
		if (farmlandBlock == null || farmlandBlock.getWorld() == null) {
			return 1.0;
		}
		long now = System.currentTimeMillis();
		double perHarvest = configManager.getConfig().getDouble("farming.fatigue.perHarvest", 0.12);
		SoilKey key = SoilKey.fromBlock(farmlandBlock);
		SoilEntry entry = entries.computeIfAbsent(key, ignored -> new SoilEntry());
		double fatigue = applyRecovery(entry, now);
		fatigue = clamp(fatigue + Math.max(0.0, perHarvest), 0.0, 1.0);
		entry.fatigue = fatigue;
		entry.lastUpdate = now;
		entry.lastTouched = now;
		return toMultiplier(fatigue);
	}

	public void tagHarvestedItem(ItemStack item, double multiplier) {
		if (item == null) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}
		double normalizedMultiplier = normalizeMultiplier(multiplier);
		PersistentDataContainer container = meta.getPersistentDataContainer();
		container.set(fatigueKey, PersistentDataType.DOUBLE, normalizedMultiplier);
		updateFatigueLore(meta, normalizedMultiplier);
		item.setItemMeta(meta);
	}

	public double getMultiplier(ItemStack item) {
		if (item == null) {
			return 1.0;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return 1.0;
		}
		PersistentDataContainer container = meta.getPersistentDataContainer();
		Double value = container.get(fatigueKey, PersistentDataType.DOUBLE);
		if (value == null) {
			return 1.0;
		}
		return value;
	}

	private double applyRecovery(SoilEntry entry, long now) {
		if (entry.lastUpdate <= 0) {
			entry.lastUpdate = now;
			return entry.fatigue;
		}
		double hours = Math.max(0.0, (now - entry.lastUpdate) / 3600000.0);
		if (hours <= 0.0) {
			return entry.fatigue;
		}
		double recoveryPerDay = Math.max(0.0, configManager.getConfig().getDouble("farming.fatigue.recoveryPerDay", 0.18));
		double recoveryPerHour = recoveryPerDay / 24.0;
		entry.fatigue = Math.max(0.0, entry.fatigue - recoveryPerHour * hours);
		entry.lastUpdate = now;
		return entry.fatigue;
	}

	private void prune(long now) {
		double pruneDays = Math.max(1.0, configManager.getConfig().getDouble("farming.fatigue.pruneDays", 14.0));
		long pruneMs = (long) (pruneDays * 86400000L);
		for (Map.Entry<SoilKey, SoilEntry> entry : entries.entrySet()) {
			SoilEntry state = entry.getValue();
			applyRecovery(state, now);
		}
		entries.entrySet().removeIf(entry -> entry.getValue().fatigue <= 0.0
			&& now - entry.getValue().lastTouched > pruneMs);
	}

	private double toMultiplier(double fatigue) {
		double minMultiplier = clamp(configManager.getConfig().getDouble("farming.fatigue.minMultiplier", 0.25), 0.0, 1.0);
		return clamp(1.0 - fatigue, minMultiplier, 1.0);
	}

	private double normalizeMultiplier(double multiplier) {
		double step = configManager.getConfig().getDouble("farming.fatigue.stackStep", 0.05);
		if (step <= 0.0) {
			return multiplier;
		}
		double normalized = Math.round(multiplier / step) * step;
		normalized = clamp(normalized, 0.0, 1.0);
		return Math.round(normalized * 1000.0) / 1000.0;
	}

	private double clamp(double value, double min, double max) {
		return Math.min(max, Math.max(min, value));
	}

	private void updateFatigueLore(ItemMeta meta, double normalizedMultiplier) {
		double fatigue = clamp(1.0 - normalizedMultiplier, 0.0, 1.0);
		int percent = (int) Math.round(fatigue * 100.0);
		String display = ChatColor.GRAY + FATIGUE_LORE_PREFIX + " " + ChatColor.YELLOW + percent + "%";

		java.util.List<String> lore = meta.getLore();
		if (lore == null) {
			lore = new java.util.ArrayList<>();
		} else {
			lore = new java.util.ArrayList<>(lore);
		}
		lore.removeIf(line -> {
			String stripped = ChatColor.stripColor(line);
			return stripped != null && stripped.startsWith(FATIGUE_LORE_PREFIX);
		});
		lore.add(display);
		meta.setLore(lore);
	}

	private record SoilKey(UUID worldId, int x, int y, int z) {
		private static SoilKey fromBlock(Block block) {
			World world = block.getWorld();
			return new SoilKey(world.getUID(), block.getX(), block.getY(), block.getZ());
		}

		private String toKey() {
			return worldId + ":" + x + ":" + y + ":" + z;
		}

		private static SoilKey parse(String raw) {
			if (raw == null || raw.isBlank()) {
				return null;
			}
			String[] parts = raw.split(":");
			if (parts.length != 4) {
				return null;
			}
			try {
				UUID world = UUID.fromString(parts[0]);
				int x = Integer.parseInt(parts[1]);
				int y = Integer.parseInt(parts[2]);
				int z = Integer.parseInt(parts[3]);
				return new SoilKey(world, x, y, z);
			} catch (IllegalArgumentException ex) {
				return null;
			}
		}
	}

	private static class SoilEntry {
		private double fatigue;
		private long lastUpdate;
		private long lastTouched;
	}
}
