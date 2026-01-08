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

public class PlaceholderMenu implements LedgerMenu {
	private static final int MENU_SIZE = 27;
	private final MenuId menuId;
	private final String title;

	public PlaceholderMenu(MenuId menuId, String title) {
		this.menuId = menuId;
		this.title = title;
	}

	@Override
	public MenuId id() {
		return menuId;
	}

	@Override
	public Inventory build(Player player) {
		Inventory inventory = Bukkit.createInventory(new LedgerHolder(id()), MENU_SIZE, GuiManager.MENU_TITLE_PREFIX + title);
		ItemStack placeholder = new ItemStack(Material.BARRIER);
		ItemMeta meta = placeholder.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(ChatColor.RED + "Coming soon");
			placeholder.setItemMeta(meta);
		}
		inventory.setItem(13, placeholder);
		return inventory;
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clicked, ClickType type, InventoryClickEvent event) {
	}
}
