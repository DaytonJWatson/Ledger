package com.daytonjwatson.ledger.market;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public class ItemTagService {
	private static final Set<Material> TRASH_COMMON = EnumSet.of(
		Material.DIRT,
		Material.COARSE_DIRT,
		Material.ROOTED_DIRT,
		Material.GRAVEL,
		Material.SAND,
		Material.RED_SAND,
		Material.COBBLESTONE,
		Material.NETHERRACK,
		Material.TUFF,
		Material.COBBLED_DEEPSLATE
	);

	private static final Set<Material> COMMON_NATURAL = EnumSet.of(
		Material.STONE,
		Material.GRANITE,
		Material.DIORITE,
		Material.ANDESITE,
		Material.CALCITE,
		Material.DRIPSTONE_BLOCK,
		Material.OAK_LOG,
		Material.SPRUCE_LOG,
		Material.BIRCH_LOG,
		Material.JUNGLE_LOG,
		Material.ACACIA_LOG,
		Material.DARK_OAK_LOG,
		Material.MANGROVE_LOG,
		Material.CHERRY_LOG,
		Material.OAK_LEAVES,
		Material.SPRUCE_LEAVES,
		Material.BIRCH_LEAVES,
		Material.JUNGLE_LEAVES,
		Material.ACACIA_LEAVES,
		Material.DARK_OAK_LEAVES,
		Material.MANGROVE_LEAVES,
		Material.CHERRY_LEAVES,
		Material.CLAY
	);

	private static final Set<Material> COMMON_BUILD = EnumSet.of(
		Material.OAK_PLANKS,
		Material.SPRUCE_PLANKS,
		Material.BIRCH_PLANKS,
		Material.JUNGLE_PLANKS,
		Material.ACACIA_PLANKS,
		Material.DARK_OAK_PLANKS,
		Material.MANGROVE_PLANKS,
		Material.CHERRY_PLANKS,
		Material.STONE_BRICKS,
		Material.GLASS,
		Material.BRICKS,
		Material.COBBLESTONE_STAIRS,
		Material.STONE_SLAB,
		Material.OAK_SLAB,
		Material.OAK_STAIRS,
		Material.STONE_STAIRS,
		Material.WHITE_WOOL,
		Material.GLASS_PANE,
		Material.WHITE_TERRACOTTA
	);

	private static final Set<Material> FARM_COMMON = EnumSet.of(
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
		Material.SWEET_BERRIES,
		Material.KELP,
		Material.CACTUS
	);

	private static final Set<Material> FARM_VALUABLE = EnumSet.of(
		Material.HONEYCOMB,
		Material.HONEY_BOTTLE
	);

	private static final Set<Material> MOB_COMMON = EnumSet.of(
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
		Material.FEATHER,
		Material.LEATHER
	);

	private static final Set<Material> MOB_VALUABLE = EnumSet.of(
		Material.BLAZE_ROD,
		Material.GHAST_TEAR,
		Material.ENDER_PEARL,
		Material.MAGMA_CREAM,
		Material.SHULKER_SHELL
	);

	private static final Set<Material> REDSTONE_COMPONENTS = EnumSet.of(
		Material.REDSTONE,
		Material.REDSTONE_TORCH,
		Material.REDSTONE_BLOCK,
		Material.REPEATER,
		Material.COMPARATOR,
		Material.OBSERVER,
		Material.PISTON,
		Material.STICKY_PISTON,
		Material.DISPENSER,
		Material.DROPPER,
		Material.HOPPER,
		Material.DAYLIGHT_DETECTOR,
		Material.TRIPWIRE_HOOK,
		Material.NOTE_BLOCK,
		Material.TARGET
	);

	private static final Set<Material> UTILITY_INFRA = EnumSet.of(
		Material.CHEST,
		Material.BARREL,
		Material.FURNACE,
		Material.CRAFTING_TABLE,
		Material.LADDER,
		Material.TORCH,
		Material.RAIL,
		Material.POWERED_RAIL,
		Material.MINECART,
		Material.CHEST_MINECART,
		Material.FURNACE_MINECART,
		Material.BOOKSHELF,
		Material.ANVIL,
		Material.GRINDSTONE,
		Material.SMOKER,
		Material.BLAST_FURNACE
	);

	private static final Set<Material> ADMIN_BLOCKS = EnumSet.of(
		Material.BARRIER,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.STRUCTURE_BLOCK,
		Material.STRUCTURE_VOID,
		Material.JIGSAW,
		Material.LIGHT,
		Material.DEBUG_STICK,
		Material.BEDROCK
	);

	private static final Set<Material> CONTAINERS = EnumSet.of(
		Material.BUNDLE,
		Material.SHULKER_BOX,
		Material.WHITE_SHULKER_BOX,
		Material.ORANGE_SHULKER_BOX,
		Material.MAGENTA_SHULKER_BOX,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.YELLOW_SHULKER_BOX,
		Material.LIME_SHULKER_BOX,
		Material.PINK_SHULKER_BOX,
		Material.GRAY_SHULKER_BOX,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.CYAN_SHULKER_BOX,
		Material.PURPLE_SHULKER_BOX,
		Material.BLUE_SHULKER_BOX,
		Material.BROWN_SHULKER_BOX,
		Material.GREEN_SHULKER_BOX,
		Material.RED_SHULKER_BOX,
		Material.BLACK_SHULKER_BOX
	);

	public PriceBandTag getBaseTag(Material material) {
		if (material == null || material == Material.AIR) {
			return PriceBandTag.UNSELLABLE;
		}
		String name = material.name().toUpperCase(Locale.ROOT);
		if (ADMIN_BLOCKS.contains(material)) {
			return PriceBandTag.UNSELLABLE;
		}
		if (name.endsWith("_SPAWN_EGG")) {
			return PriceBandTag.UNSELLABLE;
		}
		if (name.contains("POTION") || name.equals("TIPPED_ARROW")) {
			return PriceBandTag.UNSELLABLE;
		}
		if (material == Material.ENCHANTED_BOOK) {
			return PriceBandTag.ENCHANTED;
		}
		if (material == Material.WRITTEN_BOOK || material == Material.FILLED_MAP || material == Material.PLAYER_HEAD) {
			return PriceBandTag.UNSELLABLE;
		}
		if (CONTAINERS.contains(material)) {
			return PriceBandTag.CONTAINER;
		}
		if (material == Material.ANCIENT_DEBRIS || name.contains("NETHERITE")) {
			return PriceBandTag.NETHER_RARE;
		}
		if (material == Material.END_STONE || material == Material.END_STONE_BRICKS
			|| material == Material.CHORUS_FRUIT || material == Material.CHORUS_FLOWER
			|| material == Material.CHORUS_PLANT || material == Material.DRAGON_BREATH) {
			return PriceBandTag.END_RARE;
		}
		if (isRareOre(material, name)) {
			return PriceBandTag.ORE_RARE;
		}
		if (isMidOre(material, name)) {
			return PriceBandTag.ORE_MID;
		}
		if (isCommonOre(material, name)) {
			return PriceBandTag.ORE_COMMON;
		}
		if (REDSTONE_COMPONENTS.contains(material) || name.contains("REDSTONE")) {
			return PriceBandTag.REDSTONE_INFRA;
		}
		if (UTILITY_INFRA.contains(material)) {
			return PriceBandTag.UTILITY_INFRA;
		}
		if (MOB_VALUABLE.contains(material)) {
			return PriceBandTag.MOB_VALUABLE;
		}
		if (MOB_COMMON.contains(material)) {
			return PriceBandTag.MOB_COMMON;
		}
		if (FARM_VALUABLE.contains(material)) {
			return PriceBandTag.FARM_VALUABLE;
		}
		if (FARM_COMMON.contains(material)) {
			return PriceBandTag.FARM_COMMON;
		}
		if (TRASH_COMMON.contains(material)) {
			return PriceBandTag.TRASH_COMMON;
		}
		if (COMMON_BUILD.contains(material) || name.endsWith("_BRICKS") || name.endsWith("_PLANKS")) {
			return PriceBandTag.COMMON_BUILD;
		}
		if (COMMON_NATURAL.contains(material) || name.endsWith("_LOG") || name.endsWith("_LEAVES")) {
			return PriceBandTag.COMMON_NATURAL;
		}
		return PriceBandTag.TRASH_COMMON;
	}

	private boolean isRareOre(Material material, String name) {
		return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE
			|| material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE
			|| name.contains("DIAMOND") || name.contains("EMERALD");
	}

	private boolean isMidOre(Material material, String name) {
		return material == Material.GOLD_ORE || material == Material.DEEPSLATE_GOLD_ORE
			|| material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE
			|| material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE
			|| material == Material.NETHER_GOLD_ORE || material == Material.NETHER_QUARTZ_ORE
			|| name.contains("GOLD") || name.contains("LAPIS")
			|| name.contains("QUARTZ") || name.contains("AMETHYST");
	}

	private boolean isCommonOre(Material material, String name) {
		return material == Material.COAL_ORE || material == Material.DEEPSLATE_COAL_ORE
			|| material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE
			|| material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE
			|| name.contains("IRON") || name.contains("COPPER") || name.contains("COAL");
	}
}
