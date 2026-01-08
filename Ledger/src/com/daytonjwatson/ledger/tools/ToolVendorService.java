package com.daytonjwatson.ledger.tools;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class ToolVendorService {
	private final double globalBase;
	private final MoneyService moneyService;
	private final SpawnRegionService spawnRegionService;
	private final Map<ToolType, Double> typeWeights = new EnumMap<>(ToolType.class);
	private final Map<ToolTier, Double> tierMultipliers = new EnumMap<>(ToolTier.class);

	public ToolVendorService(ConfigManager configManager, MoneyService moneyService, SpawnRegionService spawnRegionService) {
		this.globalBase = configManager.getConfig().getDouble("tools.globalBase", 8000.0);
		this.moneyService = moneyService;
		this.spawnRegionService = spawnRegionService;
		loadDefaults();
	}

	private void loadDefaults() {
		typeWeights.put(ToolType.PICKAXE, 1.00);
		typeWeights.put(ToolType.AXE, 0.90);
		typeWeights.put(ToolType.SHOVEL, 0.75);
		typeWeights.put(ToolType.SWORD, 1.10);
		tierMultipliers.put(ToolTier.WOOD, 0.15);
		tierMultipliers.put(ToolTier.STONE, 0.45);
		tierMultipliers.put(ToolTier.IRON, 1.00);
		tierMultipliers.put(ToolTier.DIAMOND, 3.50);
		tierMultipliers.put(ToolTier.NETHERITE, 12.00);
	}

	public long getBuyPrice(ToolType type, ToolTier tier, ToolVariant variant) {
		double weight = typeWeights.getOrDefault(type, 1.0);
		double tierMultiplier = tierMultipliers.getOrDefault(tier, 1.0);
		double price = globalBase * weight * tierMultiplier;
		if (variant == ToolVariant.EFFICIENCY) {
			price *= 1.40;
		} else if (variant == ToolVariant.SILK_TOUCH) {
			price *= 1.25;
		}
		return Math.round(price);
	}

	public boolean purchaseTool(Player player, ToolType type, ToolTier tier, ToolVariant variant) {
		if (!spawnRegionService.isInSpawn(player.getLocation())) {
			return false;
		}
		long price = getBuyPrice(type, tier, variant);
		if (!moneyService.removeBanked(player, price)) {
			return false;
		}
		ItemStack item = new ItemStack(type.getMaterialForTier(tier));
		player.getInventory().addItem(item);
		return true;
	}

	public enum ToolType {
		PICKAXE,
		AXE,
		SHOVEL,
		SWORD;

		public Material getMaterialForTier(ToolTier tier) {
			return switch (this) {
				case PICKAXE -> switch (tier) {
					case WOOD -> Material.WOODEN_PICKAXE;
					case STONE -> Material.STONE_PICKAXE;
					case IRON -> Material.IRON_PICKAXE;
					case DIAMOND -> Material.DIAMOND_PICKAXE;
					case NETHERITE -> Material.NETHERITE_PICKAXE;
				};
				case AXE -> switch (tier) {
					case WOOD -> Material.WOODEN_AXE;
					case STONE -> Material.STONE_AXE;
					case IRON -> Material.IRON_AXE;
					case DIAMOND -> Material.DIAMOND_AXE;
					case NETHERITE -> Material.NETHERITE_AXE;
				};
				case SHOVEL -> switch (tier) {
					case WOOD -> Material.WOODEN_SHOVEL;
					case STONE -> Material.STONE_SHOVEL;
					case IRON -> Material.IRON_SHOVEL;
					case DIAMOND -> Material.DIAMOND_SHOVEL;
					case NETHERITE -> Material.NETHERITE_SHOVEL;
				};
				case SWORD -> switch (tier) {
					case WOOD -> Material.WOODEN_SWORD;
					case STONE -> Material.STONE_SWORD;
					case IRON -> Material.IRON_SWORD;
					case DIAMOND -> Material.DIAMOND_SWORD;
					case NETHERITE -> Material.NETHERITE_SWORD;
				};
			};
		}
	}

	public enum ToolTier {
		WOOD,
		STONE,
		IRON,
		DIAMOND,
		NETHERITE
	}

	public enum ToolVariant {
		STANDARD,
		EFFICIENCY,
		SILK_TOUCH
	}
}
