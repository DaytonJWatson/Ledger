package com.daytonjwatson.ledger.market;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.util.AtomicFileWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public class MarketStorageYaml {
	private static final int SCHEMA_VERSION = 1;
	private final JavaPlugin plugin;
	private final MarketState marketState;
	private final ConfigManager configManager;
	private final File marketFile;

	public MarketStorageYaml(JavaPlugin plugin, MarketState marketState, ConfigManager configManager) {
		this.plugin = plugin;
		this.marketState = marketState;
		this.configManager = configManager;
		this.marketFile = new File(plugin.getDataFolder(), "market.yml");
	}

	public void load() {
		YamlConfiguration yaml = loadWithBackup();
		if (yaml == null) {
			return;
		}
		ConfigurationSection itemsSection = yaml.getConfigurationSection("market.items");
		if (itemsSection != null) {
			for (String key : itemsSection.getKeys(false)) {
				ConfigurationSection entry = itemsSection.getConfigurationSection(key);
				if (entry == null) {
					continue;
				}
				MarketState.ItemState state = marketState.getOrCreateItem(key);
				state.setSoldAccumulator(entry.getDouble("s", 0.0));
				state.setLastUpdate(entry.getLong("lastUpdate", System.currentTimeMillis()));
				state.setMinedTotal(entry.getDouble("minedTotal", 0.0));
			}
		}
		ConfigurationSection mobSection = yaml.getConfigurationSection("mobMarket.mobs");
		if (mobSection != null) {
			for (String key : mobSection.getKeys(false)) {
				ConfigurationSection entry = mobSection.getConfigurationSection(key);
				if (entry == null) {
					continue;
				}
				MarketState.MobState state = marketState.getOrCreateMob(key);
				state.setKillAccumulator(entry.getDouble("k", 0.0));
				state.setLastUpdate(entry.getLong("lastUpdate", System.currentTimeMillis()));
			}
		}
		applyDowntimeDecay();
	}

	public void save() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("schema", SCHEMA_VERSION);
		yaml.set("generatedAt", System.currentTimeMillis());
		ConfigurationSection itemsSection = yaml.createSection("market.items");
		for (Map.Entry<String, MarketState.ItemState> entry : marketState.getItems().entrySet()) {
			ConfigurationSection itemSection = itemsSection.createSection(entry.getKey());
			itemSection.set("s", entry.getValue().getSoldAccumulator());
			itemSection.set("lastUpdate", entry.getValue().getLastUpdate());
			itemSection.set("minedTotal", entry.getValue().getMinedTotal());
		}
		ConfigurationSection mobSection = yaml.createSection("mobMarket.mobs");
		for (Map.Entry<String, MarketState.MobState> entry : marketState.getMobs().entrySet()) {
			ConfigurationSection mobEntry = mobSection.createSection(entry.getKey());
			mobEntry.set("k", entry.getValue().getKillAccumulator());
			mobEntry.set("lastUpdate", entry.getValue().getLastUpdate());
		}
		try {
			AtomicFileWriter.writeAtomically(marketFile, yaml.saveToString().getBytes());
		} catch (IOException e) {
			plugin.getLogger().warning("Failed to save market.yml: " + e.getMessage());
		}
	}

	private YamlConfiguration loadWithBackup() {
		File tmp = new File(marketFile.getParentFile(), marketFile.getName() + ".tmp");
		if (tmp.exists() && !tmp.delete()) {
			plugin.getLogger().warning("Unable to delete stale market temp file: " + tmp.getName());
		}
		YamlConfiguration primary = loadConfig(marketFile);
		if (primary != null && !primary.getKeys(false).isEmpty()) {
			return primary;
		}
		File backup = new File(marketFile.getParentFile(), marketFile.getName() + ".bak");
		YamlConfiguration backupConfig = loadConfig(backup);
		if (backupConfig != null) {
			return backupConfig;
		}
		return primary != null ? primary : new YamlConfiguration();
	}

	private YamlConfiguration loadConfig(File file) {
		if (!file.exists()) {
			return null;
		}
		try {
			String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			YamlConfiguration yaml = new YamlConfiguration();
			yaml.loadFromString(content);
			return yaml;
		} catch (IOException | InvalidConfigurationException e) {
			plugin.getLogger().warning("Failed to load " + file.getName() + ": " + e.getMessage());
			return null;
		}
	}

	private void applyDowntimeDecay() {
		double halfLife = configManager.getConfig().getDouble("market.halfLifeHours", 72.0);
		long now = System.currentTimeMillis();
		for (MarketState.ItemState state : marketState.getItems().values()) {
			decay(state, halfLife, now);
		}
		for (MarketState.MobState state : marketState.getMobs().values()) {
			decay(state, configManager.getConfig().getDouble("mob.halfLifeHours", 48.0), now);
		}
	}

	private void decay(MarketState.ItemState state, double halfLifeHours, long now) {
		double hours = Math.min(168.0, Math.max(0.0, (now - state.getLastUpdate()) / 3600000.0));
		if (hours <= 0.0) {
			return;
		}
		double lambda = Math.log(2) / halfLifeHours;
		state.setSoldAccumulator(state.getSoldAccumulator() * Math.exp(-lambda * hours));
		state.setLastUpdate(now);
	}

	private void decay(MarketState.MobState state, double halfLifeHours, long now) {
		double hours = Math.min(168.0, Math.max(0.0, (now - state.getLastUpdate()) / 3600000.0));
		if (hours <= 0.0) {
			return;
		}
		double lambda = Math.log(2) / halfLifeHours;
		state.setKillAccumulator(state.getKillAccumulator() * Math.exp(-lambda * hours));
		state.setLastUpdate(now);
	}
}
