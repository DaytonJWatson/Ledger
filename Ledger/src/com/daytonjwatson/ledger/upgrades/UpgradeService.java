package com.daytonjwatson.ledger.upgrades;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class UpgradeService {
	private final ConfigManager configManager;
	private final MoneyService moneyService;
	private final SpawnRegionService spawnRegionService;
	private final Map<String, UpgradeDefinition> definitions = new HashMap<>();

	public UpgradeService(ConfigManager configManager, MoneyService moneyService, SpawnRegionService spawnRegionService) {
		this.configManager = configManager;
		this.moneyService = moneyService;
		this.spawnRegionService = spawnRegionService;
		loadDefinitions();
	}

	public Map<String, UpgradeDefinition> getDefinitions() {
		return Collections.unmodifiableMap(definitions);
	}

	public UpgradeDefinition getDefinition(String id) {
		if (id == null) {
			return null;
		}
		return definitions.get(id.toLowerCase(Locale.ROOT));
	}

	public int getLevel(UUID uuid, String upgradeId) {
		return moneyService.getUpgradeLevel(uuid, upgradeId);
	}

	public void setLevel(UUID uuid, String upgradeId, int level) {
		moneyService.setUpgradeLevel(uuid, upgradeId, level);
	}

	public boolean hasUpgrade(UUID uuid, String upgradeId) {
		return moneyService.hasUpgrade(uuid, upgradeId);
	}

	public String getSpecializationChoice(UUID uuid) {
		return moneyService.getSpecializationChoice(uuid);
	}

	public PurchaseResult purchaseUpgrade(Player player, String upgradeId) {
		if (player == null) {
			return PurchaseResult.failure("Players only.");
		}
		if (!spawnRegionService.isInSpawn(player.getLocation())) {
			return PurchaseResult.failure("You can only buy upgrades at spawn.");
		}
		UpgradeDefinition definition = getDefinition(upgradeId);
		if (definition == null) {
			return PurchaseResult.failure("Unknown upgrade id: " + upgradeId);
		}
		UUID uuid = player.getUniqueId();
		int currentLevel = getLevel(uuid, definition.getId());
		if (definition.getType() == UpgradeDefinition.Type.LEVEL && currentLevel >= definition.getMaxLevel()) {
			return PurchaseResult.failure("That upgrade is already maxed.");
		}
		if (definition.getType() != UpgradeDefinition.Type.LEVEL && currentLevel > 0) {
			return PurchaseResult.failure("That upgrade is already unlocked.");
		}
		String specializationRequirement = definition.getSpecializationRequirement();
		if (specializationRequirement != null && !specializationRequirement.isBlank()) {
			String choice = getSpecializationChoice(uuid);
			if (choice == null || !choice.equalsIgnoreCase(specializationRequirement)) {
				return PurchaseResult.failure("Requires specialization: " + specializationRequirement.toUpperCase(Locale.ROOT));
			}
		}
		if (definition.getType() == UpgradeDefinition.Type.CHOICE) {
			String choice = getSpecializationChoice(uuid);
			if (choice != null && !choice.isBlank()) {
				return PurchaseResult.failure("Specialization already chosen: " + choice.toUpperCase(Locale.ROOT));
			}
		}
		List<String> missing = new ArrayList<>();
		for (String prerequisite : definition.getPrerequisites()) {
			if (!hasUpgrade(uuid, prerequisite)) {
				missing.add(prerequisite);
			}
		}
		if (!missing.isEmpty()) {
			return PurchaseResult.failure("Missing prerequisites: " + String.join(", ", missing));
		}
		long cost = definition.getNextCost(currentLevel);
		long banked = moneyService.getBanked(uuid);
		if (banked < cost) {
			return PurchaseResult.failure("Cost $" + cost + ", but you only have $" + banked + " banked.");
		}
		if (!moneyService.removeBanked(player, cost)) {
			return PurchaseResult.failure("Unable to withdraw funds for purchase.");
		}
		int newLevel = definition.getType() == UpgradeDefinition.Type.LEVEL ? currentLevel + 1 : 1;
		setLevel(uuid, definition.getId(), newLevel);
		if (definition.getType() == UpgradeDefinition.Type.CHOICE) {
			moneyService.setSpecializationChoice(uuid, definition.getSpecializationChoice());
		}
		if (definition.getUnlocksVendorTier() > 0) {
			int currentTier = moneyService.getVendorTierUnlocked(uuid);
			if (definition.getUnlocksVendorTier() > currentTier) {
				moneyService.setVendorTierUnlocked(uuid, definition.getUnlocksVendorTier());
			}
		}
		return PurchaseResult.success("Purchased " + definition.getName() + " for $" + cost + ". " + getNextEffect(definition, uuid, newLevel));
	}

	public String getNextEffect(UpgradeDefinition definition, UUID uuid, int level) {
		if (definition == null) {
			return "";
		}
		return switch (definition.getId().toLowerCase(Locale.ROOT)) {
			case "barter" -> "Sell multiplier now x" + formatDecimal(getBarterMultiplier(level));
			case "insurance" -> "Death loss reduction now " + Math.round(level * 2) + "%.";
			case "logistics" -> "Diversity bonus now +" + Math.round(level * 4) + "% per extra type.";
			case "spec_miner", "spec_farmer", "spec_hunter" -> "Specialization bonus now +" + Math.round(level * 3) + "% (penalty " + Math.round(level) + "%).";
			default -> {
				if (definition.getType() == UpgradeDefinition.Type.CHOICE) {
					yield "Specialization set to " + definition.getSpecializationChoice().toUpperCase(Locale.ROOT) + ".";
				}
				if (definition.getUnlocksVendorTier() > 0) {
					yield "Vendor tier unlocked.";
				}
				if (definition.getRefinementLevel() > 0) {
					yield "Refinement level " + definition.getRefinementLevel() + " unlocked (not implemented yet).";
				}
				yield "";
			}
		};
	}

	public String formatUpgradeLine(Player player, UpgradeDefinition definition) {
		if (player == null || definition == null) {
			return "";
		}
		int level = getLevel(player.getUniqueId(), definition.getId());
		String levelText = definition.getType() == UpgradeDefinition.Type.LEVEL
			? "Level " + level + "/" + definition.getMaxLevel()
			: (level > 0 ? "Unlocked" : "Locked");
		return ChatColor.GOLD + definition.getId() + ChatColor.WHITE + " - " + definition.getName() + " (" + levelText + ")";
	}

	public double getBarterMultiplier(int level) {
		return 1.0 + 0.60 * (1.0 - Math.exp(-0.18 * level));
	}

	public double getLogisticsMultiplier(int level, int distinctTypes) {
		if (level <= 0 || distinctTypes <= 1) {
			return 1.0;
		}
		double bonus = 0.04 * level * Math.max(0, distinctTypes - 1);
		return clamp(1.0 + bonus, 1.0, 2.0);
	}

	public double getSpecializationMultiplier(int level, boolean matches) {
		if (level <= 0) {
			return 1.0;
		}
		double bonus = matches ? 0.03 * level : -0.01 * level;
		return clamp(1.0 + bonus, 0.5, 2.0);
	}

	private void loadDefinitions() {
		ConfigurationSection root = configManager.getUpgrades().getConfigurationSection("upgrades");
		if (root == null) {
			return;
		}
		for (String key : root.getKeys(false)) {
			ConfigurationSection section = root.getConfigurationSection(key);
			if (section == null) {
				continue;
			}
			String id = key.toLowerCase(Locale.ROOT);
			String name = section.getString("name", key);
			String description = section.getString("description", "");
			UpgradeDefinition.Type type = parseType(section.getString("type", "LEVEL"));
			int maxLevel = section.getInt("maxLevel", type == UpgradeDefinition.Type.LEVEL ? 1 : 1);
			ConfigurationSection costSection = section.getConfigurationSection("cost");
			double costBase = costSection != null ? costSection.getDouble("c0", 0.0) : section.getDouble("cost", 0.0);
			double costGrowth = costSection != null ? costSection.getDouble("g", 1.0) : section.getDouble("costGrowth", 1.0);
			long fixedCost = section.getLong("cost", costSection != null ? 0L : 0L);
			String specializationChoice = section.getString("specializationChoice", null);
			String specializationRequirement = section.getString("specializationRequirement", null);
			int unlocksVendorTier = section.getInt("unlocksVendorTier", 0);
			int refinementLevel = section.getInt("refinementLevel", 0);
			List<String> prerequisites = section.getStringList("requires");
			definitions.put(id, new UpgradeDefinition(id, name, description, type, maxLevel, costBase, costGrowth,
				fixedCost, specializationChoice, specializationRequirement, unlocksVendorTier, refinementLevel, prerequisites));
		}
	}

	private UpgradeDefinition.Type parseType(String raw) {
		if (raw == null) {
			return UpgradeDefinition.Type.LEVEL;
		}
		try {
			return UpgradeDefinition.Type.valueOf(raw.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return UpgradeDefinition.Type.LEVEL;
		}
	}

	private String formatDecimal(double value) {
		return String.format(Locale.US, "%.2f", value);
	}

	private double clamp(double value, double min, double max) {
		return Math.min(max, Math.max(min, value));
	}

	public static class PurchaseResult {
		private final boolean success;
		private final String message;

		public PurchaseResult(boolean success, String message) {
			this.success = success;
			this.message = message;
		}

		public boolean isSuccess() {
			return success;
		}

		public String getMessage() {
			return message;
		}

		public static PurchaseResult success(String message) {
			return new PurchaseResult(true, message);
		}

		public static PurchaseResult failure(String message) {
			return new PurchaseResult(false, message);
		}
	}
}
