package com.daytonjwatson.ledger.config;

import com.daytonjwatson.ledger.market.ItemTagService;
import com.daytonjwatson.ledger.market.PriceBandTag;
import com.daytonjwatson.ledger.util.ItemKeyUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PriceGenerator {
	private final ItemTagService itemTagService;
	private final PriceBandTable bandTable;

	public PriceGenerator(ItemTagService itemTagService, PriceBandTable bandTable) {
		this.itemTagService = itemTagService;
		this.bandTable = bandTable;
	}

	public YamlConfiguration generate() {
		YamlConfiguration yaml = new YamlConfiguration();
		ConfigurationSection pricesSection = yaml.createSection("prices");
		List<Material> materials = List.of(Material.values());
		List<String> keys = new ArrayList<>();
		for (Material material : materials) {
			String key = ItemKeyUtil.toKey(material);
			if (key != null) {
				keys.add(key);
			}
		}
		keys.sort(Comparator.naturalOrder());
		for (String key : keys) {
			Material material = Material.matchMaterial(key);
			if (material == null) {
				continue;
			}
			PriceBandTag tag = itemTagService.getBaseTag(material);
			PriceBandTable.PriceBand band = bandTable.getBand(tag);
			if (band == null) {
				band = new PriceBandTable.PriceBand(0.0, 0.0, 1.0, 1.0, 0.0);
			}
			double jitter = computeJitter(material);
			double price = band.minPrice() + jitter * (band.maxPrice() - band.minPrice());
			double base = roundTwoDecimals(price);
			ConfigurationSection entry = pricesSection.createSection(key);
			entry.set("base", base);
			entry.set("cap", band.cap());
			entry.set("sigma", band.sigma());
			entry.set("tag", tag.name());
			if (band.baseline() > 0.0) {
				entry.set("baseline", band.baseline());
			}
			if (tag == PriceBandTag.UNSELLABLE || tag == PriceBandTag.ENCHANTED) {
				entry.set("unsellable", true);
			}
		}
		return yaml;
	}

	public void write(File file, YamlConfiguration existing) throws IOException {
		YamlConfiguration generated = generate();
		if (existing != null && existing.getConfigurationSection("mobPrices") != null) {
			generated.set("mobPrices", existing.get("mobPrices"));
		}
		String content = generated.saveToString();
		com.daytonjwatson.ledger.util.AtomicFileWriter.writeAtomically(file, content.getBytes(StandardCharsets.UTF_8));
	}

	public Map<PriceBandTag, PriceSummary> summarize(YamlConfiguration yaml) {
		ConfigurationSection pricesSection = yaml.getConfigurationSection("prices");
		java.util.Map<PriceBandTag, PriceSummary> summary = new java.util.EnumMap<>(PriceBandTag.class);
		if (pricesSection == null) {
			return summary;
		}
		for (String key : pricesSection.getKeys(false)) {
			ConfigurationSection entry = pricesSection.getConfigurationSection(key);
			if (entry == null) {
				continue;
			}
			String tagValue = entry.getString("tag", PriceBandTag.TRASH_COMMON.name());
			PriceBandTag tag;
			try {
				tag = PriceBandTag.valueOf(tagValue.toUpperCase());
			} catch (IllegalArgumentException e) {
				continue;
			}
			double base = entry.getDouble("base", 0.0);
			PriceSummary current = summary.computeIfAbsent(tag, ignored -> new PriceSummary());
			current.add(base);
		}
		return summary;
	}

	private double computeJitter(Material material) {
		byte[] hash = sha1(material.name());
		int value = ((hash[0] & 0xFF) << 24)
			| ((hash[1] & 0xFF) << 16)
			| ((hash[2] & 0xFF) << 8)
			| (hash[3] & 0xFF);
		int normalized = Math.abs(value % 10000);
		return normalized / 10000.0;
	}

	private byte[] sha1(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			return digest.digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			return value.getBytes(StandardCharsets.UTF_8);
		}
	}

	private double roundTwoDecimals(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	public static class PriceSummary {
		private double min = Double.MAX_VALUE;
		private double max = 0.0;
		private double total = 0.0;
		private int count = 0;

		public void add(double value) {
			min = Math.min(min, value);
			max = Math.max(max, value);
			total += value;
			count++;
		}

		public double getMin() {
			return count == 0 ? 0.0 : min;
		}

		public double getMax() {
			return max;
		}

		public double getAverage() {
			return count == 0 ? 0.0 : total / count;
		}

		public int getCount() {
			return count;
		}
	}
}
