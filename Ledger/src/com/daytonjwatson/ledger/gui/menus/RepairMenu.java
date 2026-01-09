package com.daytonjwatson.ledger.gui.menus;

import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.gui.GuiManager;
import com.daytonjwatson.ledger.gui.LedgerHolder;
import com.daytonjwatson.ledger.gui.LedgerMenu;
import com.daytonjwatson.ledger.gui.MenuId;
import com.daytonjwatson.ledger.tools.RepairService;
import com.daytonjwatson.ledger.tools.ToolMetaService;
import com.daytonjwatson.ledger.tools.ToolParser;
import com.daytonjwatson.ledger.tools.ToolVendorService;
import com.daytonjwatson.ledger.tools.ToolVendorService.ToolTier;
import com.daytonjwatson.ledger.tools.ToolVendorService.ToolType;
import com.daytonjwatson.ledger.tools.ToolVendorService.ToolVariant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class RepairMenu implements LedgerMenu {
	private static final int MENU_SIZE = 27;
	private static final int SLOT_ITEM = 11;
	private static final int SLOT_INFO = 15;
	private static final int SLOT_BACK = 18;
	private static final int SLOT_REPAIR = 22;

	private final GuiManager guiManager;
	private final MoneyService moneyService;
	private final RepairService repairService;
	private final ToolMetaService toolMetaService;
	private final ToolVendorService toolVendorService;
	private final JavaPlugin plugin;
	private final ItemStack fillerItem;

	public RepairMenu(GuiManager guiManager, MoneyService moneyService, RepairService repairService, ToolMetaService toolMetaService, ToolVendorService toolVendorService, JavaPlugin plugin) {
		this.guiManager = guiManager;
		this.moneyService = moneyService;
		this.repairService = repairService;
		this.toolMetaService = toolMetaService;
		this.toolVendorService = toolVendorService;
		this.plugin = plugin;
		this.fillerItem = createFillerItem();
	}

	@Override
	public MenuId id() {
		return MenuId.REPAIR;
	}

	@Override
	public Inventory build(Player player) {
		Inventory inventory = Bukkit.createInventory(new LedgerHolder(id()), MENU_SIZE, GuiManager.MENU_TITLE_PREFIX + "Repair");
		for (int slot = 0; slot < MENU_SIZE; slot++) {
			inventory.setItem(slot, fillerItem);
		}
		inventory.setItem(SLOT_ITEM, null);
		inventory.setItem(SLOT_INFO, createNoToolCostItem());
		inventory.setItem(SLOT_REPAIR, createRepairButton(false, "Place a tool to repair."));
		inventory.setItem(SLOT_BACK, createBackButton());
		return inventory;
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clicked, ClickType type, InventoryClickEvent event) {
		Inventory inventory = event.getView().getTopInventory();
		if (slot == SLOT_BACK) {
			event.setCancelled(true);
			returnRepairItem(player, inventory);
			guiManager.open(MenuId.HUB, player);
			return;
		}
		if (slot == SLOT_REPAIR) {
			event.setCancelled(true);
			handleRepair(player, inventory);
			return;
		}
		if (slot < inventory.getSize() && slot != SLOT_ITEM) {
			event.setCancelled(true);
			return;
		}
		if (slot == SLOT_ITEM) {
			if (!isValidRepairAction(player, event, inventory)) {
				event.setCancelled(true);
				return;
			}
			scheduleRefresh(inventory);
			return;
		}
		if (event.isShiftClick()) {
			if (!canShiftMoveToSlot(event, inventory)) {
				event.setCancelled(true);
				return;
			}
			scheduleRefresh(inventory);
		}
	}

	@Override
	public boolean cancelAllClicks() {
		return false;
	}

	@Override
	public void onClose(Player player, Inventory inventory) {
		returnRepairItem(player, inventory);
	}

	private void handleRepair(Player player, Inventory inventory) {
		ItemStack item = getRepairItem(inventory);
		if (!isValidRepairItem(item)) {
			player.sendMessage(ChatColor.RED + "Place a tool to repair.");
			refreshDisplay(inventory);
			return;
		}
		double remaining = getRemainingDurability(item);
		if (remaining <= 0.0) {
			player.sendMessage(ChatColor.RED + "Cannot repair broken tools.");
			refreshDisplay(inventory);
			return;
		}
		long cost = repairService.getRepairCost(item);
		if (cost < 0) {
			player.sendMessage(ChatColor.RED + "Unable to calculate repair cost.");
			refreshDisplay(inventory);
			return;
		}
		long banked = moneyService.getBanked(player.getUniqueId());
		if (banked < cost) {
			player.sendMessage(ChatColor.RED + "Not enough banked money. Cost $" + formatMoney(cost) + ".");
			refreshDisplay(inventory);
			return;
		}
		if (!moneyService.removeBanked(player, cost)) {
			player.sendMessage(ChatColor.RED + "Unable to withdraw banked money.");
			refreshDisplay(inventory);
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta instanceof Damageable damageable) {
			damageable.setDamage(0);
			item.setItemMeta(damageable);
		}
		toolMetaService.incrementRepairCount(item);
		player.sendMessage(ChatColor.GREEN + "Repaired for $" + formatMoney(cost) + ".");
		refreshDisplay(inventory);
	}

	private boolean isValidRepairAction(Player player, InventoryClickEvent event, Inventory inventory) {
		ClickType clickType = event.getClick();
		if (clickType == ClickType.DOUBLE_CLICK || clickType == ClickType.MIDDLE || clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) {
			return false;
		}
		ItemStack incoming = resolveIncomingItem(player, event);
		if (incoming == null || incoming.getType() == Material.AIR) {
			return true;
		}
		if (!isValidRepairItem(incoming)) {
			player.sendMessage(ChatColor.RED + "Only repairable tools can go here.");
			return false;
		}
		return true;
	}

	private boolean canShiftMoveToSlot(InventoryClickEvent event, Inventory inventory) {
		if (event.getRawSlot() < inventory.getSize()) {
			return true;
		}
		ItemStack current = event.getCurrentItem();
		if (!isValidRepairItem(current)) {
			return false;
		}
		return getRepairItem(inventory) == null;
	}

	private ItemStack resolveIncomingItem(Player player, InventoryClickEvent event) {
		if (event.getClick() == ClickType.NUMBER_KEY) {
			int hotbar = event.getHotbarButton();
			if (hotbar >= 0) {
				return player.getInventory().getItem(hotbar);
			}
		}
		if (event.getClick() == ClickType.SWAP_OFFHAND) {
			return player.getInventory().getItemInOffHand();
		}
		return event.getCursor();
	}

	private void scheduleRefresh(Inventory inventory) {
		Bukkit.getScheduler().runTask(plugin, () -> refreshDisplay(inventory));
	}

	private void refreshDisplay(Inventory inventory) {
		ItemStack item = getRepairItem(inventory);
		if (item == null) {
			inventory.setItem(SLOT_INFO, createNoToolCostItem());
			inventory.setItem(SLOT_REPAIR, createRepairButton(false, "Place a tool to repair."));
			return;
		}
		inventory.setItem(SLOT_INFO, createCostItem(item));
		boolean canRepair = canRepair(item);
		inventory.setItem(SLOT_REPAIR, createRepairButton(canRepair, canRepair ? "Repair this tool." : getRepairDisabledReason(item)));
	}

	private ItemStack getRepairItem(Inventory inventory) {
		if (inventory == null) {
			return null;
		}
		ItemStack item = inventory.getItem(SLOT_ITEM);
		if (item == null || item.getType() == Material.AIR) {
			return null;
		}
		return item;
	}

	private void returnRepairItem(Player player, Inventory inventory) {
		ItemStack item = getRepairItem(inventory);
		if (item == null) {
			return;
		}
		inventory.setItem(SLOT_ITEM, null);
		Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
		if (!leftovers.isEmpty()) {
			leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
		}
		refreshDisplay(inventory);
	}

	private boolean isSupportedTool(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}
		return ToolParser.getTier(item.getType()) != null && ToolParser.getType(item.getType()) != null;
	}

	private boolean canRepair(ItemStack item) {
		return isValidRepairItem(item) && getRemainingDurability(item) > 0.0 && repairService.getRepairCost(item) >= 0;
	}

	private boolean isValidRepairItem(ItemStack item) {
		return isSupportedTool(item) && item.getAmount() == 1;
	}

	private String getRepairDisabledReason(ItemStack item) {
		if (!isValidRepairItem(item)) {
			return "Place a tool to repair.";
		}
		if (getRemainingDurability(item) <= 0.0) {
			return "Cannot repair broken tools.";
		}
		return "Unable to repair this tool.";
	}

	private ItemStack createNoToolCostItem() {
		ItemStack item = new ItemStack(Material.ANVIL);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.YELLOW + "Repair Cost");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Place a tool to view cost.");
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createCostItem(ItemStack item) {
		ItemStack display = new ItemStack(Material.ANVIL);
		ItemMeta meta = display.getItemMeta();
		if (meta == null) {
			return display;
		}
		meta.setDisplayName(ChatColor.YELLOW + "Repair Cost");
		List<String> lore = new ArrayList<>();
		ToolType type = ToolParser.getType(item.getType());
		ToolTier tier = ToolParser.getTier(item.getType());
		ToolVariant variant = resolveVariant(item);
		long buyPrice = type == null || tier == null ? 0L : toolVendorService.getBuyPrice(type, tier, variant);
		long repairCost = repairService.getRepairCost(item);
		double remaining = getRemainingDurability(item);
		int repairCount = toolMetaService.getRepairCount(item);
		lore.add(ChatColor.GRAY + "Buy price: " + ChatColor.GOLD + "$" + formatMoney(buyPrice));
		lore.add(ChatColor.GRAY + "Remaining durability: " + ChatColor.AQUA + formatPercent(remaining));
		lore.add(ChatColor.GRAY + "Repair count: " + ChatColor.YELLOW + repairCount);
		if (remaining <= 0.0) {
			lore.add(ChatColor.RED + "Cannot repair broken tool.");
			lore.add(ChatColor.GRAY + "Repair cost: " + ChatColor.RED + "N/A");
		} else if (repairCost >= 0) {
			lore.add(ChatColor.GRAY + "Repair cost: " + ChatColor.GOLD + "$" + formatMoney(repairCost));
		} else {
			lore.add(ChatColor.GRAY + "Repair cost: " + ChatColor.RED + "N/A");
		}
		meta.setLore(lore);
		display.setItemMeta(meta);
		return display;
	}

	private ItemStack createRepairButton(boolean enabled, String loreText) {
		Material material = enabled ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + "Repair Tool");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + loreText);
		lore.add(ChatColor.DARK_GRAY + "Banked money only");
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createBackButton() {
		ItemStack item = new ItemStack(Material.BARRIER);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.RED + "Back");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Return to the hub");
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
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

	private ToolVariant resolveVariant(ItemStack item) {
		if (item != null && item.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
			return ToolVariant.SILK_TOUCH;
		}
		if (item != null && item.getEnchantments().containsKey(Enchantment.EFFICIENCY)) {
			return ToolVariant.EFFICIENCY;
		}
		return ToolVariant.STANDARD;
	}

	private double getRemainingDurability(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (!(meta instanceof Damageable damageable)) {
			return 1.0;
		}
		int max = item.getType().getMaxDurability();
		if (max <= 0) {
			return 1.0;
		}
		return Math.max(0.0, 1.0 - ((double) damageable.getDamage() / max));
	}

	private String formatPercent(double value) {
		double percent = Math.max(0.0, Math.min(1.0, value)) * 100.0;
		return String.format("%.0f%%", percent);
	}

	private String formatMoney(long value) {
		return String.format("%,d", value);
	}
}
