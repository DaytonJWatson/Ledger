package com.daytonjwatson.ledger.market;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.config.PriceTable;
import com.daytonjwatson.ledger.farming.SoilFatigueService;
import com.daytonjwatson.ledger.tools.SilkTouchMarkService;
import com.daytonjwatson.ledger.upgrades.UpgradeService;
import com.daytonjwatson.ledger.util.ItemKeyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MarketService {
	private final ConfigManager configManager;
	private final MarketState marketState;
	private PriceTable priceTable;
	private final UpgradeService upgradeService;
	private final SilkTouchMarkService silkTouchMarkService;
	private final ScarcityWindowService scarcityWindowService;
	private final SoilFatigueService soilFatigueService;
	private final Map<String, Double> priceCache = new HashMap<>();
	private final Set<String> missingKeys = new HashSet<>();
	private long marketVersion = 0;
	private long priceCacheVersion = -1;

	public MarketService(ConfigManager configManager, MarketState marketState, UpgradeService upgradeService,
						 SilkTouchMarkService silkTouchMarkService, ScarcityWindowService scarcityWindowService,
						 SoilFatigueService soilFatigueService) {
		this.configManager = configManager;
		this.marketState = marketState;
		this.upgradeService = upgradeService;
		this.silkTouchMarkService = silkTouchMarkService;
		this.scarcityWindowService = scarcityWindowService;
		this.soilFatigueService = soilFatigueService;
		this.priceTable = new PriceTable(configManager.getPrices(), configManager.getOverrides());
	}

	public double getSellPrice(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return 0.0;
		}
		double base = getSellPrice(ItemKeyUtil.toKey(item.getType()));
		if (base <= 0.0) {
			return 0.0;
		}
		double price = base * silkTouchMarkService.getSellMultiplier(item);
		return applyFatigueMultiplier(item, price);
	}

	public double getSellPrice(String key) {
		String normalized = ItemKeyUtil.normalizeKey(key);
		if (normalized == null) {
			return 0.0;
		}
		PriceTable.PriceEntry entry = priceTable.getEntry(normalized);
		if (entry == null) {
			logMissingPrice(normalized);
			return 0.0;
		}
		if (entry.isUnsellable() || entry.getBase() <= 0.0) {
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
		double price = getSellPrice(ItemKeyUtil.toKey(item.getType()));
		if (price <= 0.0) {
			return 0.0;
		}
		applySale(ItemKeyUtil.toKey(item.getType()), quantity);
		return price * quantity;
	}

	public double getSellPrice(Player player, ItemStack item) {
		return getSellPrice(player, item, 1, false);
	}

	public double getSellPriceForInventory(Player player, ItemStack item, int distinctTypes) {
		return getSellPrice(player, item, distinctTypes, true);
	}

	public double getSellPrice(Player player, ItemStack item, int distinctTypes) {
		return getSellPrice(player, item, distinctTypes, false);
	}

	private double getSellPrice(Player player, ItemStack item, int distinctTypes, boolean applyLogistics) {
		if (player == null) {
			return getSellPrice(item);
		}
		double base = getRefinedBasePrice(player.getUniqueId(), item);
		if (base <= 0.0) {
			return 0.0;
		}
		UUID uuid = player.getUniqueId();
		int barterLevel = upgradeService.getLevel(uuid, "barter");
		String specialization = upgradeService.getSpecializationChoice(uuid);
		int specializationLevel = getSpecializationLevel(uuid, specialization);
		double windowMultiplier = scarcityWindowService.getWindowMultiplier(player, ItemKeyUtil.toKey(item.getType()), ScarcityWindowService.WindowContext.MARKET);
		double price = base * windowMultiplier;
		price *= upgradeService.getBarterMultiplier(barterLevel);
		price *= getSpecializationMultiplier(specializationLevel, specialization, item);
		if (applyLogistics) {
			int logisticsLevel = upgradeService.getLevel(uuid, "logistics");
			price *= upgradeService.getLogisticsMultiplier(logisticsLevel, distinctTypes);
		}
		double clamped = clamp(price, base * windowMultiplier * 0.2, base * windowMultiplier * 5.0);
		return applyFatigueMultiplier(item, clamped);
	}

	public double sell(Player player, ItemStack item, int quantity) {
		return sell(player, item, quantity, 1);
	}

	public double sell(Player player, ItemStack item, int quantity, int distinctTypes) {
		if (item == null || item.getType() == Material.AIR || quantity <= 0) {
			return 0.0;
		}
		double price = getSellPriceForInventory(player, item, distinctTypes);
		if (price <= 0.0) {
			return 0.0;
		}
		applySale(ItemKeyUtil.toKey(item.getType()), quantity);
		return price * quantity;
	}

	public long getProjectedSellValueAfterMarketChange(List<ItemStack> items, Map<Material, Integer> quantities) {
		if (items == null || items.isEmpty() || quantities == null || quantities.isEmpty()) {
			return 0L;
		}
		MarketSnapshot snapshot = snapshotMarketState(quantities.keySet());
		try {
			long total = 0L;
			for (ItemStack item : items) {
				if (item == null || item.getType() == Material.AIR) {
					continue;
				}
				double price = getSellPrice(item);
				if (price <= 0.0) {
					continue;
				}
				total += Math.round(price * item.getAmount());
				applySale(ItemKeyUtil.toKey(item.getType()), item.getAmount());
			}
			return total;
		} finally {
			restoreMarketState(snapshot);
		}
	}

	public void applySale(String key, int quantity) {
		String normalized = ItemKeyUtil.normalizeKey(key);
		if (normalized == null) {
			return;
		}
		MarketState.ItemState state = marketState.getOrCreateItem(normalized);
		decay(state);
		state.setSoldAccumulator(state.getSoldAccumulator() + quantity);
		state.setLastUpdate(System.currentTimeMillis());
		marketVersion++;
	}

	private MarketSnapshot snapshotMarketState(Set<Material> materials) {
		Map<String, ItemStateSnapshot> itemSnapshots = new HashMap<>();
		for (Material material : materials) {
			if (material == null) {
				continue;
			}
			String key = ItemKeyUtil.normalizeKey(ItemKeyUtil.toKey(material));
			if (key == null) {
				continue;
			}
			MarketState.ItemState state = marketState.getOrCreateItem(key);
			itemSnapshots.put(key, new ItemStateSnapshot(state.getSoldAccumulator(), state.getLastUpdate(), state.getMinedTotal()));
		}
		return new MarketSnapshot(itemSnapshots, marketVersion, priceCacheVersion, new HashMap<>(priceCache));
	}

	private void restoreMarketState(MarketSnapshot snapshot) {
		if (snapshot == null) {
			return;
		}
		for (Map.Entry<String, ItemStateSnapshot> entry : snapshot.itemSnapshots().entrySet()) {
			MarketState.ItemState state = marketState.getOrCreateItem(entry.getKey());
			ItemStateSnapshot itemSnapshot = entry.getValue();
			state.setSoldAccumulator(itemSnapshot.soldAccumulator());
			state.setLastUpdate(itemSnapshot.lastUpdate());
			state.setMinedTotal(itemSnapshot.minedTotal());
		}
		marketVersion = snapshot.marketVersion();
		priceCacheVersion = snapshot.priceCacheVersion();
		priceCache.clear();
		priceCache.putAll(snapshot.priceCache());
	}

	private record MarketSnapshot(Map<String, ItemStateSnapshot> itemSnapshots, long marketVersion,
								  long priceCacheVersion, Map<String, Double> priceCache) {
	}

	private record ItemStateSnapshot(double soldAccumulator, long lastUpdate, double minedTotal) {
	}

	public void recordMining(String key, double quantity) {
		String normalized = ItemKeyUtil.normalizeKey(key);
		if (normalized == null || quantity <= 0.0) {
			return;
		}
		MarketState.ItemState state = marketState.getOrCreateItem(normalized);
		state.setMinedTotal(state.getMinedTotal() + quantity);
		marketVersion++;
	}

	private double computePrice(PriceTable.PriceEntry entry, String key) {
		MarketState.ItemState state = marketState.getOrCreateItem(key);
		decay(state);
		double supplyFactor = 1.0 / Math.pow(1.0 + (state.getSoldAccumulator() / entry.getCap()), entry.getSigma());
		double scarcityFactor = getScarcityFactor(entry, key, state);
		double minFactor = entry.getMinFactor();
		double maxFactor = entry.getMaxFactor();
		double raw = entry.getBase() * supplyFactor * scarcityFactor;
		double clamped = clamp(raw, entry.getBase() * minFactor, entry.getBase() * maxFactor * scarcityFactor);
		double adjusted = applyAntiArbitrage(entry, key, clamped);
		return Math.max(0.0, adjusted);
	}

	private double applyAntiArbitrage(PriceTable.PriceEntry entry, String key, double price) {
		String tag = entry.getTag() == null ? "" : entry.getTag().toUpperCase(Locale.ROOT);
		if (!tag.equals(PriceBandTag.UTILITY_INFRA.name()) && !tag.equals(PriceBandTag.REDSTONE_INFRA.name())) {
			return price;
		}
		Material material = Material.matchMaterial(key);
		if (material == null) {
			return price;
		}
		double materialValue = computeMaterialValue(material, key);
		if (materialValue <= 0.0) {
			return price;
		}
		return Math.min(price, materialValue * 0.85);
	}

	private double computeMaterialValue(Material material, String key) {
		for (Recipe recipe : Bukkit.getRecipesFor(new ItemStack(material))) {
			if (recipe instanceof ShapedRecipe shaped) {
				double value = computeShapedValue(shaped, key);
				if (value > 0.0) {
					return value;
				}
			}
			if (recipe instanceof ShapelessRecipe shapeless) {
				double value = computeShapelessValue(shapeless, key);
				if (value > 0.0) {
					return value;
				}
			}
		}
		return 0.0;
	}

	private double computeShapedValue(ShapedRecipe shaped, String key) {
		Map<Character, Integer> counts = new HashMap<>();
		for (String row : shaped.getShape()) {
			for (char symbol : row.toCharArray()) {
				if (symbol == ' ') {
					continue;
				}
				counts.put(symbol, counts.getOrDefault(symbol, 0) + 1);
			}
		}
		double total = 0.0;
		for (Map.Entry<Character, ItemStack> entry : shaped.getIngredientMap().entrySet()) {
			ItemStack ingredient = entry.getValue();
			if (ingredient == null || ingredient.getType() == Material.AIR) {
				continue;
			}
			String ingredientKey = ItemKeyUtil.toKey(ingredient.getType());
			if (ingredientKey.equalsIgnoreCase(key)) {
				return 0.0;
			}
			int count = counts.getOrDefault(entry.getKey(), 0);
			double ingredientPrice = getSellPrice(ingredientKey);
			if (ingredientPrice <= 0.0 || count <= 0) {
				return 0.0;
			}
			total += ingredientPrice * ingredient.getAmount() * count;
		}
		return total;
	}

	private double computeShapelessValue(ShapelessRecipe shapeless, String key) {
		double total = 0.0;
		for (ItemStack ingredient : shapeless.getIngredientList()) {
			if (ingredient == null || ingredient.getType() == Material.AIR) {
				continue;
			}
			String ingredientKey = ItemKeyUtil.toKey(ingredient.getType());
			if (ingredientKey.equalsIgnoreCase(key)) {
				return 0.0;
			}
			double ingredientPrice = getSellPrice(ingredientKey);
			if (ingredientPrice <= 0.0) {
				return 0.0;
			}
			total += ingredientPrice * ingredient.getAmount();
		}
		return total;
	}

	private double getScarcityFactor(PriceTable.PriceEntry entry, String key, MarketState.ItemState state) {
		double baseline = entry.getBaseline();
		double rho = entry.getRho();
		if (baseline <= 0.0) {
			baseline = configManager.getConfig().getDouble("market.depletionBaseline", 50000.0);
		}
		if (rho <= 0.0) {
			rho = configManager.getConfig().getDouble("market.scarcityRho", 0.25);
		}
		double depletionRatio = clamp(state.getMinedTotal() / baseline, 0.0, 1.0);
		return 1.0 + rho * depletionRatio;
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

	private double getSpecializationMultiplier(int specializationLevel, String specialization, ItemStack item) {
		if (specializationLevel <= 0 || specialization == null || item == null) {
			return 1.0;
		}
		SpecializationDomain itemDomain = getDomain(item.getType());
		if (itemDomain == SpecializationDomain.NONE) {
			return 1.0;
		}
		SpecializationDomain playerDomain = SpecializationDomain.fromChoice(specialization);
		if (playerDomain == SpecializationDomain.NONE) {
			return 1.0;
		}
		if (playerDomain == itemDomain) {
			return 1.0 + (0.03 * specializationLevel);
		}
		return 1.0 - (0.015 * specializationLevel);
	}

	private double getRefinedBasePrice(UUID uuid, ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return 0.0;
		}
		double base = getSellPrice(ItemKeyUtil.toKey(item.getType()));
		if (base <= 0.0) {
			return 0.0;
		}
		base *= silkTouchMarkService.getSellMultiplier(item);
		int refinementLevel = upgradeService.getHighestRefinementLevel(uuid);
		if (refinementLevel <= 0) {
			return base;
		}
		Material output = getRefinementOutput(item.getType());
		if (output == null) {
			return base;
		}
		double outputPrice = getSellPrice(ItemKeyUtil.toKey(output));
		if (outputPrice <= 0.0) {
			return base;
		}
		double feeMultiplier = 1.0 - getRefinementFee(refinementLevel);
		return outputPrice * feeMultiplier * silkTouchMarkService.getSellMultiplier(item);
	}

	private double getRefinementFee(int level) {
		return switch (Math.max(1, Math.min(5, level))) {
			case 1 -> 0.15;
			case 2 -> 0.12;
			case 3 -> 0.09;
			case 4 -> 0.07;
			default -> 0.05;
		};
	}

	private Material getRefinementOutput(Material input) {
		if (input == null) {
			return null;
		}
		return switch (input) {
			case IRON_ORE, DEEPSLATE_IRON_ORE, RAW_IRON -> Material.IRON_INGOT;
			case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE, RAW_GOLD -> Material.GOLD_INGOT;
			case COPPER_ORE, DEEPSLATE_COPPER_ORE, RAW_COPPER -> Material.COPPER_INGOT;
			case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
			default -> null;
		};
	}

	private SpecializationDomain getDomain(Material material) {
		if (material == null) {
			return SpecializationDomain.NONE;
		}
		String name = material.name().toUpperCase(Locale.ROOT);
		if (name.contains("_ORE") || name.startsWith("RAW_") || name.endsWith("_INGOT")) {
			return SpecializationDomain.MINER;
		}
		if (isFarmerMaterial(material, name)) {
			return SpecializationDomain.FARMER;
		}
		MarketItemTag tag = MarketItemTag.fromMaterial(material);
		if (tag == MarketItemTag.MOB) {
			return SpecializationDomain.HUNTER;
		}
		return SpecializationDomain.NONE;
	}

	private boolean isFarmerMaterial(Material material, String name) {
		if (name.endsWith("_SEEDS") || name.endsWith("_SAPLING") || name.endsWith("_FLOWER")) {
			return true;
		}
		return switch (material) {
			case WHEAT, CARROT, POTATO, BEETROOT, BEETROOT_SEEDS, MELON_SLICE, PUMPKIN, SUGAR_CANE, BAMBOO,
				 COCOA_BEANS, NETHER_WART, SWEET_BERRIES, KELP, CACTUS, WHEAT_SEEDS, MELON_SEEDS, PUMPKIN_SEEDS,
				 BROWN_MUSHROOM, RED_MUSHROOM, VINE -> true;
			default -> false;
		};
	}

	private enum SpecializationDomain {
		MINER,
		FARMER,
		HUNTER,
		NONE;

		static SpecializationDomain fromChoice(String choice) {
			if (choice == null) {
				return NONE;
			}
			return switch (choice.toUpperCase(Locale.ROOT)) {
				case "MINER" -> MINER;
				case "FARMER" -> FARMER;
				case "HUNTER" -> HUNTER;
				default -> NONE;
			};
		}
	}

	private double applyFatigueMultiplier(ItemStack item, double price) {
		if (soilFatigueService == null) {
			return price;
		}
		if (MarketItemTag.fromMaterial(item.getType()) != MarketItemTag.CROP) {
			return price;
		}
		return price * soilFatigueService.getMultiplier(item);
	}

	public PriceTable getPriceTable() {
		return priceTable;
	}

	public void reloadPrices() {
		priceCache.clear();
		priceCacheVersion = -1;
		missingKeys.clear();
		priceTable = new PriceTable(configManager.getPrices(), configManager.getOverrides());
	}

	public void validateCoverage() {
		int total = 0;
		int covered = 0;
		for (Material material : Material.values()) {
			if (material == Material.AIR) {
				continue;
			}
			total++;
			String key = ItemKeyUtil.toKey(material);
			if (priceTable.getEntry(key) != null) {
				covered++;
			}
		}
		if (total == 0) {
			return;
		}
		double ratio = (double) covered / total;
		if (ratio < 0.9) {
			Bukkit.getLogger().warning("[Ledger] Price coverage below 90%: " + covered + "/" + total);
		}
	}

	private void logMissingPrice(String key) {
		if (!missingKeys.add(key)) {
			return;
		}
		Bukkit.getLogger().warning("[Ledger] Missing price entry for key: " + key);
	}
}
