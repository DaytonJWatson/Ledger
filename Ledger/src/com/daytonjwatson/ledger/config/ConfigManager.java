package com.daytonjwatson.ledger.config;

import com.daytonjwatson.ledger.util.AtomicFileWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
	private final JavaPlugin plugin;
	private YamlConfiguration config;
	private YamlConfiguration prices;
	private YamlConfiguration upgrades;
	private YamlConfiguration spawn;

	public ConfigManager(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public void loadAll() {
		AtomicFileWriter.ensureDirectory(plugin.getDataFolder());
		this.config = loadOrCreate(new File(plugin.getDataFolder(), "config.yml"), this::writeDefaultConfig);
		this.prices = loadOrCreate(new File(plugin.getDataFolder(), "prices.yml"), this::writeDefaultPrices);
		this.upgrades = loadOrCreate(new File(plugin.getDataFolder(), "upgrades.yml"), this::writeDefaultUpgrades);
		this.spawn = loadOrCreate(new File(plugin.getDataFolder(), "spawn.yml"), this::writeDefaultSpawn);
	}

	public YamlConfiguration getConfig() {
		return config;
	}

	public YamlConfiguration getPrices() {
		return prices;
	}

	public YamlConfiguration getUpgrades() {
		return upgrades;
	}

	public YamlConfiguration getSpawn() {
		return spawn;
	}

	private YamlConfiguration loadOrCreate(File file, DefaultWriter writer) {
		if (!file.exists()) {
			try {
				writer.write(file);
			} catch (IOException e) {
				plugin.getLogger().warning("Failed to write defaults for " + file.getName() + ": " + e.getMessage());
			}
		}
		return YamlConfiguration.loadConfiguration(file);
	}

	private void writeDefaultConfig(File file) throws IOException {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("economy.loss.base", 0.30);
		yaml.set("economy.loss.floor", 0.10);
		yaml.set("market.sigma", 1.0);
		yaml.set("market.halfLifeHours", 72.0);
		yaml.set("market.minFactor", 0.20);
		yaml.set("market.maxFactor", 2.50);
		yaml.set("tools.globalBase", 8000.0);
		yaml.set("tools.repairCap", 0.60);
		yaml.set("tools.repairCountFactor", 0.04);
		yaml.set("tools.repairBase", 0.10);
		yaml.set("mob.sigma", 1.0);
		yaml.set("mob.halfLifeHours", 48.0);
		yaml.set("mob.spawnerMultiplier", 0.20);
		yaml.set("mob.sameChunkMultiplier", 0.60);
		yaml.set("mob.sameChunkWindowSeconds", 45);
		writeYaml(file, yaml);
	}

	private void writeDefaultPrices(File file) throws IOException {
		YamlConfiguration yaml = new YamlConfiguration();
		ConfigurationSection pricesSection = yaml.createSection("prices");
		createPrice(pricesSection, "STONE", 1.0, 10000);
		createPrice(pricesSection, "COBBLESTONE", 0.7, 10000);
		createPrice(pricesSection, "IRON_INGOT", 25.0, 5000);
		createPrice(pricesSection, "GOLD_INGOT", 40.0, 4000);
		createPrice(pricesSection, "DIAMOND", 250.0, 1000);
		createPrice(pricesSection, "NETHERITE_INGOT", 1200.0, 500);
		createPrice(pricesSection, "WHEAT", 4.0, 8000);
		createPrice(pricesSection, "CARROT", 3.5, 8000);
		createPrice(pricesSection, "POTATO", 3.5, 8000);
		createPrice(pricesSection, "BEEF", 8.0, 6000);
		createPrice(pricesSection, "PORKCHOP", 8.0, 6000);
		createPrice(pricesSection, "CHICKEN", 6.0, 6000);
		ConfigurationSection mobsSection = yaml.createSection("mobPrices");
		createPrice(mobsSection, "entity:ZOMBIE", 12.0, 5000);
		createPrice(mobsSection, "entity:SKELETON", 14.0, 5000);
		createPrice(mobsSection, "entity:CREEPER", 20.0, 4000);
		createPrice(mobsSection, "entity:SPIDER", 10.0, 5000);
		writeYaml(file, yaml);
	}

	private void createPrice(ConfigurationSection section, String key, double base, double cap) {
		ConfigurationSection entry = section.createSection(key);
		entry.set("base", base);
		entry.set("cap", cap);
		entry.set("minFactor", 0.20);
		entry.set("maxFactor", 2.50);
		entry.set("sigma", 1.0);
	}

	private void writeDefaultUpgrades(File file) throws IOException {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("upgrades", null);
		writeYaml(file, yaml);
	}

	private void writeDefaultSpawn(File file) throws IOException {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("world", "world");
		yaml.set("min.x", -10);
		yaml.set("min.y", 0);
		yaml.set("min.z", -10);
		yaml.set("max.x", 10);
		yaml.set("max.y", 255);
		yaml.set("max.z", 10);
		writeYaml(file, yaml);
	}

	private void writeYaml(File file, YamlConfiguration yaml) throws IOException {
		AtomicFileWriter.writeAtomically(file, yaml.saveToString().getBytes());
	}

	private interface DefaultWriter {
		void write(File file) throws IOException;
	}
}
