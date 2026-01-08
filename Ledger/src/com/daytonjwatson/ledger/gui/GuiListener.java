package com.daytonjwatson.ledger.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;

public class GuiListener implements Listener {
	private final GuiManager guiManager;

	public GuiListener(GuiManager guiManager) {
		this.guiManager = guiManager;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		InventoryView view = event.getView();
		if (!guiManager.isLedgerMenu(view)) {
			return;
		}
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		Player player = (Player) event.getWhoClicked();
		MenuId menuId = guiManager.resolveMenuId(view, player.getUniqueId());
		if (menuId != null) {
			LedgerMenu menu = guiManager.getMenu(menuId);
			if (menu != null) {
				event.setCancelled(menu.cancelAllClicks());
				menu.onClick(player, event.getRawSlot(), event.getCurrentItem(), event.getClick(), event);
				return;
			}
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		if (guiManager.isLedgerMenu(event.getView())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player player) {
			MenuId menuId = guiManager.resolveMenuId(event.getView(), player.getUniqueId());
			if (menuId != null) {
				LedgerMenu menu = guiManager.getMenu(menuId);
				if (menu != null) {
					menu.onClose(player, event.getView().getTopInventory());
				}
			}
			guiManager.clearSession(player.getUniqueId());
		}
	}
}
