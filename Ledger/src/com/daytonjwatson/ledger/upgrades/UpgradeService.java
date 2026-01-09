package com.daytonjwatson.ledger.upgrades;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import org.bukkit.Bukkit;
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
		moneyService.clampUpgradeLevels(definitions);
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

	public boolean hasVendorTierUnlocked(UUID uuid, int tier) {
		if (tier <= 0) {
			return true;
		}
		for (UpgradeDefinition definition : definitions.values()) {
			if (definition.getUnlocksVendorTier() != tier) {
				continue;
			}
			if (getLevel(uuid, definition.getId()) >= 1) {
				return true;
			}
		}
		return false;
	}

	public boolean meetsPrerequisites(UUID uuid, String upgradeId) {
		UpgradeDefinition definition = getDefinition(upgradeId);
		if (definition == null) {
			return false;
		}
		return meetsPrerequisites(uuid, definition);
	}

	private boolean meetsPrerequisites(UUID uuid, UpgradeDefinition definition) {
		if (definition == null) {
			return false;
		}
		String specializationRequirement = definition.getSpecializationRequirement();
		if (specializationRequirement != null && !specializationRequirement.isBlank()) {
			String choice = getSpecializationChoice(uuid);
			if (choice == null || !choice.equalsIgnoreCase(specializationRequirement)) {
				return false;
			}
		}
		for (String prerequisite : definition.getPrerequisites()) {
			if (!hasUpgrade(uuid, prerequisite)) {
				return false;
			}
		}
		return true;
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
		return PurchaseResult.success("Purchased " + definition.getName() + " for $" + cost + ". " + getNextEffect(definition, uuid, newLevel));
	}

	public String getNextEffect(UpgradeDefinition definition, UUID uuid, int level) {
		if (definition == null) {
			return "";
		}
		return switch (definition.getId().toLowerCase(Locale.ROOT)) {
			case "barter" -> "Sell multiplier now x" + formatDecimal(getBarterMultiplier(level));
			case "insurance" -> "Death loss reduction now " + Math.round(level * 2) + "%.";
			case "logistics" -> "Sell inventory bonus now +" + Math.round(getLogisticsMaxBonusPercent(level)) + "% at full diversity.";
			case "spec_miner", "spec_farmer", "spec_hunter" -> "Specialization bonus now +" + Math.round(level * 3) + "% (penalty " + Math.round(level * 1.5) + "%).";
			default -> {
				if (definition.getType() == UpgradeDefinition.Type.CHOICE) {
					yield "Specialization set to " + definition.getSpecializationChoice().toUpperCase(Locale.ROOT) + ".";
				}
				if (definition.getUnlocksVendorTier() > 0) {
					yield "Vendor tier " + definition.getUnlocksVendorTier() + " unlocked.";
				}
				if (definition.getRefinementLevel() > 0) {
					yield "Refinement level " + definition.getRefinementLevel() + " unlocked.";
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
		if (level <= 0 || distinctTypes <= 0) {
			return 1.0;
		}
		int clampedLevel = Math.max(1, Math.min(5, level));
		double maxBonus = switch (clampedLevel) {
			case 1 -> 0.12;
			case 2 -> 0.14;
			case 3 -> 0.16;
			case 4 -> 0.18;
			default -> 0.20;
		};
		int targetDistinct = switch (clampedLevel) {
			case 1 -> 12;
			case 2 -> 11;
			case 3 -> 10;
			case 4 -> 9;
			default -> 8;
		};
		double ratio = Math.min((double) distinctTypes / targetDistinct, 1.0);
		return 1.0 + (maxBonus * ratio);
	}

	public int getHighestRefinementLevel(UUID uuid) {
		int highest = 0;
		for (UpgradeDefinition definition : definitions.values()) {
			if (definition.getRefinementLevel() <= 0) {
				continue;
			}
			if (getLevel(uuid, definition.getId()) <= 0) {
				continue;
			}
			highest = Math.max(highest, definition.getRefinementLevel());
		}
		return highest;
	}

	public void reloadDefinitions() {
		definitions.clear();
		loadDefinitions();
		moneyService.clampUpgradeLevels(definitions);
	}

	private void loadDefinitions() {
		ConfigurationSection root = configManager.getUpgrades().getConfigurationSection("upgrades");
		if (root == null) {
			return;
		}
		List<String> warnings = new ArrayList<>();
		for (String key : root.getKeys(false)) {
			ConfigurationSection section = root.getConfigurationSection(key);
			if (section == null) {
				continue;
			}
			String id = key.toLowerCase(Locale.ROOT);
			if (definitions.containsKey(id)) {
				warnings.add("Duplicate upgrade id: " + id);
				continue;
			}
			String name = section.getString("name", key);
			String description = section.getString("description", "");
			String rawType = section.getString("type", "LEVEL");
			UpgradeDefinition.Type type = parseType(rawType, warnings, id);
			if (type == null) {
				continue;
			}
			int maxLevel = type == UpgradeDefinition.Type.LEVEL ? section.getInt("maxLevel", 1) : 1;
			if (type == UpgradeDefinition.Type.LEVEL && !section.isInt("maxLevel")) {
				warnings.add("Upgrade " + id + " missing maxLevel.");
			}
			if (type != UpgradeDefinition.Type.LEVEL && section.isInt("maxLevel")) {
				warnings.add("Upgrade " + id + " should not define maxLevel.");
			}
			ConfigurationSection costSection = section.getConfigurationSection("cost");
			double costBase = costSection != null ? costSection.getDouble("c0", 0.0) : section.getDouble("cost", 0.0);
			double costGrowth = costSection != null ? costSection.getDouble("g", 1.0) : section.getDouble("costGrowth", 1.0);
			long fixedCost = type == UpgradeDefinition.Type.CHOICE ? section.getLong("cost", 0L) : 0L;
			String specializationChoice = section.getString("specializationChoice", null);
			String specializationRequirement = section.getString("specializationRequirement", null);
			if (type != UpgradeDefinition.Type.CHOICE && specializationChoice != null) {
				warnings.add("Upgrade " + id + " should not define specializationChoice.");
				specializationChoice = null;
			}
			if (type == UpgradeDefinition.Type.CHOICE && (specializationChoice == null || specializationChoice.isBlank())) {
				warnings.add("Upgrade " + id + " missing specializationChoice.");
			}
			if (type != UpgradeDefinition.Type.LEVEL && specializationRequirement != null) {
				warnings.add("Upgrade " + id + " should not define specializationRequirement.");
				specializationRequirement = null;
			}
			int unlocksVendorTier = section.getInt("unlocksVendorTier", 0);
			int refinementLevel = section.getInt("refinementLevel", 0);
			if (unlocksVendorTier > 0 && type != UpgradeDefinition.Type.LEVEL) {
				warnings.add("Upgrade " + id + " should use LEVEL type for vendor unlocks.");
			}
			if (refinementLevel > 0 && type != UpgradeDefinition.Type.LEVEL) {
				warnings.add("Upgrade " + id + " should use LEVEL type for refinement.");
			}
			if (unlocksVendorTier > 0 && refinementLevel > 0) {
				warnings.add("Upgrade " + id + " cannot be both vendor unlock and refinement.");
			}
			List<String> prerequisites = section.getStringList("requires");
			definitions.put(id, new UpgradeDefinition(id, name, description, type, maxLevel, costBase, costGrowth,
				fixedCost, specializationChoice, specializationRequirement, unlocksVendorTier, refinementLevel, prerequisites));
		}
		warnings.addAll(validatePrerequisites());
		warnings.addAll(validateCycles());
		for (String warning : warnings) {
			Bukkit.getLogger().warning("[Ledger] " + warning);
		}
	}

	private UpgradeDefinition.Type parseType(String raw, List<String> warnings, String id) {
		if (raw == null) {
			return UpgradeDefinition.Type.LEVEL;
		}
		try {
			UpgradeDefinition.Type type = UpgradeDefinition.Type.valueOf(raw.toUpperCase(Locale.ROOT));
			if (type != UpgradeDefinition.Type.LEVEL && type != UpgradeDefinition.Type.CHOICE) {
				warnings.add("Upgrade " + id + " has unsupported type: " + raw);
				return null;
			}
			return type;
		} catch (IllegalArgumentException ex) {
			warnings.add("Upgrade " + id + " has unsupported type: " + raw);
			return null;
		}
	}

	private List<String> validatePrerequisites() {
		List<String> warnings = new ArrayList<>();
		for (UpgradeDefinition definition : definitions.values()) {
			for (String prerequisite : definition.getPrerequisites()) {
				if (!definitions.containsKey(prerequisite.toLowerCase(Locale.ROOT))) {
					warnings.add("Upgrade " + definition.getId() + " has unknown prerequisite: " + prerequisite);
				}
			}
		}
		return warnings;
	}

	private List<String> validateCycles() {
		List<String> warnings = new ArrayList<>();
		Map<String, VisitState> visitStates = new HashMap<>();
		for (String id : definitions.keySet()) {
			if (visitStates.getOrDefault(id, VisitState.UNVISITED) == VisitState.UNVISITED) {
				dfsVisit(id, new ArrayList<>(), visitStates, warnings);
			}
		}
		return warnings;
	}

	private void dfsVisit(String id, List<String> stack, Map<String, VisitState> visitStates, List<String> warnings) {
		visitStates.put(id, VisitState.VISITING);
		stack.add(id);
		UpgradeDefinition definition = definitions.get(id);
		if (definition != null) {
			for (String prerequisite : definition.getPrerequisites()) {
				String normalized = prerequisite.toLowerCase(Locale.ROOT);
				VisitState state = visitStates.getOrDefault(normalized, VisitState.UNVISITED);
				if (state == VisitState.VISITING) {
					warnings.add("Upgrade prerequisite cycle detected: " + String.join(" -> ", stack) + " -> " + normalized);
				} else if (state == VisitState.UNVISITED) {
					dfsVisit(normalized, stack, visitStates, warnings);
				}
			}
		}
		stack.remove(stack.size() - 1);
		visitStates.put(id, VisitState.VISITED);
	}

	private enum VisitState {
		UNVISITED,
		VISITING,
		VISITED
	}

	private double getLogisticsMaxBonusPercent(int level) {
		int clampedLevel = Math.max(1, Math.min(5, level));
		return switch (clampedLevel) {
			case 1 -> 12.0;
			case 2 -> 14.0;
			case 3 -> 16.0;
			case 4 -> 18.0;
			default -> 20.0;
		};
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
