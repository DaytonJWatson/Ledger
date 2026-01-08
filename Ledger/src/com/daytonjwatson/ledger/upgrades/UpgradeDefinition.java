package com.daytonjwatson.ledger.upgrades;

import java.util.Collections;
import java.util.List;

public class UpgradeDefinition {
	public enum Type {
		LEVEL,
		UNLOCK,
		CHOICE
	}

	private final String id;
	private final String name;
	private final String description;
	private final Type type;
	private final int maxLevel;
	private final double costBase;
	private final double costGrowth;
	private final long fixedCost;
	private final String specializationChoice;
	private final String specializationRequirement;
	private final int unlocksVendorTier;
	private final int refinementLevel;
	private final List<String> prerequisites;

	public UpgradeDefinition(String id, String name, String description, Type type, int maxLevel, double costBase, double costGrowth,
							 long fixedCost, String specializationChoice, String specializationRequirement, int unlocksVendorTier,
							 int refinementLevel, List<String> prerequisites) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.type = type;
		this.maxLevel = maxLevel;
		this.costBase = costBase;
		this.costGrowth = costGrowth;
		this.fixedCost = fixedCost;
		this.specializationChoice = specializationChoice;
		this.specializationRequirement = specializationRequirement;
		this.unlocksVendorTier = unlocksVendorTier;
		this.refinementLevel = refinementLevel;
		this.prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Type getType() {
		return type;
	}

	public int getMaxLevel() {
		return maxLevel;
	}

	public double getCostBase() {
		return costBase;
	}

	public double getCostGrowth() {
		return costGrowth;
	}

	public long getFixedCost() {
		return fixedCost;
	}

	public String getSpecializationChoice() {
		return specializationChoice;
	}

	public String getSpecializationRequirement() {
		return specializationRequirement;
	}

	public int getUnlocksVendorTier() {
		return unlocksVendorTier;
	}

	public int getRefinementLevel() {
		return refinementLevel;
	}

	public List<String> getPrerequisites() {
		return Collections.unmodifiableList(prerequisites);
	}

	public long getNextCost(int currentLevel) {
		if (type == Type.LEVEL) {
			double cost = costBase * Math.pow(costGrowth, Math.max(0, currentLevel));
			return Math.max(0L, Math.round(cost));
		}
		return Math.max(0L, fixedCost);
	}
}
