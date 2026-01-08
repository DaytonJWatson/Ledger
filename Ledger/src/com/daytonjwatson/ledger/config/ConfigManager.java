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
		writeYaml(file, yaml);
	}

	private void writeDefaultPrices(File file) throws IOException {
		YamlConfiguration yaml = new YamlConfiguration();
		ConfigurationSection pricesSection = yaml.createSection("prices");
		createPrice(pricesSection, "STONE", 1.0, 10000, 50000.0, 0.25, false);
		createPrice(pricesSection, "COBBLESTONE", 0.7, 10000, 50000.0, 0.25, false);
		createPrice(pricesSection, "IRON_INGOT", 25.0, 5000, 15000.0, 0.35, false);
		createPrice(pricesSection, "GOLD_INGOT", 40.0, 4000, 12000.0, 0.35, false);
		createPrice(pricesSection, "DIAMOND", 250.0, 1000, 5000.0, 0.40, false);
		createPrice(pricesSection, "NETHERITE_INGOT", 1200.0, 500, 2000.0, 0.45, false);
		createPrice(pricesSection, "WHEAT", 4.0, 8000, 40000.0, 0.20, false);
		createPrice(pricesSection, "CARROT", 3.5, 8000, 35000.0, 0.20, false);
		createPrice(pricesSection, "POTATO", 3.5, 8000, 35000.0, 0.20, false);
		createPrice(pricesSection, "BEEF", 8.0, 6000, 30000.0, 0.20, false);
		createPrice(pricesSection, "PORKCHOP", 8.0, 6000, 30000.0, 0.20, false);
		createPrice(pricesSection, "CHICKEN", 6.0, 6000, 30000.0, 0.20, false);
		createPrice(pricesSection, "CHEST", 6.0, 12000, 50000.0, 0.20, true);
		createPrice(pricesSection, "LADDER", 1.2, 15000, 50000.0, 0.20, true);
		createPrice(pricesSection, "RAIL", 2.5, 12000, 40000.0, 0.20, true);
		createPrice(pricesSection, "POWERED_RAIL", 8.0, 8000, 30000.0, 0.20, true);
		createPrice(pricesSection, "TORCH", 0.8, 20000, 60000.0, 0.20, true);
		createPrice(pricesSection, "OAK_PLANKS", 0.5, 20000, 60000.0, 0.15, true);
		createPrice(pricesSection, "STONE_BRICKS", 1.5, 15000, 50000.0, 0.20, true);
		createPrice(pricesSection, "GLASS", 1.4, 15000, 40000.0, 0.20, true);
		createPrice(pricesSection, "FURNACE", 3.0, 12000, 50000.0, 0.20, true);
		createPrice(pricesSection, "HOPPER", 18.0, 5000, 15000.0, 0.25, true);
		createPrice(pricesSection, "BARREL", 5.0, 12000, 40000.0, 0.20, true);
		createPrice(pricesSection, "CARTOGRAPHY_TABLE", 8.0, 8000, 30000.0, 0.20, true);
		createPrice(pricesSection, "SMOKER", 6.0, 10000, 30000.0, 0.20, true);
		createPrice(pricesSection, "BLAST_FURNACE", 12.0, 7000, 20000.0, 0.20, true);
		createPrice(pricesSection, "LECTERN", 7.0, 10000, 35000.0, 0.20, true);
		createPrice(pricesSection, "COMPOSTER", 2.0, 15000, 50000.0, 0.20, true);
		createPrice(pricesSection, "OAK_TRAPDOOR", 1.1, 20000, 60000.0, 0.15, true);
		createPrice(pricesSection, "OAK_FENCE", 0.9, 20000, 60000.0, 0.15, true);
		createPrice(pricesSection, "OAK_DOOR", 1.2, 20000, 60000.0, 0.15, true);
		createPrice(pricesSection, "STONE_BUTTON", 0.3, 25000, 80000.0, 0.10, true);
		createPrice(pricesSection, "LEVER", 0.5, 25000, 80000.0, 0.10, true);
		createPrice(pricesSection, "REDSTONE_TORCH", 1.2, 15000, 50000.0, 0.20, true);
		createPrice(pricesSection, "OAK_SIGN", 0.8, 20000, 60000.0, 0.15, true);
		createPrice(pricesSection, "OAK_STAIRS", 0.7, 20000, 60000.0, 0.15, true);
		createPrice(pricesSection, "STONE_STAIRS", 0.9, 20000, 60000.0, 0.15, true);
		createPrice(pricesSection, "STONE_SLAB", 0.4, 25000, 70000.0, 0.10, true);
		createPrice(pricesSection, "OAK_SLAB", 0.3, 25000, 70000.0, 0.10, true);
		createPrice(pricesSection, "STICK", 0.2, 30000, 90000.0, 0.10, true);
		createPrice(pricesSection, "BOOKSHELF", 12.0, 6000, 20000.0, 0.25, true);
		createPrice(pricesSection, "ITEM_FRAME", 4.0, 12000, 40000.0, 0.20, true);
		createPrice(pricesSection, "PAINTING", 3.0, 12000, 40000.0, 0.20, true);
		createPrice(pricesSection, "BOOK", 4.0, 15000, 40000.0, 0.20, true);
		createPrice(pricesSection, "MINECART", 12.0, 7000, 20000.0, 0.20, true);
		createPrice(pricesSection, "CHEST_MINECART", 14.0, 6000, 20000.0, 0.20, true);
		createPrice(pricesSection, "OAK_PRESSURE_PLATE", 0.6, 22000, 70000.0, 0.10, true);
		createPrice(pricesSection, "STONE_PRESSURE_PLATE", 0.6, 22000, 70000.0, 0.10, true);
		createPrice(pricesSection, "CAMPFIRE", 4.0, 10000, 30000.0, 0.20, true);
		createPrice(pricesSection, "LANTERN", 6.0, 8000, 25000.0, 0.20, true);
		createPrice(pricesSection, "CHAIN", 2.0, 15000, 40000.0, 0.20, true);
		createPrice(pricesSection, "ANVIL", 30.0, 3000, 10000.0, 0.30, true);
		createPrice(pricesSection, "GRINDSTONE", 4.0, 12000, 30000.0, 0.20, true);
		createPrice(pricesSection, "SMITHING_TABLE", 5.0, 12000, 30000.0, 0.20, true);
		createPrice(pricesSection, "CRAFTING_TABLE", 1.0, 20000, 60000.0, 0.10, true);
		createPrice(pricesSection, "REDSTONE", 2.5, 15000, 40000.0, 0.20, false);
		createPrice(pricesSection, "COAL", 2.0, 18000, 60000.0, 0.20, false);
		createPrice(pricesSection, "COPPER_INGOT", 8.0, 9000, 20000.0, 0.30, false);
		createPrice(pricesSection, "EMERALD", 200.0, 1500, 4000.0, 0.40, false);
		createPrice(pricesSection, "LAPIS_LAZULI", 6.0, 10000, 25000.0, 0.25, false);
		createPrice(pricesSection, "QUARTZ", 5.0, 12000, 30000.0, 0.20, false);
		createPrice(pricesSection, "AMETHYST_SHARD", 15.0, 6000, 10000.0, 0.30, false);
		createPrice(pricesSection, "STRING", 2.0, 15000, 40000.0, 0.20, false);
		createPrice(pricesSection, "BONE", 2.0, 15000, 40000.0, 0.20, false);
		createPrice(pricesSection, "GUNPOWDER", 4.0, 12000, 30000.0, 0.20, false);
		createPrice(pricesSection, "ROTTEN_FLESH", 1.0, 20000, 50000.0, 0.15, false);
		createPrice(pricesSection, "SPIDER_EYE", 3.0, 15000, 40000.0, 0.20, false);
		createPrice(pricesSection, "ENDER_PEARL", 15.0, 8000, 15000.0, 0.30, false);
		createPrice(pricesSection, "LEATHER", 6.0, 12000, 30000.0, 0.20, false);
		createPrice(pricesSection, "RABBIT_HIDE", 4.0, 12000, 30000.0, 0.20, false);
		createPrice(pricesSection, "FEATHER", 3.0, 15000, 40000.0, 0.20, false);
		createPrice(pricesSection, "SUGAR_CANE", 2.0, 20000, 60000.0, 0.15, false);
		createPrice(pricesSection, "MELON_SLICE", 1.2, 20000, 60000.0, 0.15, false);
		createPrice(pricesSection, "PUMPKIN", 3.0, 15000, 40000.0, 0.20, false);
		createPrice(pricesSection, "BEETROOT", 3.0, 18000, 50000.0, 0.15, false);
		createPrice(pricesSection, "BEETROOT_SEEDS", 1.0, 22000, 70000.0, 0.10, false);
		createPrice(pricesSection, "COCOA_BEANS", 2.5, 18000, 50000.0, 0.15, false);
		createPrice(pricesSection, "NETHER_WART", 4.0, 15000, 40000.0, 0.20, false);
		createPrice(pricesSection, "SWEET_BERRIES", 2.0, 20000, 60000.0, 0.15, false);
		createPrice(pricesSection, "BAMBOO", 0.8, 25000, 80000.0, 0.10, false);
		createPrice(pricesSection, "COD", 5.0, 12000, 30000.0, 0.20, false);
		createPrice(pricesSection, "SALMON", 5.0, 12000, 30000.0, 0.20, false);
		createPrice(pricesSection, "PUFFERFISH", 6.0, 10000, 25000.0, 0.20, false);
		createPrice(pricesSection, "TROPICAL_FISH", 6.0, 10000, 25000.0, 0.20, false);
		createPrice(pricesSection, "TAG:ORE", 0.0, 1.0, 50000.0, 0.30, false);
		createPrice(pricesSection, "TAG:CROP", 0.0, 1.0, 60000.0, 0.20, false);
		createPrice(pricesSection, "TAG:MOB", 0.0, 1.0, 50000.0, 0.20, false);
		createPrice(pricesSection, "TAG:INFRA", 0.0, 1.0, 70000.0, 0.15, false);
		ConfigurationSection mobsSection = yaml.createSection("mobPrices");
		createPrice(mobsSection, "entity:ZOMBIE", 12.0, 5000, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:SKELETON", 14.0, 5000, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:CREEPER", 20.0, 4000, 0.0, 0.0, false);
		createPrice(mobsSection, "entity:SPIDER", 10.0, 5000, 0.0, 0.0, false);
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

	private void writeDefaultUpgrades(File file) throws IOException {
		YamlConfiguration yaml = new YamlConfiguration();
		ConfigurationSection upgradesSection = yaml.createSection("upgrades");
		createCurveUpgrade(upgradesSection, "barter", "Bartering", 20, 250, 1.55,
			"Improve sell prices with a bartering bonus.");
		createCurveUpgrade(upgradesSection, "insurance", "Insurance", 10, 1200, 1.60,
			"Reduce the percentage of money lost on death.");
		createCurveUpgrade(upgradesSection, "logistics", "Logistics", 5, 900, 1.55,
			"Earn bonuses for selling diverse batches of items.");

		createChoiceUpgrade(upgradesSection, "spec_choice_miner", "Specialization: Miner", 2000,
			"MINER", "Choose the Miner specialization.");
		createChoiceUpgrade(upgradesSection, "spec_choice_farmer", "Specialization: Farmer", 2000,
			"FARMER", "Choose the Farmer specialization.");
		createChoiceUpgrade(upgradesSection, "spec_choice_hunter", "Specialization: Hunter", 2000,
			"HUNTER", "Choose the Hunter specialization.");

		createSpecializationUpgrade(upgradesSection, "spec_miner", "Miner Mastery", 10, 500, 1.45, "MINER",
			"Boost ore sales when specialized as a Miner.");
		createSpecializationUpgrade(upgradesSection, "spec_farmer", "Farmer Mastery", 10, 500, 1.45, "FARMER",
			"Boost crop sales when specialized as a Farmer.");
		createSpecializationUpgrade(upgradesSection, "spec_hunter", "Hunter Mastery", 10, 500, 1.45, "HUNTER",
			"Boost mob drop sales when specialized as a Hunter.");

		createVendorUnlock(upgradesSection, "vendor_iron", "Vendor Tier: Iron", 5000, 1,
			"Unlock iron tool purchases.");
		createVendorUnlock(upgradesSection, "vendor_diamond", "Vendor Tier: Diamond", 15000, 2,
			"Unlock diamond tool purchases.", "vendor_iron");
		createVendorUnlock(upgradesSection, "vendor_netherite", "Vendor Tier: Netherite", 35000, 3,
			"Unlock netherite tool purchases.", "vendor_diamond");

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
		section.set("type", "UNLOCK");
		section.set("cost", cost);
		section.set("unlocksVendorTier", tier);
		section.set("description", description);
		if (requires.length > 0) {
			section.set("requires", requires);
		}
	}

	private void createRefinementUnlock(ConfigurationSection root, String id, String name, long cost, int level, String... requires) {
		ConfigurationSection section = root.createSection(id);
		section.set("name", name);
		section.set("type", "UNLOCK");
		section.set("cost", cost);
		section.set("refinementLevel", level);
		section.set("description", "Unlock autosmelt level " + level + " (not implemented yet).");
		if (requires.length > 0) {
			section.set("requires", requires);
		}
	}

	private interface DefaultWriter {
		void write(File file) throws IOException;
	}
}
