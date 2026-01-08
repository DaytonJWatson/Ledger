package com.daytonjwatson.ledger.gui;

import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

public class GuiManager {
	public static final String MENU_TITLE_PREFIX = "Ledger \u2022 ";

	private final SpawnRegionService spawnRegionService;
	private final Map<MenuId, LedgerMenu> menus;
	private final Map<UUID, MenuId> sessions;
	private final Map<UUID, Integer> upgradePages;

	public GuiManager(SpawnRegionService spawnRegionService) {
		this.spawnRegionService = spawnRegionService;
		this.menus = new EnumMap<>(MenuId.class);
		this.sessions = new ConcurrentHashMap<>();
		this.upgradePages = new ConcurrentHashMap<>();
	}

	public void register(LedgerMenu menu) {
		if (menu != null) {
			menus.put(menu.id(), menu);
		}
	}

	public LedgerMenu getMenu(MenuId id) {
		return menus.get(id);
	}

	public MenuId getSessionMenu(UUID playerId) {
		return sessions.get(playerId);
	}

	public void clearSession(UUID playerId) {
		sessions.remove(playerId);
		upgradePages.remove(playerId);
	}

	public int getUpgradePage(UUID playerId) {
		return upgradePages.getOrDefault(playerId, 0);
	}

	public void setUpgradePage(UUID playerId, int page) {
		upgradePages.put(playerId, Math.max(0, page));
	}

	public boolean isLedgerMenu(InventoryView view) {
		if (view == null) {
			return false;
		}
		Inventory topInventory = view.getTopInventory();
		if (topInventory == null) {
			return false;
		}
		if (topInventory.getHolder() instanceof LedgerHolder) {
			return true;
		}
		String title = view.getTitle();
		return title != null && title.startsWith(MENU_TITLE_PREFIX);
	}

	public MenuId resolveMenuId(InventoryView view, UUID playerId) {
		if (view != null) {
			InventoryHolder holder = view.getTopInventory().getHolder();
			if (holder instanceof LedgerHolder) {
				return ((LedgerHolder) holder).getMenuId();
			}
		}
		return sessions.get(playerId);
	}

	public void open(MenuId menuId, Player player) {
		if (menuId == null || player == null) {
			return;
		}
		if (menuId == MenuId.UPGRADES && !spawnRegionService.isInSpawn(player.getLocation())) {
			player.sendMessage(ChatColor.RED + "You can only use menus at spawn.");
			return;
		}
		LedgerMenu menu = menus.get(menuId);
		if (menu == null) {
			player.sendMessage(ChatColor.RED + "That menu is not available.");
			return;
		}
		Inventory inventory = menu.build(player);
		if (inventory == null) {
			return;
		}
		InventoryHolder holder = inventory.getHolder();
		if (holder instanceof LedgerHolder) {
			((LedgerHolder) holder).setInventory(inventory);
		}
		sessions.put(player.getUniqueId(), menuId);
		player.openInventory(inventory);
	}
}
