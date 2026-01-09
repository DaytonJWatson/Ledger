package com.daytonjwatson.ledger.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.daytonjwatson.ledger.util.ItemKeyUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PriceTable {
	private final Map<String, PriceEntry> entries = new HashMap<>();

	public PriceTable(YamlConfiguration yaml, YamlConfiguration overrides) {
		loadSection(yaml, "prices");
		loadSection(yaml, "mobPrices");
		applyOverrides(overrides);
	}

	public PriceTable(YamlConfiguration yaml) {
		this(yaml, null);
	}

	private void loadSection(YamlConfiguration yaml, String sectionKey) {
		if (yaml == null) {
			return;
		}
		ConfigurationSection section = yaml.getConfigurationSection(sectionKey);
		if (section == null) {
			if ("prices".equals(sectionKey)) {
				loadRootEntries(yaml);
			}
			return;
		}
		for (String key : section.getKeys(false)) {
			ConfigurationSection entrySection = section.getConfigurationSection(key);
			if (entrySection == null) {
				continue;
			}
			PriceEntry entry = readEntry(entrySection, key);
			if (entry != null) {
				entries.put(entry.key(), entry);
			}
		}
	}

	private void loadRootEntries(YamlConfiguration yaml) {
		for (String key : yaml.getKeys(false)) {
			ConfigurationSection entrySection = yaml.getConfigurationSection(key);
			if (entrySection == null) {
				continue;
			}
			PriceEntry entry = readEntry(entrySection, key);
			if (entry != null) {
				entries.put(entry.key(), entry);
			}
		}
	}

	private void applyOverrides(YamlConfiguration overrides) {
		if (overrides == null) {
			return;
		}
		ConfigurationSection section = overrides.getConfigurationSection("overrides");
		if (section == null) {
			section = overrides;
		}
		for (String key : section.getKeys(false)) {
			ConfigurationSection entrySection = section.getConfigurationSection(key);
			if (entrySection == null) {
				continue;
			}
			String normalizedKey = ItemKeyUtil.normalizeKey(key);
			if (normalizedKey == null) {
				continue;
			}
			PriceEntry baseEntry = entries.getOrDefault(normalizedKey, PriceEntry.empty(normalizedKey));
			PriceEntry merged = baseEntry.applyOverride(entrySection);
			entries.put(normalizedKey, merged);
		}
	}

	private PriceEntry readEntry(ConfigurationSection entrySection, String key) {
		String normalizedKey = ItemKeyUtil.normalizeKey(key);
		if (normalizedKey == null) {
			return null;
		}
		double base = entrySection.getDouble("base", 0.0);
		double cap = entrySection.getDouble("cap", 10000.0);
		double minFactor = entrySection.getDouble("minFactor", 0.2);
		double maxFactor = entrySection.getDouble("maxFactor", 2.5);
		double sigma = entrySection.getDouble("sigma", 1.0);
		double baseline = entrySection.getDouble("baseline", 0.0);
		double rho = entrySection.getDouble("rho", 0.0);
		String tag = entrySection.getString("tag", "");
		boolean unsellable = entrySection.getBoolean("unsellable", false);
		return new PriceEntry(normalizedKey, base, cap, minFactor, maxFactor, sigma, baseline, rho, tag, unsellable);
	}

	public Map<String, PriceEntry> getEntries() {
		return Collections.unmodifiableMap(entries);
	}

	public PriceEntry getEntry(String key) {
		String normalized = ItemKeyUtil.normalizeKey(key);
		if (normalized == null) {
			return null;
		}
		return entries.get(normalized);
	}

	public static class PriceEntry {
		private final String key;
		private final double base;
		private final double cap;
		private final double minFactor;
		private final double maxFactor;
		private final double sigma;
		private final double baseline;
		private final double rho;
		private final String tag;
		private final boolean unsellable;

		public PriceEntry(String key, double base, double cap, double minFactor, double maxFactor, double sigma,
						  double baseline, double rho, String tag, boolean unsellable) {
			this.key = key;
			this.base = base;
			this.cap = cap;
			this.minFactor = minFactor;
			this.maxFactor = maxFactor;
			this.sigma = sigma;
			this.baseline = baseline;
			this.rho = rho;
			this.tag = tag;
			this.unsellable = unsellable;
		}

		public static PriceEntry empty(String key) {
			return new PriceEntry(key, 0.0, 0.0, 0.2, 2.5, 1.0, 0.0, 0.0, "", true);
		}

		public String key() {
			return key;
		}

		public double getBase() {
			return base;
		}

		public double getCap() {
			return cap;
		}

		public double getMinFactor() {
			return minFactor;
		}

		public double getMaxFactor() {
			return maxFactor;
		}

		public double getSigma() {
			return sigma;
		}

		public double getBaseline() {
			return baseline;
		}

		public double getRho() {
			return rho;
		}

		public String getTag() {
			return tag;
		}

		public boolean isUnsellable() {
			return unsellable;
		}

		private PriceEntry applyOverride(ConfigurationSection overrideSection) {
			double newBase = overrideSection.contains("base") ? overrideSection.getDouble("base", base) : base;
			double newCap = overrideSection.contains("cap") ? overrideSection.getDouble("cap", cap) : cap;
			double newMinFactor = overrideSection.contains("minFactor") ? overrideSection.getDouble("minFactor", minFactor) : minFactor;
			double newMaxFactor = overrideSection.contains("maxFactor") ? overrideSection.getDouble("maxFactor", maxFactor) : maxFactor;
			double newSigma = overrideSection.contains("sigma") ? overrideSection.getDouble("sigma", sigma) : sigma;
			double newBaseline = overrideSection.contains("baseline") ? overrideSection.getDouble("baseline", baseline) : baseline;
			double newRho = overrideSection.contains("rho") ? overrideSection.getDouble("rho", rho) : rho;
			String newTag = overrideSection.contains("tag") ? overrideSection.getString("tag", tag) : tag;
			boolean newUnsellable = overrideSection.contains("unsellable") ? overrideSection.getBoolean("unsellable", unsellable) : unsellable;
			return new PriceEntry(key, newBase, newCap, newMinFactor, newMaxFactor, newSigma, newBaseline, newRho, newTag, newUnsellable);
		}
	}
}
