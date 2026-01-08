package com.daytonjwatson.ledger.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PriceTable {
	private final Map<String, PriceEntry> entries = new HashMap<>();

	public PriceTable(YamlConfiguration yaml) {
		loadSection(yaml.getConfigurationSection("prices"));
		loadSection(yaml.getConfigurationSection("mobPrices"));
	}

	private void loadSection(ConfigurationSection section) {
		if (section == null) {
			return;
		}
		for (String key : section.getKeys(false)) {
			ConfigurationSection entrySection = section.getConfigurationSection(key);
			if (entrySection == null) {
				continue;
			}
			double base = entrySection.getDouble("base", 0.0);
			double cap = entrySection.getDouble("cap", 10000.0);
			double minFactor = entrySection.getDouble("minFactor", 0.2);
			double maxFactor = entrySection.getDouble("maxFactor", 2.5);
			double sigma = entrySection.getDouble("sigma", 1.0);
			entries.put(key.toUpperCase(), new PriceEntry(base, cap, minFactor, maxFactor, sigma));
		}
	}

	public Map<String, PriceEntry> getEntries() {
		return Collections.unmodifiableMap(entries);
	}

	public PriceEntry getEntry(String key) {
		if (key == null) {
			return null;
		}
		return entries.get(key.toUpperCase());
	}

	public static class PriceEntry {
		private final double base;
		private final double cap;
		private final double minFactor;
		private final double maxFactor;
		private final double sigma;

		public PriceEntry(double base, double cap, double minFactor, double maxFactor, double sigma) {
			this.base = base;
			this.cap = cap;
			this.minFactor = minFactor;
			this.maxFactor = maxFactor;
			this.sigma = sigma;
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
	}
}
