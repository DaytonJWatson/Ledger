package com.daytonjwatson.ledger.market;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.config.PriceTable;
import com.daytonjwatson.ledger.upgrades.UpgradeService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MarketService {
	private final ConfigManager configManager;
	private final MarketState marketState;
	private final PriceTable priceTable;
	private final UpgradeService upgradeService;
	private final Map<String, Double> priceCache = new HashMap<>();
	private long marketVersion = 0;
	private long priceCacheVersion = -1;

	public MarketService(ConfigManager configManager, MarketState marketState, UpgradeService upgradeService) {
		this.configManager = configManager;
		this.marketState = marketState;
		this.upgradeService = upgradeService;
		this.priceTable = new PriceTable(configManager.getPrices());
	}

	public double getSellPrice(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return 0.0;
		}
		return getSellPrice(item.getType().name());
	}

	public double getSellPrice(String key) {
		if (key == null) {
			return 0.0;
		}
		String normalized = key.toUpperCase();
		PriceTable.PriceEntry entry = priceTable.getEntry(normalized);
		if (entry == null || entry.getBase() <= 0.0) {
			return 0.0;
		}
		if (priceCacheVersion != marketVersion) {
			priceCache.clear();
			priceCacheVersion = marketVersion;
		}
		return priceCache.computeIfAbsent(normalized, ignored -> computePrice(entry, normalized));
	}

	public double sell(ItemStack item, int quantity) {
		if (item == null || item.getType() == Material.AIR || quantity <= 0) {
			return 0.0;
		}
		double price = getSellPrice(item.getType().name());
		if (price <= 0.0) {
			return 0.0;
		}
		applySale(item.getType().name(), quantity);
		return price * quantity;
	}

	public double getSellPrice(Player player, ItemStack item) {
		return getSellPrice(player, item, 1);
	}

	public double getSellPrice(Player player, ItemStack item, int distinctTypes) {
		if (player == null) {
			return getSellPrice(item);
		}
		double base = getSellPrice(item);
		if (base <= 0.0) {
			return 0.0;
		}
		UUID uuid = player.getUniqueId();
		int barterLevel = upgradeService.getLevel(uuid, "barter");
		int logisticsLevel = upgradeService.getLevel(uuid, "logistics");
		String specialization = upgradeService.getSpecializationChoice(uuid);
		int specializationLevel = getSpecializationLevel(uuid, specialization);
		double price = base;
		price *= upgradeService.getBarterMultiplier(barterLevel);
		price *= upgradeService.getLogisticsMultiplier(logisticsLevel, distinctTypes);
		price *= upgradeService.getSpecializationMultiplier(specializationLevel, matchesSpecialization(specialization, item));
		return clamp(price, base * 0.2, base * 5.0);
	}

	public double sell(Player player, ItemStack item, int quantity) {
		return sell(player, item, quantity, 1);
	}

	public double sell(Player player, ItemStack item, int quantity, int distinctTypes) {
		if (item == null || item.getType() == Material.AIR || quantity <= 0) {
			return 0.0;
		}
		double price = getSellPrice(player, item, distinctTypes);
		if (price <= 0.0) {
			return 0.0;
		}
		applySale(item.getType().name(), quantity);
		return price * quantity;
	}

	public void applySale(String key, int quantity) {
		MarketState.ItemState state = marketState.getOrCreateItem(key);
		decay(state);
		state.setSoldAccumulator(state.getSoldAccumulator() + quantity);
		state.setLastUpdate(System.currentTimeMillis());
		marketVersion++;
	}

	private double computePrice(PriceTable.PriceEntry entry, String key) {
		MarketState.ItemState state = marketState.getOrCreateItem(key);
		decay(state);
		double supplyFactor = 1.0 / Math.pow(1.0 + (state.getSoldAccumulator() / entry.getCap()), entry.getSigma());
		double minFactor = entry.getMinFactor();
		double maxFactor = entry.getMaxFactor();
		double raw = entry.getBase() * supplyFactor;
		double clamped = clamp(raw, entry.getBase() * minFactor, entry.getBase() * maxFactor);
		return Math.max(0.0, clamped);
	}

	private void decay(MarketState.ItemState state) {
		long now = System.currentTimeMillis();
		double hours = Math.min(168.0, Math.max(0.0, (now - state.getLastUpdate()) / 3600000.0));
		if (hours <= 0.0) {
			return;
		}
		double halfLife = configManager.getConfig().getDouble("market.halfLifeHours", 72.0);
		double lambda = Math.log(2) / halfLife;
		state.setSoldAccumulator(state.getSoldAccumulator() * Math.exp(-lambda * hours));
		state.setLastUpdate(now);
	}

	private double clamp(double value, double min, double max) {
		return Math.min(max, Math.max(min, value));
	}

	private int getSpecializationLevel(UUID uuid, String specialization) {
		if (specialization == null) {
			return 0;
		}
		return switch (specialization.toUpperCase(Locale.ROOT)) {
			case "MINER" -> upgradeService.getLevel(uuid, "spec_miner");
			case "FARMER" -> upgradeService.getLevel(uuid, "spec_farmer");
			case "HUNTER" -> upgradeService.getLevel(uuid, "spec_hunter");
			default -> 0;
		};
	}

	private boolean matchesSpecialization(String specialization, ItemStack item) {
		if (specialization == null || item == null) {
			return false;
		}
		ItemCategory category = getCategory(item.getType());
		return switch (specialization.toUpperCase(Locale.ROOT)) {
			case "MINER" -> category == ItemCategory.ORE;
			case "FARMER" -> category == ItemCategory.CROP;
			case "HUNTER" -> category == ItemCategory.MOB;
			default -> false;
		};
	}

	private ItemCategory getCategory(Material material) {
		if (material == null) {
			return ItemCategory.INFRA;
		}
		String name = material.name();
		if (name.contains("ORE") || name.contains("INGOT") || name.contains("RAW_") || name.contains("NUGGET")
			|| name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("AMETHYST")) {
			return ItemCategory.ORE;
		}
		if (CROP_ITEMS.contains(material)) {
			return ItemCategory.CROP;
		}
		if (MOB_ITEMS.contains(material)) {
			return ItemCategory.MOB;
		}
		return ItemCategory.INFRA;
	}

	private enum ItemCategory {
		ORE,
		CROP,
		MOB,
		INFRA
	}

	private static final Set<Material> CROP_ITEMS = EnumSet.of(
		Material.WHEAT,
		Material.CARROT,
		Material.POTATO,
		Material.BEETROOT,
		Material.BEETROOT_SEEDS,
		Material.MELON_SLICE,
		Material.PUMPKIN,
		Material.SUGAR_CANE,
		Material.BAMBOO,
		Material.COCOA_BEANS,
		Material.NETHER_WART,
		Material.SWEET_BERRIES
	);

	private static final Set<Material> MOB_ITEMS = EnumSet.of(
		Material.BEEF,
		Material.PORKCHOP,
		Material.CHICKEN,
		Material.MUTTON,
		Material.RABBIT,
		Material.COD,
		Material.SALMON,
		Material.TROPICAL_FISH,
		Material.PUFFERFISH,
		Material.ROTTEN_FLESH,
		Material.BONE,
		Material.STRING,
		Material.SPIDER_EYE,
		Material.GUNPOWDER,
		Material.ENDER_PEARL
	);
}
