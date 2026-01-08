package com.daytonjwatson.ledger.gui.menus;

import com.daytonjwatson.ledger.gui.GuiManager;
import com.daytonjwatson.ledger.gui.LedgerHolder;
import com.daytonjwatson.ledger.gui.LedgerMenu;
import com.daytonjwatson.ledger.gui.MenuId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HubMenu implements LedgerMenu {
	private static final int MENU_SIZE = 27;
	private final GuiManager guiManager;
	private final ItemStack borderItem;

	public HubMenu(GuiManager guiManager) {
		this.guiManager = guiManager;
		this.borderItem = createBorderItem();
	}

	@Override
	public MenuId id() {
		return MenuId.HUB;
	}

	@Override
	public Inventory build(Player player) {
		Inventory inventory = Bukkit.createInventory(new LedgerHolder(id()), MENU_SIZE, GuiManager.MENU_TITLE_PREFIX + "Hub");
		for (int slot = 0; slot < MENU_SIZE; slot++) {
			if (isBorderSlot(slot)) {
				inventory.setItem(slot, borderItem);
			}
		}
		inventory.setItem(10, createButton(Material.EMERALD, "Sell"));
		inventory.setItem(12, createButton(Material.CHEST, "Bank"));
		inventory.setItem(14, createButton(Material.IRON_PICKAXE, "Tools"));
		inventory.setItem(16, createButton(Material.NETHER_STAR, "Upgrades"));
		inventory.setItem(22, createButton(Material.ANVIL, "Repair"));
		return inventory;
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clicked, ClickType type, InventoryClickEvent event) {
		switch (slot) {
			case 10 -> guiManager.open(MenuId.SELL, player);
			case 12 -> guiManager.open(MenuId.BANK, player);
			case 14 -> guiManager.open(MenuId.TOOLS, player);
			case 16 -> guiManager.open(MenuId.UPGRADES, player);
			case 22 -> guiManager.open(MenuId.REPAIR, player);
			default -> {
			}
		}
	}

	private ItemStack createBorderItem() {
		ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(" ");
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack createButton(Material material, String name) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.YELLOW + name);
		item.setItemMeta(meta);
		return item;
	}

	private boolean isBorderSlot(int slot) {
		int row = slot / 9;
		int col = slot % 9;
		return row == 0 || row == 2 || col == 0 || col == 8;
	}

}
