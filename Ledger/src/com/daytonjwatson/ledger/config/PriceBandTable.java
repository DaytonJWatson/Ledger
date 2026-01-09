package com.daytonjwatson.ledger.config;

import com.daytonjwatson.ledger.market.PriceBandTag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class PriceBandTable {
	private final Map<PriceBandTag, PriceBand> bands = new EnumMap<>(PriceBandTag.class);

	public PriceBandTable(YamlConfiguration yaml) {
		ConfigurationSection bandSection = yaml.getConfigurationSection("bands");
		if (bandSection == null) {
			return;
		}
		for (String key : bandSection.getKeys(false)) {
			PriceBandTag tag;
			try {
				tag = PriceBandTag.valueOf(key.toUpperCase());
			} catch (IllegalArgumentException e) {
				continue;
			}
			ConfigurationSection entry = bandSection.getConfigurationSection(key);
			if (entry == null) {
				continue;
			}
			double minPrice = entry.getDouble("minPrice", 0.0);
			double maxPrice = entry.getDouble("maxPrice", 0.0);
			double cap = entry.getDouble("cap", 0.0);
			double sigma = entry.getDouble("sigma", 1.0);
			double baseline = entry.getDouble("baseline", 0.0);
			bands.put(tag, new PriceBand(minPrice, maxPrice, cap, sigma, baseline));
		}
	}

	public Map<PriceBandTag, PriceBand> getBands() {
		return Collections.unmodifiableMap(bands);
	}

	public PriceBand getBand(PriceBandTag tag) {
		return bands.get(tag);
	}

	public record PriceBand(double minPrice, double maxPrice, double cap, double sigma, double baseline) {
	}
}
