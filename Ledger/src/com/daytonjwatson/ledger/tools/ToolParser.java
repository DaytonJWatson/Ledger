package com.daytonjwatson.ledger.tools;

import org.bukkit.Material;

public final class ToolParser {
	private ToolParser() {
	}

	public static ToolVendorService.ToolTier getTier(Material material) {
		if (material == null) {
			return null;
		}
		String name = material.name();
		if (name.startsWith("WOODEN_")) {
			return ToolVendorService.ToolTier.WOOD;
		}
		if (name.startsWith("STONE_")) {
			return ToolVendorService.ToolTier.STONE;
		}
		if (name.startsWith("IRON_")) {
			return ToolVendorService.ToolTier.IRON;
		}
		if (name.startsWith("DIAMOND_")) {
			return ToolVendorService.ToolTier.DIAMOND;
		}
		if (name.startsWith("NETHERITE_")) {
			return ToolVendorService.ToolTier.NETHERITE;
		}
		return null;
	}

	public static ToolVendorService.ToolType getType(Material material) {
		if (material == null) {
			return null;
		}
		String name = material.name();
		if (name.endsWith("_PICKAXE")) {
			return ToolVendorService.ToolType.PICKAXE;
		}
		if (name.endsWith("_AXE")) {
			return ToolVendorService.ToolType.AXE;
		}
		if (name.endsWith("_SHOVEL")) {
			return ToolVendorService.ToolType.SHOVEL;
		}
		if (name.endsWith("_SWORD")) {
			return ToolVendorService.ToolType.SWORD;
		}
		return null;
	}
}
