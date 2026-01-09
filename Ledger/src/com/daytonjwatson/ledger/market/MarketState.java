package com.daytonjwatson.ledger.market;

import java.util.HashMap;
import java.util.Map;

public class MarketState {
	private final Map<String, ItemState> items = new HashMap<>();
	private final Map<String, MobState> mobs = new HashMap<>();

	public Map<String, ItemState> getItems() {
		return items;
	}

	public Map<String, MobState> getMobs() {
		return mobs;
	}

	public ItemState getOrCreateItem(String key) {
		String normalized = com.daytonjwatson.ledger.util.ItemKeyUtil.normalizeKey(key);
		if (normalized == null) {
			normalized = key == null ? "" : key.toUpperCase();
		}
		return items.computeIfAbsent(normalized, ignored -> new ItemState());
	}

	public MobState getOrCreateMob(String key) {
		return mobs.computeIfAbsent(key.toUpperCase(), ignored -> new MobState());
	}

	public static class ItemState {
		private double soldAccumulator;
		private long lastUpdate;
		private double minedTotal;

		public double getSoldAccumulator() {
			return soldAccumulator;
		}

		public void setSoldAccumulator(double soldAccumulator) {
			this.soldAccumulator = soldAccumulator;
		}

		public long getLastUpdate() {
			return lastUpdate;
		}

		public void setLastUpdate(long lastUpdate) {
			this.lastUpdate = lastUpdate;
		}

		public double getMinedTotal() {
			return minedTotal;
		}

		public void setMinedTotal(double minedTotal) {
			this.minedTotal = minedTotal;
		}
	}

	public static class MobState {
		private double killAccumulator;
		private long lastUpdate;

		public double getKillAccumulator() {
			return killAccumulator;
		}

		public void setKillAccumulator(double killAccumulator) {
			this.killAccumulator = killAccumulator;
		}

		public long getLastUpdate() {
			return lastUpdate;
		}

		public void setLastUpdate(long lastUpdate) {
			this.lastUpdate = lastUpdate;
		}
	}
}
