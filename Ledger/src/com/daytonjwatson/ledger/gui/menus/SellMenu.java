package com.daytonjwatson.ledger.gui.menus;

import com.daytonjwatson.ledger.gui.GuiManager;
import com.daytonjwatson.ledger.gui.LedgerHolder;
import com.daytonjwatson.ledger.gui.LedgerMenu;
import com.daytonjwatson.ledger.gui.MenuId;
import com.daytonjwatson.ledger.market.MarketService;
import com.daytonjwatson.ledger.market.SellValidator;
import com.daytonjwatson.ledger.spawn.SellService;
import com.daytonjwatson.ledger.spawn.SellService.SellOutcome;
import com.daytonjwatson.ledger.spawn.SellService.SellStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class SellMenu implements LedgerMenu {
	private static final int MENU_SIZE = 54;
	private static final int SUMMARY_SLOT = 4;
	private static final int SELL_BUTTON_SLOT = 49;
	private static final int BACK_BUTTON_SLOT = 53;
	private static final Set<Integer> SELL_SLOTS = buildSellSlots();
	private final GuiManager guiManager;
	private final MarketService marketService;
	private final SellService sellService;
	private final SellValidator sellValidator;
	private final ItemStack fillerItem;

	public SellMenu(GuiManager guiManager, MarketService marketService, SellService sellService, SellValidator sellValidator) {
		this.guiManager = guiManager;
		this.marketService = marketService;
		this.sellService = sellService;
		this.sellValidator = sellValidator;
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
		for (int slot = 45; slot < MENU_SIZE; slot++) {
			inventory.setItem(slot, fillerItem);
		}
		inventory.setItem(SUMMARY_SLOT, createSummaryItem(player, inventory));
		inventory.setItem(SELL_BUTTON_SLOT, createButton(Material.EMERALD, ChatColor.GREEN + "Sell Items", ChatColor.GRAY + "Sell items placed above"));
		inventory.setItem(BACK_BUTTON_SLOT, createButton(Material.BARRIER, ChatColor.RED + "Back", ChatColor.GRAY + "Return to the hub"));
		return inventory;
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clicked, ClickType type, InventoryClickEvent event) {
		Inventory topInventory = event.getView().getTopInventory();
		if (slot == SELL_BUTTON_SLOT) {
			event.setCancelled(true);
			handleSellItems(player, topInventory);
			return;
		}
		if (slot == BACK_BUTTON_SLOT) {
			event.setCancelled(true);
			guiManager.open(MenuId.HUB, player);
			return;
		}
		if (slot < MENU_SIZE && !SELL_SLOTS.contains(slot)) {
			event.setCancelled(true);
			return;
		}
		if (slot < MENU_SIZE || event.isShiftClick()) {
			updateSummaryLater(player, topInventory);
		}
	}

	@Override
	public boolean cancelAllClicks() {
		return false;
	}

	@Override
	public void onClose(Player player, Inventory inventory) {
		returnUnsoldItems(player, inventory);
	}

	private void handleSellItems(Player player, Inventory inventory) {
		SellOutcome outcome = sellService.sellInventory(player, inventory, SELL_SLOTS);
		if (outcome.getStatus() == SellStatus.NO_SELLABLE) {
			player.sendMessage(ChatColor.RED + "No sellable items in the sell slots.");
			return;
		}
		player.sendMessage(ChatColor.GREEN + "Sold " + outcome.getSoldCount() + " items for $" + outcome.getTotal() + ".");
		inventory.setItem(SUMMARY_SLOT, createSummaryItem(player, inventory));
	}

	private ItemStack createSummaryItem(Player player, Inventory inventory) {
		ItemStack item = new ItemStack(Material.PAPER);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.YELLOW + "Sell Summary");
		List<String> lore = new ArrayList<>();
		InventorySummary summary = summarizeInventory(player, inventory);
		lore.add(ChatColor.GRAY + "Sell Slot Value: " + ChatColor.GOLD + "$" + formatMoney(summary.inventoryValue()));
		lore.add(ChatColor.GRAY + "Upgrade Bonus: " + formatMarketDelta(summary.upgradeBonus()));
		lore.add(ChatColor.GRAY + "Market Change: " + formatMarketDelta(summary.marketDelta()));
		lore.add(ChatColor.GRAY + "Sellable Types: " + ChatColor.YELLOW + summary.distinctSellableTypes());
		if (!summary.unsellableReasons().isEmpty()) {
			for (Map.Entry<String, Integer> entry : summary.unsellableReasons().entrySet()) {
				lore.add(ChatColor.RED + "Unsellable (" + entry.getKey() + "): " + entry.getValue());
			}
		}
		lore.add(ChatColor.DARK_GRAY + "Place items below to sell.");
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private InventorySummary summarizeInventory(Player player, Inventory inventory) {
		long total = 0L;
		Set<Material> distinctTypes = new HashSet<>();
		Map<String, Integer> unsellableReasons = new LinkedHashMap<>();
		List<ItemStack> sellableItems = new ArrayList<>();
		Map<Material, Integer> sellableQuantities = new LinkedHashMap<>();
		for (int slot : SELL_SLOTS) {
			ItemStack item = inventory.getItem(slot);
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			SellValidator.SellResult result = sellValidator.validate(item);
			if (!result.sellable()) {
				unsellableReasons.merge(result.reason(), item.getAmount(), Integer::sum);
				continue;
			}
			double price = marketService.getSellPrice(item);
			if (price <= 0.0) {
				continue;
			}
			distinctTypes.add(item.getType());
			total += Math.round(price * item.getAmount());
			sellableItems.add(item);
			sellableQuantities.merge(item.getType(), item.getAmount(), Integer::sum);
		}
		int distinctCount = distinctTypes.size();
		long upgradeTotal = 0L;
		for (ItemStack item : sellableItems) {
			double price = marketService.getSellPrice(player, item, distinctCount);
			if (price <= 0.0) {
				continue;
			}
			upgradeTotal += Math.round(price * item.getAmount());
		}
		long afterMarketValue = marketService.getProjectedSellValueAfterMarketChange(sellableItems, sellableQuantities);
		long marketDelta = afterMarketValue - total;
		long upgradeBonus = upgradeTotal - total;
		return new InventorySummary(total, marketDelta, distinctCount, upgradeBonus, unsellableReasons);
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

	private String formatMarketDelta(long value) {
		String formatted = formatMoney(Math.abs(value));
		String prefix = value >= 0 ? ChatColor.GREEN + "+$" : ChatColor.RED + "-$";
		return prefix + formatted;
	}

	private record InventorySummary(long inventoryValue, long marketDelta, int distinctSellableTypes, long upgradeBonus,
									Map<String, Integer> unsellableReasons) {
	}

	private static Set<Integer> buildSellSlots() {
		Set<Integer> slots = new HashSet<>();
		for (int slot = 9; slot < 45; slot++) {
			slots.add(slot);
		}
		return slots;
	}

	private void updateSummaryLater(Player player, Inventory inventory) {
		Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(SellMenu.class), () -> {
			inventory.setItem(SUMMARY_SLOT, createSummaryItem(player, inventory));
		});
	}

	private void returnUnsoldItems(Player player, Inventory inventory) {
		for (int slot : SELL_SLOTS) {
			ItemStack item = inventory.getItem(slot);
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			inventory.setItem(slot, null);
			player.getInventory().addItem(item).values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
		}
	}
}
