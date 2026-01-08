package com.daytonjwatson.ledger.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface LedgerMenu {

	MenuId id();

	Inventory build(Player player);

	void onClick(Player player, int slot, ItemStack clicked, ClickType type);

	default boolean cancelAllClicks() {
		return true;
	}
}
