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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class RepairMenu implements LedgerMenu {
	private static final int MENU_SIZE = 27;
	private static final int SLOT_PREVIEW = 11;
	private static final int SLOT_INFO = 15;
	private static final int SLOT_BACK = 18;
	private static final int SLOT_REPAIR = 22;

	private final GuiManager guiManager;
	private final MoneyService moneyService;
	private final RepairService repairService;
	private final ToolMetaService toolMetaService;
	private final ToolVendorService toolVendorService;
	private final ItemStack fillerItem;

	public RepairMenu(GuiManager guiManager, MoneyService moneyService, RepairService repairService, ToolMetaService toolMetaService, ToolVendorService toolVendorService) {
		this.guiManager = guiManager;
		this.moneyService = moneyService;
		this.repairService = repairService;
		this.toolMetaService = toolMetaService;
		this.toolVendorService = toolVendorService;
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
		ItemStack held = player.getInventory().getItemInMainHand();
		if (!isSupportedTool(held)) {
			inventory.setItem(SLOT_PREVIEW, createHoldToolItem());
			inventory.setItem(SLOT_INFO, createNoToolCostItem());
			inventory.setItem(SLOT_REPAIR, createRepairButton(false, "Hold a tool to repair."));
			inventory.setItem(SLOT_BACK, createBackButton());
			return inventory;
		}
		inventory.setItem(SLOT_PREVIEW, held.clone());
		inventory.setItem(SLOT_INFO, createCostItem(held));
		boolean canRepair = canRepair(held);
		inventory.setItem(SLOT_REPAIR, createRepairButton(canRepair, canRepair ? "Repair your held tool." : getRepairDisabledReason(held)));
		inventory.setItem(SLOT_BACK, createBackButton());
		return inventory;
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clicked, ClickType type) {
		if (slot == SLOT_BACK) {
			guiManager.open(MenuId.HUB, player);
			return;
		}
		if (slot != SLOT_REPAIR) {
			return;
		}
		ItemStack held = player.getInventory().getItemInMainHand();
		if (!isSupportedTool(held)) {
			player.sendMessage(ChatColor.RED + "Hold a tool to repair.");
			guiManager.open(MenuId.REPAIR, player);
			return;
		}
		double remaining = getRemainingDurability(held);
		if (remaining <= 0.0) {
			player.sendMessage(ChatColor.RED + "Cannot repair broken tools.");
			guiManager.open(MenuId.REPAIR, player);
			return;
		}
		long cost = repairService.getRepairCost(held);
		if (cost < 0) {
			player.sendMessage(ChatColor.RED + "Unable to calculate repair cost.");
			guiManager.open(MenuId.REPAIR, player);
			return;
		}
		long banked = moneyService.getBanked(player.getUniqueId());
		if (banked < cost) {
			player.sendMessage(ChatColor.RED + "Not enough banked money. Cost $" + formatMoney(cost) + ".");
			guiManager.open(MenuId.REPAIR, player);
			return;
		}
		if (!moneyService.removeBanked(player, cost)) {
			player.sendMessage(ChatColor.RED + "Unable to withdraw banked money.");
			guiManager.open(MenuId.REPAIR, player);
			return;
		}
		ItemMeta meta = held.getItemMeta();
		if (meta instanceof Damageable damageable) {
			damageable.setDamage(0);
			held.setItemMeta(damageable);
		}
		toolMetaService.incrementRepairCount(held);
		player.sendMessage(ChatColor.GREEN + "Repaired for $" + formatMoney(cost) + ".");
		guiManager.open(MenuId.REPAIR, player);
	}

	private boolean isSupportedTool(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}
		return ToolParser.getTier(item.getType()) != null && ToolParser.getType(item.getType()) != null;
	}

	private boolean canRepair(ItemStack item) {
		return isSupportedTool(item) && getRemainingDurability(item) > 0.0 && repairService.getRepairCost(item) >= 0;
	}

	private String getRepairDisabledReason(ItemStack item) {
		if (!isSupportedTool(item)) {
			return "Hold a tool to repair.";
		}
		if (getRemainingDurability(item) <= 0.0) {
			return "Cannot repair broken tools.";
		}
		return "Unable to repair this tool.";
	}

	private ItemStack createHoldToolItem() {
		ItemStack item = new ItemStack(Material.BARRIER);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.RED + "Hold a tool");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Hold a supported tool");
		lore.add(ChatColor.GRAY + "to view repair info.");
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createNoToolCostItem() {
		ItemStack item = new ItemStack(Material.ANVIL);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.YELLOW + "Repair Cost");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Hold a tool to view cost.");
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
		meta.setDisplayName(ChatColor.GREEN + "Repair Held Tool");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + loreText);
		lore.add(ChatColor.DARK_GRAY + "Banked money only");
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createBackButton() {
		ItemStack item = new ItemStack(Material.ARROW);
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
