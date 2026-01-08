package com.daytonjwatson.ledger.market;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum MarketItemTag {
	ORE,
	CROP,
	MOB,
	INFRA;

	public static MarketItemTag fromMaterial(Material material) {
		if (material == null) {
			return INFRA;
		}
		String name = material.name();
		if (name.contains("ORE") || name.contains("INGOT") || name.contains("RAW_") || name.contains("NUGGET")
			|| name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("AMETHYST")) {
			return ORE;
		}
		if (CROP_ITEMS.contains(material)) {
			return CROP;
		}
		if (MOB_ITEMS.contains(material)) {
			return MOB;
		}
		return INFRA;
	}

	public static MarketItemTag fromKey(String key) {
		if (key == null) {
			return INFRA;
		}
		String normalized = key.toUpperCase(Locale.ROOT);
		if (normalized.startsWith("ENTITY:")) {
			return MOB;
		}
		if (normalized.startsWith("TAG:")) {
			String tagName = normalized.substring("TAG:".length());
			for (MarketItemTag tag : values()) {
				if (tag.name().equals(tagName)) {
					return tag;
				}
			}
			return INFRA;
		}
		Material material = Material.matchMaterial(normalized);
		return fromMaterial(material);
	}

	public String toTagKey() {
		return "TAG:" + name();
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
