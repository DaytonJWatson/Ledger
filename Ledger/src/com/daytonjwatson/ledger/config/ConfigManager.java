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
	private YamlConfiguration priceBands;
	private YamlConfiguration overrides;
	private YamlConfiguration upgrades;
	private YamlConfiguration spawn;

	public ConfigManager(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public void loadAll() {
		AtomicFileWriter.ensureDirectory(plugin.getDataFolder());
		this.config = loadOrCreate(new File(plugin.getDataFolder(), "config.yml"), this::writeDefaultConfig);
		this.priceBands = loadOrCreate(new File(plugin.getDataFolder(), "price_bands.yml"), this::writeDefaultPriceBands);
		this.overrides = loadOrCreate(new File(plugin.getDataFolder(), "overrides.yml"), this::writeDefaultOverrides);
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

	public YamlConfiguration getPriceBands() {
		return priceBands;
	}

	public YamlConfiguration getOverrides() {
		return overrides;
	}

	public YamlConfiguration getUpgrades() {
		return upgrades;
	}

	public YamlConfiguration getSpawn() {
		return spawn;
	}

	public File getDataFolder() {
		return plugin.getDataFolder();
	}

	public void reloadPrices() {
		this.priceBands = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "price_bands.yml"));
		this.overrides = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "overrides.yml"));
		this.prices = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "prices.yml"));
	}

	public void reloadUpgrades() {
		this.upgrades = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "upgrades.yml"));
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
		yaml.set("market.depletionBaseline", 50000.0);
		yaml.set("market.scarcityRho", 0.25);
		yaml.set("market.windows.nightMultiplier", 1.25);
		yaml.set("market.windows.rainMultiplier", 1.20);
		yaml.set("market.windows.depthMultiplier", 1.30);
		yaml.set("market.windows.depthY", 32);
		yaml.set("tools.globalBase", 8000.0);
		yaml.set("tools.repairCap", 0.60);
		yaml.set("tools.repairCountFactor", 0.04);
		yaml.set("tools.repairBase", 0.10);
		yaml.set("mob.sigma", 1.0);
		yaml.set("mob.halfLifeHours", 48.0);
		yaml.set("mob.spawnerMultiplier", 0.20);
		yaml.set("mob.sameChunkMultiplier", 0.60);
		yaml.set("mob.sameChunkWindowSeconds", 45);
		yaml.set("farming.fatigue.perHarvest", 0.12);
		yaml.set("farming.fatigue.recoveryPerDay", 0.18);
		yaml.set("farming.fatigue.minMultiplier", 0.25);
		yaml.set("farming.fatigue.stackStep", 0.05);
		yaml.set("farming.fatigue.pruneDays", 14.0);
		writeYaml(file, yaml);
	}

	private void writeDefaultPrices(File file) throws IOException {
		YamlConfiguration bands = this.priceBands == null ? buildDefaultPriceBands() : this.priceBands;
		PriceBandTable bandTable = new PriceBandTable(bands);
		PriceGenerator generator = new PriceGenerator(new com.daytonjwatson.ledger.market.ItemTagService(), bandTable);
		YamlConfiguration yaml = generator.generate();
		ConfigurationSection mobsSection = yaml.createSection("mobPrices");
		createPrice(mobsSection, "entity:ZOMBIE", 12.0, 5000, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:SKELETON", 14.0, 5000, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:CREEPER", 20.0, 4000, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:SPIDER", 10.0, 5000, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:COW", 22.0, 4500, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:PIG", 18.0, 4500, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:SHEEP", 20.0, 4500, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:CHICKEN", 14.0, 5000, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:RABBIT", 16.0, 5000, 0.0, 0.0, false);
		writeYaml(file, yaml);
	}

	private void createPrice(ConfigurationSection section, String key, double base, double cap, double baseline, double rho, boolean infra) {
		ConfigurationSection entry = section.createSection(key);
		entry.set("base", base);
		entry.set("cap", cap);
		entry.set("minFactor", 0.20);
		entry.set("maxFactor", 2.50);
		entry.set("sigma", 1.0);
		entry.set("baseline", baseline);
		entry.set("rho", rho);
		entry.set("infra", infra);
	}

	private void writeDefaultPriceBands(File file) throws IOException {
		writeYaml(file, buildDefaultPriceBands());
	}

	private YamlConfiguration buildDefaultPriceBands() {
		YamlConfiguration yaml = new YamlConfiguration();
		ConfigurationSection bandsSection = yaml.createSection("bands");
		writeBand(bandsSection, "TRASH_COMMON", 0.05, 0.35, 200000, 0.9, 0.0);
		writeBand(bandsSection, "COMMON_BUILD", 0.25, 1.25, 120000, 0.9, 0.0);
		writeBand(bandsSection, "COMMON_NATURAL", 0.10, 0.80, 90000, 0.9, 0.0);
		writeBand(bandsSection, "ORE_COMMON", 1.5, 4.0, 30000, 1.0, 0.0);
		writeBand(bandsSection, "ORE_MID", 4.0, 18.0, 8000, 1.15, 0.0);
		writeBand(bandsSection, "ORE_RARE", 18.0, 65.0, 600, 1.35, 0.0);
		writeBand(bandsSection, "NETHER_RARE", 40.0, 200.0, 150, 1.45, 0.0);
		writeBand(bandsSection, "END_RARE", 20.0, 120.0, 250, 1.35, 0.0);
		writeBand(bandsSection, "MOB_COMMON", 0.25, 2.5, 20000, 1.1, 0.0);
		writeBand(bandsSection, "MOB_VALUABLE", 4.0, 60.0, 800, 1.3, 0.0);
		writeBand(bandsSection, "FARM_COMMON", 0.25, 2.0, 25000, 1.05, 0.0);
		writeBand(bandsSection, "FARM_VALUABLE", 2.0, 20.0, 5000, 1.2, 0.0);
		writeBand(bandsSection, "UTILITY_INFRA", 1.0, 12.0, 2000, 1.35, 0.0);
		writeBand(bandsSection, "REDSTONE_INFRA", 1.0, 25.0, 250, 1.55, 0.0);
		writeBand(bandsSection, "CONTAINER", 0.0, 0.2, 5000, 1.3, 0.0);
		writeBand(bandsSection, "ENCHANTED", 0.0, 0.0, 1, 1.0, 0.0);
		writeBand(bandsSection, "UNSELLABLE", 0.0, 0.0, 1, 1.0, 0.0);
		return yaml;
	}

	private void writeBand(ConfigurationSection section, String tag, double minPrice, double maxPrice, double cap, double sigma, double baseline) {
		ConfigurationSection entry = section.createSection(tag);
		entry.set("minPrice", minPrice);
		entry.set("maxPrice", maxPrice);
		entry.set("cap", cap);
		entry.set("sigma", sigma);
		entry.set("baseline", baseline);
	}

	private void writeDefaultOverrides(File file) throws IOException {
		YamlConfiguration yaml = new YamlConfiguration();
		ConfigurationSection overridesSection = yaml.createSection("overrides");
		writeOverride(overridesSection, "ENCHANTED_BOOK", 0.0, true);
		writeOverride(overridesSection, "WRITTEN_BOOK", 0.0, true);
		writeOverride(overridesSection, "FILLED_MAP", 0.0, true);
		writeOverride(overridesSection, "PLAYER_HEAD", 0.0, true);
		writeOverride(overridesSection, "POTION", 0.0, true);
		writeOverride(overridesSection, "SPLASH_POTION", 0.0, true);
		writeOverride(overridesSection, "LINGERING_POTION", 0.0, true);
		writeOverride(overridesSection, "TIPPED_ARROW", 0.0, true);
		writeOverride(overridesSection, "BUNDLE", 0.0, true);
		writeOverride(overridesSection, "SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "WHITE_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "ORANGE_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "MAGENTA_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "LIGHT_BLUE_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "YELLOW_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "LIME_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "PINK_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "GRAY_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "LIGHT_GRAY_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "CYAN_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "PURPLE_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "BLUE_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "BROWN_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "GREEN_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "RED_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "BLACK_SHULKER_BOX", 0.0, true);
		writeOverride(overridesSection, "BARRIER", 0.0, true);
		writeOverride(overridesSection, "COMMAND_BLOCK", 0.0, true);
		writeOverride(overridesSection, "CHAIN_COMMAND_BLOCK", 0.0, true);
		writeOverride(overridesSection, "REPEATING_COMMAND_BLOCK", 0.0, true);
		writeOverride(overridesSection, "STRUCTURE_BLOCK", 0.0, true);
		writeOverride(overridesSection, "STRUCTURE_VOID", 0.0, true);
		writeOverride(overridesSection, "JIGSAW", 0.0, true);
		writeOverride(overridesSection, "LIGHT", 0.0, true);
		writeOverride(overridesSection, "DEBUG_STICK", 0.0, true);
		writeOverride(overridesSection, "BAMBOO", 0.05, false);
		writeOverride(overridesSection, "KELP", 0.05, false);
		writeOverride(overridesSection, "SUGAR_CANE", 0.05, false);
		writeOverride(overridesSection, "CACTUS", 0.05, false);
		ConfigurationSection ironOverride = overridesSection.createSection("IRON_INGOT");
		ironOverride.set("base", 1.5);
		ironOverride.set("cap", 15000.0);
		ConfigurationSection debrisOverride = overridesSection.createSection("ANCIENT_DEBRIS");
		debrisOverride.set("base", 120.0);
		debrisOverride.set("cap", 80.0);
		ConfigurationSection scrapOverride = overridesSection.createSection("NETHERITE_SCRAP");
		scrapOverride.set("base", 80.0);
		scrapOverride.set("cap", 60.0);
		writeYaml(file, yaml);
	}

	private void writeOverride(ConfigurationSection section, String key, double base, boolean unsellable) {
		ConfigurationSection entry = section.createSection(key);
		entry.set("base", base);
		if (unsellable) {
			entry.set("unsellable", true);
		}
	}

	private void writeDefaultUpgrades(File file) throws IOException {
		YamlConfiguration yaml = new YamlConfiguration();
		ConfigurationSection upgradesSection = yaml.createSection("upgrades");
		createCurveUpgrade(upgradesSection, "barter", "Bartering", 20, 250, 1.55,
			"Improve sell prices with a bartering bonus.");
		createCurveUpgrade(upgradesSection, "insurance", "Insurance", 10, 1200, 1.60,
			"Reduce the percentage of money lost on death.");
		createCurveUpgrade(upgradesSection, "logistics", "Logistics", 5, 900, 1.55,
			"Earn bonuses for selling diverse batches of items.");

		createChoiceUpgrade(upgradesSection, "spec_miner_choice", "Specialization: Miner", 2000,
			"MINER", "Choose the Miner specialization.");
		createChoiceUpgrade(upgradesSection, "spec_farmer_choice", "Specialization: Farmer", 2000,
			"FARMER", "Choose the Farmer specialization.");
		createChoiceUpgrade(upgradesSection, "spec_hunter_choice", "Specialization: Hunter", 2000,
			"HUNTER", "Choose the Hunter specialization.");

		createSpecializationUpgrade(upgradesSection, "spec_miner", "Miner Mastery", 10, 500, 1.45, "MINER",
			"Boost ore sales when specialized as a Miner.");
		createSpecializationUpgrade(upgradesSection, "spec_farmer", "Farmer Mastery", 10, 500, 1.45, "FARMER",
			"Boost crop sales when specialized as a Farmer.");
		createSpecializationUpgrade(upgradesSection, "spec_hunter", "Hunter Mastery", 10, 500, 1.45, "HUNTER",
			"Boost mob drop sales when specialized as a Hunter.");

		createVendorUnlock(upgradesSection, "vendor_t2_iron", "Vendor Tier: Iron", 5000, 2,
			"Unlock iron tool purchases.");
		createVendorUnlock(upgradesSection, "vendor_t3_diamond", "Vendor Tier: Diamond", 15000, 3,
			"Unlock diamond tool purchases.", "vendor_t2_iron");
		createVendorUnlock(upgradesSection, "vendor_t4_netherite", "Vendor Tier: Netherite", 35000, 4,
			"Unlock netherite tool purchases.", "vendor_t3_diamond");

		createRefinementUnlock(upgradesSection, "refine_autosmelt_1", "Refinement: Autosmelt I", 4000, 1);
		createRefinementUnlock(upgradesSection, "refine_autosmelt_2", "Refinement: Autosmelt II", 6500, 2, "refine_autosmelt_1");
		createRefinementUnlock(upgradesSection, "refine_autosmelt_3", "Refinement: Autosmelt III", 9500, 3, "refine_autosmelt_2");
		createRefinementUnlock(upgradesSection, "refine_autosmelt_4", "Refinement: Autosmelt IV", 13000, 4, "refine_autosmelt_3");
		createRefinementUnlock(upgradesSection, "refine_autosmelt_5", "Refinement: Autosmelt V", 17500, 5, "refine_autosmelt_4");
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
		yaml.set("animalPen.min.x", -4);
		yaml.set("animalPen.min.y", 0);
		yaml.set("animalPen.min.z", -4);
		yaml.set("animalPen.max.x", 4);
		yaml.set("animalPen.max.y", 255);
		yaml.set("animalPen.max.z", 4);
		yaml.set("hubBlocks", java.util.List.of("EMERALD_BLOCK"));
		writeYaml(file, yaml);
	}

	private void writeYaml(File file, YamlConfiguration yaml) throws IOException {
		AtomicFileWriter.writeAtomically(file, yaml.saveToString().getBytes());
	}

	private void createCurveUpgrade(ConfigurationSection root, String id, String name, int maxLevel, double c0, double g, String description) {
		ConfigurationSection section = root.createSection(id);
		section.set("name", name);
		section.set("type", "LEVEL");
		section.set("maxLevel", maxLevel);
		ConfigurationSection cost = section.createSection("cost");
		cost.set("c0", c0);
		cost.set("g", g);
		section.set("description", description);
	}

	private void createChoiceUpgrade(ConfigurationSection root, String id, String name, long cost, String specializationChoice, String description) {
		ConfigurationSection section = root.createSection(id);
		section.set("name", name);
		section.set("type", "CHOICE");
		section.set("cost", cost);
		section.set("specializationChoice", specializationChoice);
		section.set("description", description);
	}

	private void createSpecializationUpgrade(ConfigurationSection root, String id, String name, int maxLevel, double c0, double g,
											 String specializationRequirement, String description) {
		ConfigurationSection section = root.createSection(id);
		section.set("name", name);
		section.set("type", "LEVEL");
		section.set("maxLevel", maxLevel);
		ConfigurationSection cost = section.createSection("cost");
		cost.set("c0", c0);
		cost.set("g", g);
		section.set("specializationRequirement", specializationRequirement);
		section.set("description", description);
	}

	private void createVendorUnlock(ConfigurationSection root, String id, String name, long cost, int tier, String description, String... requires) {
		ConfigurationSection section = root.createSection(id);
		section.set("name", name);
		section.set("type", "LEVEL");
		section.set("maxLevel", 1);
		ConfigurationSection costSection = section.createSection("cost");
		costSection.set("c0", cost);
		costSection.set("g", 1.0);
		section.set("unlocksVendorTier", tier);
		section.set("description", description);
		if (requires.length > 0) {
			section.set("requires", requires);
		}
	}

	private void createRefinementUnlock(ConfigurationSection root, String id, String name, long cost, int level, String... requires) {
		ConfigurationSection section = root.createSection(id);
		section.set("name", name);
		section.set("type", "LEVEL");
		section.set("maxLevel", 1);
		ConfigurationSection costSection = section.createSection("cost");
		costSection.set("c0", cost);
		costSection.set("g", 1.0);
		section.set("refinementLevel", level);
		section.set("description", "Unlock autosmelt level " + level + ".");
		if (requires.length > 0) {
			section.set("requires", requires);
		}
	}

	private interface DefaultWriter {
		void write(File file) throws IOException;
	}
}
