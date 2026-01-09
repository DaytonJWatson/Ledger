package com.daytonjwatson.ledger.spawn;

import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.market.MarketService;
import com.daytonjwatson.ledger.market.SellValidator;
import com.daytonjwatson.ledger.util.ItemKeyUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SellService {
	private final MarketService marketService;
	private final MoneyService moneyService;
	private final SellValidator sellValidator;

	public SellService(MarketService marketService, MoneyService moneyService, SellValidator sellValidator) {
		this.marketService = marketService;
		this.moneyService = moneyService;
		this.sellValidator = sellValidator;
	}

	public SellOutcome sellInventory(Player player) {
		ItemStack[] items = player.getInventory().getContents();
		Map<Material, Integer> sellableCounts = new HashMap<>();
		for (ItemStack item : items) {
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			if (!sellValidator.validate(item).sellable()) {
				continue;
			}
			if (marketService.getSellPrice(item) <= 0.0) {
				continue;
			}
			sellableCounts.merge(item.getType(), item.getAmount(), Integer::sum);
		}
		int distinctTypes = sellableCounts.size();
		long total = 0;
		int soldCount = 0;
		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			if (!sellValidator.validate(item).sellable()) {
				continue;
			}
			double value = marketService.getSellPrice(player, item, distinctTypes);
			if (value <= 0.0) {
				continue;
			}
			total += Math.round(value * item.getAmount());
			soldCount += item.getAmount();
			marketService.applySale(ItemKeyUtil.toKey(item.getType()), item.getAmount());
			items[i] = null;
		}
		player.getInventory().setContents(items);
		if (total <= 0) {
			return SellOutcome.noSellable();
		}
		moneyService.addCarried(player, total);
		return SellOutcome.sold(total, soldCount);
	}

	public SellOutcome sellInventory(Player player, Inventory inventory, Set<Integer> slots) {
		Map<Material, Integer> sellableCounts = new HashMap<>();
		for (int slot : slots) {
			ItemStack item = inventory.getItem(slot);
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			if (!sellValidator.validate(item).sellable()) {
				continue;
			}
			if (marketService.getSellPrice(item) <= 0.0) {
				continue;
			}
			sellableCounts.merge(item.getType(), item.getAmount(), Integer::sum);
		}
		int distinctTypes = sellableCounts.size();
		long total = 0;
		int soldCount = 0;
		for (int slot : slots) {
			ItemStack item = inventory.getItem(slot);
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			if (!sellValidator.validate(item).sellable()) {
				continue;
			}
			double value = marketService.getSellPrice(player, item, distinctTypes);
			if (value <= 0.0) {
				continue;
			}
			total += Math.round(value * item.getAmount());
			soldCount += item.getAmount();
			marketService.applySale(ItemKeyUtil.toKey(item.getType()), item.getAmount());
			inventory.setItem(slot, null);
		}
		if (total <= 0) {
			return SellOutcome.noSellable();
		}
		moneyService.addCarried(player, total);
		return SellOutcome.sold(total, soldCount);
	}

	public SellOutcome sellHand(Player player) {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item == null || item.getType() == Material.AIR) {
			return SellOutcome.noItem();
		}
		if (!sellValidator.validate(item).sellable()) {
			return SellOutcome.noSellable();
		}
		double value = marketService.getSellPrice(player, item);
		if (value <= 0.0) {
			return SellOutcome.noSellable();
		}
		long total = Math.round(value * item.getAmount());
		marketService.applySale(ItemKeyUtil.toKey(item.getType()), item.getAmount());
		player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
		moneyService.addCarried(player, total);
		return SellOutcome.sold(total, item.getAmount());
	}

	public enum SellStatus {
		SOLD,
		NO_ITEM,
		NO_SELLABLE
	}

	public static final class SellOutcome {
		private final SellStatus status;
		private final long total;
		private final int soldCount;

		private SellOutcome(SellStatus status, long total, int soldCount) {
			this.status = status;
			this.total = total;
			this.soldCount = soldCount;
		}

		public static SellOutcome sold(long total, int soldCount) {
			return new SellOutcome(SellStatus.SOLD, total, soldCount);
		}

		public static SellOutcome noItem() {
			return new SellOutcome(SellStatus.NO_ITEM, 0L, 0);
		}

		public static SellOutcome noSellable() {
			return new SellOutcome(SellStatus.NO_SELLABLE, 0L, 0);
		}

		public SellStatus getStatus() {
			return status;
		}

		public long getTotal() {
			return total;
		}

		public int getSoldCount() {
			return soldCount;
		}
	}
}
