package com.daytonjwatson.ledger.gui.menus;

import com.daytonjwatson.ledger.gui.GuiManager;
import com.daytonjwatson.ledger.gui.LedgerHolder;
import com.daytonjwatson.ledger.gui.LedgerMenu;
import com.daytonjwatson.ledger.gui.MenuId;
import com.daytonjwatson.ledger.market.MarketService;
import com.daytonjwatson.ledger.spawn.SellService;
import com.daytonjwatson.ledger.spawn.SellService.SellOutcome;
import com.daytonjwatson.ledger.spawn.SellService.SellStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SellMenu implements LedgerMenu {
	private static final int MENU_SIZE = 54;
	private final GuiManager guiManager;
	private final MarketService marketService;
	private final SellService sellService;
	private final ItemStack fillerItem;

	public SellMenu(GuiManager guiManager, MarketService marketService, SellService sellService) {
		this.guiManager = guiManager;
		this.marketService = marketService;
		this.sellService = sellService;
		this.fillerItem = createFillerItem();
	}

	@Override
	public MenuId id() {
		return MenuId.SELL;
	}

	@Override
	public Inventory build(Player player) {
		Inventory inventory = Bukkit.createInventory(new LedgerHolder(id()), MENU_SIZE, GuiManager.MENU_TITLE_PREFIX + "Sell");
		for (int slot = 0; slot < 9; slot++) {
			inventory.setItem(slot, fillerItem);
		}
		inventory.setItem(4, createSummaryItem(player));
		inventory.setItem(20, createButton(Material.LIME_DYE, ChatColor.GREEN + "Sell Hand", ChatColor.GRAY + "Sell your held stack"));
		inventory.setItem(24, createButton(Material.EMERALD, ChatColor.GREEN + "Sell Inventory", ChatColor.GRAY + "Sell all sellable items"));
		inventory.setItem(31, createButton(Material.BARRIER, ChatColor.RED + "Back", ChatColor.GRAY + "Return to the hub"));
		return inventory;
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clicked, ClickType type) {
		switch (slot) {
			case 20 -> handleSellHand(player);
			case 24 -> handleSellInventory(player);
			case 31 -> guiManager.open(MenuId.HUB, player);
			default -> {
			}
		}
	}

	private void handleSellHand(Player player) {
		SellOutcome outcome = sellService.sellHand(player);
		if (outcome.getStatus() == SellStatus.NO_ITEM) {
			player.sendMessage(ChatColor.RED + "Hold an item to sell.");
			return;
		}
		if (outcome.getStatus() == SellStatus.NO_SELLABLE) {
			player.sendMessage(ChatColor.RED + "No sellable items.");
			return;
		}
		player.sendMessage(ChatColor.GREEN + "Sold " + outcome.getSoldCount() + " items for $" + outcome.getTotal() + ".");
		guiManager.open(MenuId.SELL, player);
	}

	private void handleSellInventory(Player player) {
		SellOutcome outcome = sellService.sellInventory(player);
		if (outcome.getStatus() == SellStatus.NO_SELLABLE) {
			player.sendMessage(ChatColor.RED + "No sellable items.");
			return;
		}
		player.sendMessage(ChatColor.GREEN + "Sold " + outcome.getSoldCount() + " items for $" + outcome.getTotal() + ".");
		guiManager.open(MenuId.SELL, player);
	}

	private ItemStack createSummaryItem(Player player) {
		ItemStack item = new ItemStack(Material.PAPER);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.GOLD + "Sell Summary");
		List<String> lore = new ArrayList<>();
		InventorySummary summary = summarizeInventory(player);
		ItemStack hand = player.getInventory().getItemInMainHand();
		long handValue = 0L;
		if (hand != null && hand.getType() != Material.AIR) {
			handValue = Math.round(marketService.getSellPrice(hand) * hand.getAmount());
		}
		lore.add(ChatColor.GRAY + "Inventory Value: " + ChatColor.GOLD + "$" + formatMoney(summary.inventoryValue()));
		lore.add(ChatColor.GRAY + "Hand Value: " + ChatColor.GOLD + "$" + formatMoney(handValue));
		lore.add(ChatColor.GRAY + "Sellable Types: " + ChatColor.YELLOW + summary.distinctSellableTypes());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private InventorySummary summarizeInventory(Player player) {
		long total = 0L;
		Set<Material> distinctTypes = new HashSet<>();
		for (ItemStack item : player.getInventory().getContents()) {
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			double price = marketService.getSellPrice(item);
			if (price <= 0.0) {
				continue;
			}
			distinctTypes.add(item.getType());
			total += Math.round(price * item.getAmount());
		}
		return new InventorySummary(total, distinctTypes.size());
	}

	private ItemStack createFillerItem() {
		ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(" ");
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack createButton(Material material, String name, String loreText) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(name);
		List<String> lore = new ArrayList<>();
		lore.add(loreText);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private String formatMoney(long value) {
		return String.format("%,d", value);
	}

	private record InventorySummary(long inventoryValue, int distinctSellableTypes) {
	}
}
