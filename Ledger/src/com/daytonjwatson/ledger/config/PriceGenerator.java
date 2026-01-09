package com.daytonjwatson.ledger.config;

import com.daytonjwatson.ledger.market.ItemTagService;
import com.daytonjwatson.ledger.market.PriceBandTag;
import com.daytonjwatson.ledger.util.ItemKeyUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class PriceGenerator {
	private static final Logger LOGGER = Logger.getLogger(PriceGenerator.class.getName());
	private static final double MIN_MULTIPLIER = 0.05;
	private static final double MAX_MULTIPLIER = 50.0;
	private static final double NUGGET_MULTIPLIER = 0.13;
	private static final double STORAGE_BLOCK_MULTIPLIER = 9.0;
	private static final double HONEY_BLOCK_MULTIPLIER = 4.0;
	private static final EnumSet<Material> ORE_BLOCKS = EnumSet.of(
		Material.COAL_ORE,
		Material.COPPER_ORE,
		Material.IRON_ORE,
		Material.GOLD_ORE,
		Material.DIAMOND_ORE,
		Material.EMERALD_ORE,
		Material.LAPIS_ORE,
		Material.REDSTONE_ORE,
		Material.NETHER_GOLD_ORE,
		Material.NETHER_QUARTZ_ORE
	);
	private static final EnumSet<Material> DEEPSLATE_ORE_BLOCKS = EnumSet.of(
		Material.DEEPSLATE_COAL_ORE,
		Material.DEEPSLATE_COPPER_ORE,
		Material.DEEPSLATE_IRON_ORE,
		Material.DEEPSLATE_GOLD_ORE,
		Material.DEEPSLATE_DIAMOND_ORE,
		Material.DEEPSLATE_EMERALD_ORE,
		Material.DEEPSLATE_LAPIS_ORE,
		Material.DEEPSLATE_REDSTONE_ORE
	);
	private static final EnumSet<Material> RAW_ORES = EnumSet.of(
		Material.RAW_IRON,
		Material.RAW_GOLD,
		Material.RAW_COPPER
	);
	private static final EnumSet<Material> RAW_STORAGE_BLOCKS = EnumSet.of(
		Material.RAW_IRON_BLOCK,
		Material.RAW_GOLD_BLOCK,
		Material.RAW_COPPER_BLOCK
	);
	private static final EnumSet<Material> INGOTS = EnumSet.of(
		Material.IRON_INGOT,
		Material.GOLD_INGOT,
		Material.COPPER_INGOT,
		Material.NETHERITE_INGOT
	);
	private static final EnumSet<Material> NUGGETS = EnumSet.of(
		Material.IRON_NUGGET,
		Material.GOLD_NUGGET
	);
	private static final EnumSet<Material> GEMS = EnumSet.of(
		Material.DIAMOND,
		Material.EMERALD,
		Material.NETHER_QUARTZ_ORE,
		Material.AMETHYST_SHARD
	);
	private static final EnumSet<Material> STORAGE_BLOCKS = EnumSet.of(
		Material.IRON_BLOCK,
		Material.GOLD_BLOCK,
		Material.COPPER_BLOCK,
		Material.DIAMOND_BLOCK,
		Material.EMERALD_BLOCK,
		Material.NETHERITE_BLOCK,
		Material.COAL_BLOCK,
		Material.LAPIS_BLOCK,
		Material.REDSTONE_BLOCK
	);
	private static final EnumSet<Material> CROPS = EnumSet.of(
		Material.WHEAT,
		Material.CARROT,
		Material.POTATO,
		Material.BEETROOT,
		Material.MELON_SLICE,
		Material.SWEET_BERRIES,
		Material.COCOA_BEANS,
		Material.SUGAR_CANE
	);
	private static final EnumSet<Material> SIMPLE_FOODS = EnumSet.of(
		Material.BREAD,
		Material.BAKED_POTATO,
		Material.COOKIE
	);
	private static final EnumSet<Material> COOKED_MEATS = EnumSet.of(
		Material.COOKED_BEEF,
		Material.COOKED_PORKCHOP,
		Material.COOKED_CHICKEN,
		Material.COOKED_MUTTON,
		Material.COOKED_RABBIT,
		Material.COOKED_COD,
		Material.COOKED_SALMON
	);
	private static final EnumSet<Material> COMPOSITE_FOODS = EnumSet.of(
		Material.PUMPKIN_PIE,
		Material.RABBIT_STEW,
		Material.MUSHROOM_STEW,
		Material.BEETROOT_SOUP
	);
	private static final EnumSet<Material> SUGAR_CHAIN = EnumSet.of(
		Material.SUGAR,
		Material.PAPER,
		Material.BOOK
	);
	private static final EnumSet<Material> LOGS = EnumSet.of(
		Material.OAK_LOG,
		Material.SPRUCE_LOG,
		Material.BIRCH_LOG,
		Material.JUNGLE_LOG,
		Material.ACACIA_LOG,
		Material.DARK_OAK_LOG,
		Material.MANGROVE_LOG,
		Material.CHERRY_LOG,
		Material.CRIMSON_STEM,
		Material.WARPED_STEM,
		Material.BAMBOO_BLOCK
	);
	private static final EnumSet<Material> WOOD_BLOCKS = EnumSet.of(
		Material.OAK_WOOD,
		Material.SPRUCE_WOOD,
		Material.BIRCH_WOOD,
		Material.JUNGLE_WOOD,
		Material.ACACIA_WOOD,
		Material.DARK_OAK_WOOD,
		Material.MANGROVE_WOOD,
		Material.CHERRY_WOOD,
		Material.CRIMSON_HYPHAE,
		Material.WARPED_HYPHAE,
		Material.STRIPPED_OAK_WOOD,
		Material.STRIPPED_SPRUCE_WOOD,
		Material.STRIPPED_BIRCH_WOOD,
		Material.STRIPPED_JUNGLE_WOOD,
		Material.STRIPPED_ACACIA_WOOD,
		Material.STRIPPED_DARK_OAK_WOOD,
		Material.STRIPPED_MANGROVE_WOOD,
		Material.STRIPPED_CHERRY_WOOD,
		Material.STRIPPED_CRIMSON_HYPHAE,
		Material.STRIPPED_WARPED_HYPHAE,
		Material.STRIPPED_BAMBOO_BLOCK
	);
	private static final EnumSet<Material> STRIPPED_LOGS = EnumSet.of(
		Material.STRIPPED_OAK_LOG,
		Material.STRIPPED_SPRUCE_LOG,
		Material.STRIPPED_BIRCH_LOG,
		Material.STRIPPED_JUNGLE_LOG,
		Material.STRIPPED_ACACIA_LOG,
		Material.STRIPPED_DARK_OAK_LOG,
		Material.STRIPPED_MANGROVE_LOG,
		Material.STRIPPED_CHERRY_LOG,
		Material.STRIPPED_CRIMSON_STEM,
		Material.STRIPPED_WARPED_STEM
	);
	private static final EnumSet<Material> PLANKS = EnumSet.of(
		Material.OAK_PLANKS,
		Material.SPRUCE_PLANKS,
		Material.BIRCH_PLANKS,
		Material.JUNGLE_PLANKS,
		Material.ACACIA_PLANKS,
		Material.DARK_OAK_PLANKS,
		Material.MANGROVE_PLANKS,
		Material.CHERRY_PLANKS,
		Material.CRIMSON_PLANKS,
		Material.WARPED_PLANKS,
		Material.BAMBOO_PLANKS,
		Material.BAMBOO_MOSAIC
	);
	private static final EnumSet<Material> STONE_BASE = EnumSet.of(
		Material.COBBLESTONE,
		Material.STONE,
		Material.ANDESITE,
		Material.DIORITE,
		Material.GRANITE,
		Material.DEEPSLATE,
		Material.BASALT,
		Material.BLACKSTONE,
		Material.NETHERRACK,
		Material.END_STONE,
		Material.SANDSTONE,
		Material.RED_SANDSTONE
	);
	private static final EnumSet<Material> PRISMARINE_BASE = EnumSet.of(
		Material.PRISMARINE,
		Material.PRISMARINE_BRICKS,
		Material.DARK_PRISMARINE
	);
	private static final EnumSet<Material> REDSTONE_COMPONENTS = EnumSet.of(
		Material.REDSTONE,
		Material.REPEATER,
		Material.COMPARATOR,
		Material.PISTON,
		Material.STICKY_PISTON,
		Material.OBSERVER,
		Material.DROPPER,
		Material.DISPENSER,
		Material.HOPPER,
		Material.RAIL,
		Material.POWERED_RAIL,
		Material.DETECTOR_RAIL,
		Material.LANTERN,
		Material.SOUL_LANTERN,
		Material.TORCH,
		Material.SOUL_TORCH,
		Material.NOTE_BLOCK,
		Material.JUKEBOX
	);
	private static final EnumSet<Material> CONTAINERS_AND_UTILITIES = EnumSet.of(
		Material.CHEST,
		Material.BARREL,
		Material.FURNACE,
		Material.BLAST_FURNACE,
		Material.SMOKER,
		Material.ANVIL,
		Material.CHIPPED_ANVIL,
		Material.DAMAGED_ANVIL,
		Material.CRAFTING_TABLE,
		Material.BREWING_STAND
	);
	private static final Set<String> WOOD_PREFIXES = Set.of(
		"OAK_",
		"SPRUCE_",
		"BIRCH_",
		"JUNGLE_",
		"ACACIA_",
		"DARK_OAK_",
		"MANGROVE_",
		"CHERRY_",
		"CRIMSON_",
		"WARPED_",
		"BAMBOO_"
	);
	private final ItemTagService itemTagService;
	private final PriceBandTable bandTable;
	private final boolean debugProcessing;

	public PriceGenerator(ItemTagService itemTagService, PriceBandTable bandTable) {
		this(itemTagService, bandTable, false);
	}

	public PriceGenerator(ItemTagService itemTagService, PriceBandTable bandTable, boolean debugProcessing) {
		this.itemTagService = itemTagService;
		this.bandTable = bandTable;
		this.debugProcessing = debugProcessing;
	}

	public YamlConfiguration generate() {
		YamlConfiguration yaml = new YamlConfiguration();
		ConfigurationSection pricesSection = yaml.createSection("prices");
		List<Material> materials = List.of(Material.values());
		List<String> keys = new ArrayList<>();
		for (Material material : materials) {
			String key = ItemKeyUtil.toKey(material);
			if (key != null) {
				keys.add(key);
			}
		}
		keys.sort(Comparator.naturalOrder());
		for (String key : keys) {
			Material material = Material.matchMaterial(key);
			if (material == null) {
				continue;
			}
			PriceBandTag tag = itemTagService.getBaseTag(material);
			PriceBandTable.PriceBand band = bandTable.getBand(tag);
			if (band == null) {
				band = new PriceBandTable.PriceBand(0.0, 0.0, 1.0, 1.0, 0.0);
			}
			double jitter = computeJitter(material);
			double price = band.minPrice() + jitter * (band.maxPrice() - band.minPrice());
			double preMultiplierBase = roundTwoDecimals(price);
			ProcessingResult processing = processingMultiplier(material, tag);
			price = price * processing.multiplier();
			double base = roundTwoDecimals(price);
			if (debugProcessing) {
				LOGGER.info(() -> "PriceGen material=" + material.name()
					+ " tag=" + tag.name()
					+ " base=" + preMultiplierBase
					+ " multiplier=" + processing.multiplier()
					+ " final=" + base
					+ " reason=" + processing.reason());
			}
			ConfigurationSection entry = pricesSection.createSection(key);
			entry.set("base", base);
			entry.set("cap", band.cap());
			entry.set("sigma", band.sigma());
			entry.set("tag", tag.name());
			if (band.baseline() > 0.0) {
				entry.set("baseline", band.baseline());
			}
			if (tag == PriceBandTag.UNSELLABLE || tag == PriceBandTag.ENCHANTED) {
				entry.set("unsellable", true);
			}
		}
		return yaml;
	}

	public void write(File file, YamlConfiguration existing) throws IOException {
		YamlConfiguration generated = generate();
		if (existing != null && existing.getConfigurationSection("mobPrices") != null) {
			generated.set("mobPrices", existing.get("mobPrices"));
		}
		String content = generated.saveToString();
		com.daytonjwatson.ledger.util.AtomicFileWriter.writeAtomically(file, content.getBytes(StandardCharsets.UTF_8));
	}

	public Map<PriceBandTag, PriceSummary> summarize(YamlConfiguration yaml) {
		ConfigurationSection pricesSection = yaml.getConfigurationSection("prices");
		java.util.Map<PriceBandTag, PriceSummary> summary = new java.util.EnumMap<>(PriceBandTag.class);
		if (pricesSection == null) {
			return summary;
		}
		for (String key : pricesSection.getKeys(false)) {
			ConfigurationSection entry = pricesSection.getConfigurationSection(key);
			if (entry == null) {
				continue;
			}
			String tagValue = entry.getString("tag", PriceBandTag.TRASH_COMMON.name());
			PriceBandTag tag;
			try {
				tag = PriceBandTag.valueOf(tagValue.toUpperCase());
			} catch (IllegalArgumentException e) {
				continue;
			}
			double base = entry.getDouble("base", 0.0);
			PriceSummary current = summary.computeIfAbsent(tag, ignored -> new PriceSummary());
			current.add(base);
		}
		return summary;
	}

	private double computeJitter(Material material) {
		byte[] hash = sha1(material.name());
		int value = ((hash[0] & 0xFF) << 24)
			| ((hash[1] & 0xFF) << 16)
			| ((hash[2] & 0xFF) << 8)
			| (hash[3] & 0xFF);
		int normalized = Math.abs(value % 10000);
		return normalized / 10000.0;
	}

	private byte[] sha1(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			return digest.digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			return value.getBytes(StandardCharsets.UTF_8);
		}
	}

	private double roundTwoDecimals(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	private ProcessingResult processingMultiplier(Material material, PriceBandTag tag) {
		List<String> reasons = new ArrayList<>();
		Double forced = forcedMultiplier(material, reasons);
		double multiplier = forced != null ? forced : 1.0;

		if (forced == null) {
			if (isOreBlock(material)) {
				multiplier = maxMultiplier(multiplier, 0.85, reasons, "ore_block");
			}
			if (isDeepslateOre(material)) {
				multiplier = maxMultiplier(multiplier, 0.90, reasons, "deepslate_ore");
			}
			if (isRawOre(material)) {
				multiplier = maxMultiplier(multiplier, 1.00, reasons, "raw_ore");
			}
			if (isIngot(material)) {
				double ingotMultiplier = material == Material.NETHERITE_INGOT ? 1.35 : 1.20;
				multiplier = maxMultiplier(multiplier, ingotMultiplier, reasons, "ingot");
			}
			if (isGem(material)) {
				double gemMultiplier = material == Material.NETHER_QUARTZ_ORE ? 1.05 : 1.15;
				multiplier = maxMultiplier(multiplier, gemMultiplier, reasons, "gem");
			}
			multiplier = maxMultiplier(multiplier, cropMultiplier(material, reasons), reasons, null);
			multiplier = maxMultiplier(multiplier, woodMultiplier(material, reasons), reasons, null);
			multiplier = maxMultiplier(multiplier, buildingMultiplier(material, reasons), reasons, null);
			multiplier = maxMultiplier(multiplier, glassAndClayMultiplier(material, reasons), reasons, null);
			multiplier = maxMultiplier(multiplier, redstoneMultiplier(material, reasons), reasons, null);
			multiplier = maxMultiplier(multiplier, toolArmorMultiplier(material, reasons), reasons, null);
			multiplier = maxMultiplier(multiplier, containerMultiplier(material, reasons), reasons, null);
		}

		double bandMax = bandMaxMultiplier(tag);
		if (multiplier > bandMax) {
			multiplier = bandMax;
			reasons.add("band_cap");
		}
		if (multiplier < MIN_MULTIPLIER) {
			multiplier = MIN_MULTIPLIER;
			reasons.add("hard_floor");
		} else if (multiplier > MAX_MULTIPLIER) {
			multiplier = MAX_MULTIPLIER;
			reasons.add("hard_cap");
		}

		return new ProcessingResult(multiplier, String.join(", ", reasons));
	}

	private String processingMultiplierReason(Material material) {
		PriceBandTag tag = itemTagService.getBaseTag(material);
		return processingMultiplier(material, tag).reason();
	}

	private Double forcedMultiplier(Material material, List<String> reasons) {
		if (isStorageBlock(material) || isRawStorageBlock(material)) {
			reasons.add("storage_block");
			return STORAGE_BLOCK_MULTIPLIER;
		}
		if (isNugget(material)) {
			reasons.add("nugget");
			return NUGGET_MULTIPLIER;
		}
		if (material == Material.HONEY_BLOCK || material == Material.HONEYCOMB_BLOCK) {
			reasons.add("honey_block");
			return HONEY_BLOCK_MULTIPLIER;
		}
		return null;
	}

	private double cropMultiplier(Material material, List<String> reasons) {
		if (CROPS.contains(material)) {
			reasons.add("crop");
			return 1.00;
		}
		if (SIMPLE_FOODS.contains(material)) {
			reasons.add("food_simple");
			return 1.15;
		}
		if (COOKED_MEATS.contains(material)) {
			reasons.add("food_cooked");
			return 1.18;
		}
		if (COMPOSITE_FOODS.contains(material)) {
			reasons.add("food_composite");
			return 1.25;
		}
		if (SUGAR_CHAIN.contains(material)) {
			double value = switch (material) {
				case SUGAR -> 1.10;
				case PAPER -> 1.15;
				case BOOK -> 1.25;
				default -> 1.0;
			};
			reasons.add("sugar_chain");
			return value;
		}
		if (material == Material.HONEYCOMB) {
			reasons.add("honey_comb");
			return 1.05;
		}
		if (material == Material.HONEY_BOTTLE) {
			reasons.add("honey_bottle");
			return 1.12;
		}
		return 1.0;
	}

	private double woodMultiplier(Material material, List<String> reasons) {
		if (LOGS.contains(material) || WOOD_BLOCKS.contains(material)) {
			reasons.add("wood_log");
			return 1.00;
		}
		if (STRIPPED_LOGS.contains(material)) {
			reasons.add("wood_stripped");
			return 1.05;
		}
		if (PLANKS.contains(material)) {
			reasons.add("wood_planks");
			return 1.08;
		}
		if (material == Material.STICK) {
			reasons.add("wood_stick");
			return 1.10;
		}
		if (isWoodFamily(material) && isWoodBasicShape(material)) {
			reasons.add("wood_shape");
			return 1.15;
		}
		if (isWoodFamily(material) && isWoodControlShape(material)) {
			reasons.add("wood_control");
			return 1.12;
		}
		if (isWoodFamily(material) && isBoat(material)) {
			reasons.add("wood_boat");
			return 1.20;
		}
		return 1.0;
	}

	private double buildingMultiplier(Material material, List<String> reasons) {
		double familyMultiplier = 1.0;
		double shapeMultiplier = 1.0;

		if (STONE_BASE.contains(material)) {
			familyMultiplier = 1.00;
		}
		if (isPrismarine(material)) {
			familyMultiplier = material == Material.PRISMARINE ? 1.05 : 1.12;
			reasons.add("prismarine");
		}
		if (isNetherBricks(material)) {
			familyMultiplier = material == Material.NETHER_BRICKS ? 1.12 : 1.15;
			reasons.add("nether_bricks");
		}
		if (isChiseledOrCracked(material)) {
			familyMultiplier = Math.max(familyMultiplier, 1.18);
			reasons.add("chiseled");
		}
		if (isBrickOrTile(material)) {
			familyMultiplier = Math.max(familyMultiplier, 1.15);
			reasons.add("brick_tile");
		}
		if (isSmoothVariant(material)) {
			familyMultiplier = Math.max(familyMultiplier, 1.12);
			reasons.add("smooth");
		}
		if (isPolishedVariant(material)) {
			familyMultiplier = Math.max(familyMultiplier, 1.10);
			reasons.add("polished");
		}
		if (isCutSandstone(material)) {
			familyMultiplier = Math.max(familyMultiplier, 1.12);
			reasons.add("cut");
		}
		if (isFancyMossyVariant(material)) {
			familyMultiplier = Math.max(familyMultiplier, 1.12);
			reasons.add("mossy");
		}

		if (isShapeVariant(material)) {
			shapeMultiplier = 1.12;
			reasons.add("shape");
		}

		return familyMultiplier * shapeMultiplier;
	}

	private double glassAndClayMultiplier(Material material, List<String> reasons) {
		if (isGlass(material)) {
			reasons.add("glass");
			return 1.10;
		}
		if (isGlassPane(material)) {
			reasons.add("glass_pane");
			return 1.12;
		}
		if (isTerracotta(material)) {
			reasons.add("terracotta");
			return 1.10;
		}
		if (isGlazedTerracotta(material)) {
			reasons.add("glazed_terracotta");
			return 1.18;
		}
		if (isConcretePowder(material)) {
			reasons.add("concrete_powder");
			return 1.10;
		}
		if (isConcrete(material)) {
			reasons.add("concrete");
			return 1.12;
		}
		if (isDye(material)) {
			reasons.add("dye");
			return 1.00;
		}
		return 1.0;
	}

	private double redstoneMultiplier(Material material, List<String> reasons) {
		if (!REDSTONE_COMPONENTS.contains(material)) {
			return 1.0;
		}
		double value = switch (material) {
			case REDSTONE -> 1.00;
			case REPEATER, COMPARATOR -> 1.30;
			case PISTON, STICKY_PISTON -> 1.28;
			case OBSERVER, DROPPER, DISPENSER -> 1.30;
			case HOPPER -> 1.40;
			case RAIL -> 1.25;
			case POWERED_RAIL, DETECTOR_RAIL -> 1.35;
			case LANTERN, SOUL_LANTERN -> 1.18;
			case TORCH -> 1.10;
			case SOUL_TORCH -> 1.12;
			case NOTE_BLOCK -> 1.25;
			case JUKEBOX -> 1.35;
			default -> 1.25;
		};
		reasons.add("redstone");
		return value;
	}

	private double toolArmorMultiplier(Material material, List<String> reasons) {
		if (isTool(material) || isArmor(material) || isWeapon(material)) {
			reasons.add("crafted_gear");
			return 1.25;
		}
		return 1.0;
	}

	private double containerMultiplier(Material material, List<String> reasons) {
		if (CONTAINERS_AND_UTILITIES.contains(material)) {
			reasons.add("utility");
			return 1.20;
		}
		return 1.0;
	}

	private double maxMultiplier(double current, double candidate, List<String> reasons, String reason) {
		if (candidate <= current) {
			return current;
		}
		if (reason != null) {
			reasons.add(reason);
		}
		return candidate;
	}

	private double bandMaxMultiplier(PriceBandTag tag) {
		return switch (Objects.requireNonNullElse(tag, PriceBandTag.TRASH_COMMON)) {
			case TRASH_COMMON -> 2.0;
			case COMMON_BUILD, COMMON_NATURAL, FARM_COMMON, MOB_COMMON -> 5.0;
			case ORE_COMMON, FARM_VALUABLE, MOB_VALUABLE, UTILITY_INFRA, CONTAINER -> 8.0;
			case ORE_MID, ORE_RARE, END_RARE -> 12.0;
			case NETHER_RARE, REDSTONE_INFRA -> 20.0;
			case ENCHANTED, UNSELLABLE -> 2.0;
			default -> 5.0;
		};
	}

	private boolean isOreBlock(Material material) {
		return ORE_BLOCKS.contains(material);
	}

	private boolean isDeepslateOre(Material material) {
		return DEEPSLATE_ORE_BLOCKS.contains(material);
	}

	private boolean isRawOre(Material material) {
		return RAW_ORES.contains(material);
	}

	private boolean isRawStorageBlock(Material material) {
		return RAW_STORAGE_BLOCKS.contains(material);
	}

	private boolean isIngot(Material material) {
		return INGOTS.contains(material);
	}

	private boolean isNugget(Material material) {
		return NUGGETS.contains(material);
	}

	private boolean isGem(Material material) {
		return GEMS.contains(material);
	}

	private boolean isStorageBlock(Material material) {
		return STORAGE_BLOCKS.contains(material);
	}

	private boolean isWoodFamily(Material material) {
		String name = material.name();
		if (name.startsWith("STRIPPED_")) {
			name = name.substring("STRIPPED_".length());
		}
		for (String prefix : WOOD_PREFIXES) {
			if (name.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private boolean isWoodBasicShape(Material material) {
		String name = material.name();
		return name.endsWith("_SLAB")
			|| name.endsWith("_STAIRS")
			|| name.endsWith("_FENCE")
			|| name.endsWith("_FENCE_GATE")
			|| name.endsWith("_DOOR")
			|| name.endsWith("_TRAPDOOR")
			|| name.endsWith("_SIGN")
			|| name.endsWith("_HANGING_SIGN");
	}

	private boolean isWoodControlShape(Material material) {
		String name = material.name();
		return name.endsWith("_PRESSURE_PLATE")
			|| name.endsWith("_BUTTON");
	}

	private boolean isBoat(Material material) {
		String name = material.name();
		return name.endsWith("_BOAT") || name.endsWith("_CHEST_BOAT") || name.endsWith("_RAFT")
			|| name.endsWith("_CHEST_RAFT");
	}

	private boolean isShapeVariant(Material material) {
		String name = material.name();
		return name.endsWith("_SLAB") || name.endsWith("_STAIRS") || name.endsWith("_WALL");
	}

	private boolean isSmoothVariant(Material material) {
		String name = material.name();
		return name.startsWith("SMOOTH_") || material == Material.SMOOTH_STONE;
	}

	private boolean isPolishedVariant(Material material) {
		return material.name().startsWith("POLISHED_");
	}

	private boolean isCutSandstone(Material material) {
		return material == Material.CUT_SANDSTONE || material == Material.CUT_RED_SANDSTONE;
	}

	private boolean isBrickOrTile(Material material) {
		String name = material.name();
		return name.endsWith("_BRICKS") || name.endsWith("_TILES");
	}

	private boolean isChiseledOrCracked(Material material) {
		String name = material.name();
		return name.startsWith("CHISELED_") || name.startsWith("CRACKED_");
	}

	private boolean isFancyMossyVariant(Material material) {
		return material.name().startsWith("MOSSY_");
	}

	private boolean isPrismarine(Material material) {
		return PRISMARINE_BASE.contains(material);
	}

	private boolean isNetherBricks(Material material) {
		return material == Material.NETHER_BRICKS || material == Material.RED_NETHER_BRICKS
			|| material == Material.CHISELED_NETHER_BRICKS || material == Material.CRACKED_NETHER_BRICKS;
	}

	private boolean isGlass(Material material) {
		String name = material.name();
		return material == Material.GLASS || material == Material.TINTED_GLASS || name.endsWith("_STAINED_GLASS");
	}

	private boolean isGlassPane(Material material) {
		String name = material.name();
		return material == Material.GLASS_PANE || name.endsWith("_GLASS_PANE");
	}

	private boolean isTerracotta(Material material) {
		String name = material.name();
		return material == Material.TERRACOTTA || (name.endsWith("_TERRACOTTA") && !name.endsWith("_GLAZED_TERRACOTTA"));
	}

	private boolean isGlazedTerracotta(Material material) {
		return material.name().endsWith("_GLAZED_TERRACOTTA");
	}

	private boolean isConcretePowder(Material material) {
		return material.name().endsWith("_CONCRETE_POWDER");
	}

	private boolean isConcrete(Material material) {
		return material.name().endsWith("_CONCRETE") && !material.name().endsWith("_CONCRETE_POWDER");
	}

	private boolean isDye(Material material) {
		return material.name().endsWith("_DYE");
	}

	private boolean isTool(Material material) {
		String name = material.name();
		return name.endsWith("_PICKAXE")
			|| name.endsWith("_AXE")
			|| name.endsWith("_SHOVEL")
			|| name.endsWith("_HOE");
	}

	private boolean isArmor(Material material) {
		String name = material.name();
		return name.endsWith("_HELMET")
			|| name.endsWith("_CHESTPLATE")
			|| name.endsWith("_LEGGINGS")
			|| name.endsWith("_BOOTS");
	}

	private boolean isWeapon(Material material) {
		return material.name().endsWith("_SWORD")
			|| material == Material.BOW
			|| material == Material.CROSSBOW
			|| material == Material.TRIDENT;
	}

	private record ProcessingResult(double multiplier, String reason) {
	}

	public static class PriceSummary {
		private double min = Double.MAX_VALUE;
		private double max = 0.0;
		private double total = 0.0;
		private int count = 0;

		public void add(double value) {
			min = Math.min(min, value);
			max = Math.max(max, value);
			total += value;
			count++;
		}

		public double getMin() {
			return count == 0 ? 0.0 : min;
		}

		public double getMax() {
			return max;
		}

		public double getAverage() {
			return count == 0 ? 0.0 : total / count;
		}

		public int getCount() {
			return count;
		}
	}
}
