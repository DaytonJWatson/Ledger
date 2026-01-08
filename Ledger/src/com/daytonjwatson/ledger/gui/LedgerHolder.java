package com.daytonjwatson.ledger.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LedgerHolder implements InventoryHolder {
	private final MenuId menuId;
	private Inventory inventory;

	public LedgerHolder(MenuId menuId) {
		this.menuId = menuId;
	}

	public MenuId getMenuId() {
		return menuId;
	}

	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
