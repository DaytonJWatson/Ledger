package com.daytonjwatson.ledger.items;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.market.MarketService;
import com.daytonjwatson.ledger.market.SellValidator;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class LoreValueService implements Listener {
	private final JavaPlugin plugin;
	private final MarketService marketService;
	private final SellValidator sellValidator;
	private final NamespacedKey priceKey;

	public LoreValueService(JavaPlugin plugin, ConfigManager configManager, MarketService marketService, SellValidator sellValidator) {
		this.plugin = plugin;
		this.marketService = marketService;
		this.sellValidator = sellValidator;
		this.priceKey = new NamespacedKey(plugin, "lastDisplayedPrice");
	}

	public void updatePlayerInventory(Player player) {
		if (player == null) {
			return;
		}
		ItemStack[] contents = player.getInventory().getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			if (item.getItemMeta() instanceof BlockStateMeta) {
				continue;
			}
			updateItemLore(player, item);
		}
	}

	public void updateItemLore(Player player, ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}
		if (!sellValidator.validate(item).sellable()) {
			clearPriceMeta(item);
			return;
		}
		double unitPrice = marketService.getSellPrice(player, item);
		if (unitPrice <= 0.0) {
			return;
		}
		double price = unitPrice * item.getAmount();
		Double lastPrice = meta.getPersistentDataContainer().get(priceKey, PersistentDataType.DOUBLE);
		if (lastPrice != null && Math.abs(lastPrice - price) < 0.01) {
			return;
		}
		List<String> lore = meta.getLore();
		if (lore == null) {
			lore = new ArrayList<>();
		} else {
			lore = new ArrayList<>(lore);
		}
		lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Value:"));
		lore.add(ChatColor.GOLD + "Value: $" + String.format("%.2f", price));
		meta.setLore(lore);
		meta.getPersistentDataContainer().set(priceKey, PersistentDataType.DOUBLE, price);
		item.setItemMeta(meta);
	}

	private void clearPriceMeta(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}
		boolean changed = false;
		List<String> lore = meta.getLore();
		if (lore != null) {
			List<String> updated = new ArrayList<>(lore);
			boolean removed = updated.removeIf(line -> ChatColor.stripColor(line).startsWith("Value:"));
			if (removed) {
				meta.setLore(updated.isEmpty() ? null : updated);
				changed = true;
			}
		}
		if (meta.getPersistentDataContainer().has(priceKey, PersistentDataType.DOUBLE)) {
			meta.getPersistentDataContainer().remove(priceKey);
			changed = true;
		}
		if (changed) {
			item.setItemMeta(meta);
		}
	}

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (event.getPlayer() instanceof Player player) {
			updatePlayerInventory(player);
		}
	}

	@EventHandler
	public void onItemHeld(PlayerItemHeldEvent event) {
		updatePlayerInventory(event.getPlayer());
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			ItemStack current = event.getCurrentItem();
			ItemStack cursor = event.getCursor();
			clearPriceMeta(current);
			clearPriceMeta(cursor);
			ItemStack[] contents = player.getInventory().getContents();
			for (ItemStack item : contents) {
				if (item == null || item.getType() == Material.AIR) {
					continue;
				}
				if (item.getItemMeta() instanceof BlockStateMeta) {
					continue;
				}
				clearPriceMeta(item);
			}
			plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayerInventory(player));
		}
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player player) {
			ItemStack stack = event.getItem().getItemStack();
			clearPriceMeta(stack);
			event.getItem().setItemStack(stack);
			ItemStack[] contents = player.getInventory().getContents();
			for (ItemStack item : contents) {
				if (item == null || item.getType() == Material.AIR) {
					continue;
				}
				if (item.getItemMeta() instanceof BlockStateMeta) {
					continue;
				}
				clearPriceMeta(item);
			}
			plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayerInventory(player));
		}
	}
}
