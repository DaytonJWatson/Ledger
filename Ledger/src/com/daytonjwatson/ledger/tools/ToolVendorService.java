package com.daytonjwatson.ledger.tools;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import com.daytonjwatson.ledger.upgrades.UpgradeService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class ToolVendorService {
	private final double globalBase;
	private final MoneyService moneyService;
	private final SpawnRegionService spawnRegionService;
	private final UpgradeService upgradeService;
	private final ToolMetaService toolMetaService;
	private final Map<ToolType, Double> typeWeights = new EnumMap<>(ToolType.class);
	private final Map<ToolTier, Double> tierMultipliers = new EnumMap<>(ToolTier.class);

	public ToolVendorService(ConfigManager configManager, MoneyService moneyService, SpawnRegionService spawnRegionService, UpgradeService upgradeService, ToolMetaService toolMetaService) {
		this.globalBase = configManager.getConfig().getDouble("tools.globalBase", 8000.0);
		this.moneyService = moneyService;
		this.spawnRegionService = spawnRegionService;
		this.upgradeService = upgradeService;
		this.toolMetaService = toolMetaService;
		loadDefaults();
	}

	private void loadDefaults() {
		typeWeights.put(ToolType.PICKAXE, 1.00);
		typeWeights.put(ToolType.AXE, 0.90);
		typeWeights.put(ToolType.SHOVEL, 0.75);
		typeWeights.put(ToolType.SWORD, 1.10);
		tierMultipliers.put(ToolTier.WOOD, 0.15);
		tierMultipliers.put(ToolTier.STONE, 0.45);
		tierMultipliers.put(ToolTier.COPPER, 0.70);
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

	public long getBuyPrice(ToolSpec spec) {
		if (spec == null) {
			return 0L;
		}
		return getBuyPrice(spec.getType(), spec.getTier(), spec.getVariant());
	}

	public boolean purchaseTool(Player player, ToolType type, ToolTier tier, ToolVariant variant) {
		return purchaseTool(player, new ToolSpec(type, tier, variant));
	}

	public boolean purchaseTool(Player player, ToolSpec spec) {
		if (!spawnRegionService.isInSpawn(player.getLocation())) {
			return false;
		}
		if (spec == null || !isTierUnlocked(player, spec.getTier())) {
			return false;
		}
		long price = getBuyPrice(spec);
		if (!moneyService.removeBanked(player, price)) {
			return false;
		}
		ItemStack item = buildTool(spec);
		player.getInventory().addItem(item);
		return true;
	}

	public boolean isTierUnlocked(Player player, ToolTier tier) {
		if (tier == ToolTier.WOOD || tier == ToolTier.STONE || tier == ToolTier.COPPER) {
			return true;
		}
		int tierLevel = getTierLevel(tier);
		return upgradeService.hasVendorTierUnlocked(player.getUniqueId(), tierLevel);
	}

	private void applyVariant(ItemStack item, ToolTier tier, ToolVariant variant) {
		if (variant == ToolVariant.EFFICIENCY) {
			item.addEnchantment(Enchantment.EFFICIENCY, getEfficiencyLevel(tier));
		} else if (variant == ToolVariant.SILK_TOUCH) {
			item.addEnchantment(Enchantment.SILK_TOUCH, 1);
		}
	}

	public ItemStack buildTool(ToolSpec spec) {
		if (spec == null) {
			return null;
		}
		ItemStack item = new ItemStack(spec.getType().getMaterialForTier(spec.getTier()));
		applyVariant(item, spec.getTier(), spec.getVariant());
		toolMetaService.setRepairCount(item, 0);
		return item;
	}

	private int getEfficiencyLevel(ToolTier tier) {
		return switch (tier) {
			case WOOD -> 2;
			case STONE -> 2;
			case COPPER -> 2;
			case IRON -> 3;
			case DIAMOND -> 4;
			case NETHERITE -> 5;
		};
	}

	private int getTierLevel(ToolTier tier) {
		return switch (tier) {
			case IRON -> 1;
			case DIAMOND -> 2;
			case NETHERITE -> 3;
			default -> 0;
		};
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
					case COPPER -> Material.COPPER_PICKAXE;
					case IRON -> Material.IRON_PICKAXE;
					case DIAMOND -> Material.DIAMOND_PICKAXE;
					case NETHERITE -> Material.NETHERITE_PICKAXE;
				};
				case AXE -> switch (tier) {
					case WOOD -> Material.WOODEN_AXE;
					case STONE -> Material.STONE_AXE;
					case COPPER -> Material.COPPER_AXE;
					case IRON -> Material.IRON_AXE;
					case DIAMOND -> Material.DIAMOND_AXE;
					case NETHERITE -> Material.NETHERITE_AXE;
				};
				case SHOVEL -> switch (tier) {
					case WOOD -> Material.WOODEN_SHOVEL;
					case STONE -> Material.STONE_SHOVEL;
					case COPPER -> Material.COPPER_SHOVEL;
					case IRON -> Material.IRON_SHOVEL;
					case DIAMOND -> Material.DIAMOND_SHOVEL;
					case NETHERITE -> Material.NETHERITE_SHOVEL;
				};
				case SWORD -> switch (tier) {
					case WOOD -> Material.WOODEN_SWORD;
					case STONE -> Material.STONE_SWORD;
					case COPPER -> Material.COPPER_SWORD;
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
		COPPER,
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
