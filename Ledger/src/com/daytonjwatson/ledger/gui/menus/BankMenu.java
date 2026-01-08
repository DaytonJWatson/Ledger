package com.daytonjwatson.ledger.gui.menus;

import com.daytonjwatson.ledger.economy.BankService;
import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.gui.GuiManager;
import com.daytonjwatson.ledger.gui.LedgerHolder;
import com.daytonjwatson.ledger.gui.LedgerMenu;
import com.daytonjwatson.ledger.gui.MenuId;
import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BankMenu implements LedgerMenu {
	private static final int MENU_SIZE = 27;
	private final GuiManager guiManager;
	private final MoneyService moneyService;
	private final BankService bankService;
	private final SpawnRegionService spawnRegionService;

	public BankMenu(GuiManager guiManager, MoneyService moneyService, BankService bankService, SpawnRegionService spawnRegionService) {
		this.guiManager = guiManager;
		this.moneyService = moneyService;
		this.bankService = bankService;
		this.spawnRegionService = spawnRegionService;
	}

	@Override
	public MenuId id() {
		return MenuId.BANK;
	}

	@Override
	public Inventory build(Player player) {
		Inventory inventory = Bukkit.createInventory(new LedgerHolder(id()), MENU_SIZE, GuiManager.MENU_TITLE_PREFIX + "Bank");
		inventory.setItem(11, createBalanceItem(Material.GOLD_INGOT, ChatColor.YELLOW + "Carried Balance", moneyService.getCarried(player.getUniqueId())));
		inventory.setItem(15, createBalanceItem(Material.EMERALD, ChatColor.YELLOW + "Banked Balance", moneyService.getBanked(player.getUniqueId())));
		inventory.setItem(12, createDepositButton(Material.LIME_DYE, ChatColor.GREEN + "Deposit 25%", "Deposit 25% of your carried money"));
		inventory.setItem(13, createDepositButton(Material.EMERALD_BLOCK, ChatColor.GREEN + "Deposit All", "Deposit all carried money"));
		inventory.setItem(14, createDepositButton(Material.GREEN_DYE, ChatColor.GREEN + "Deposit 50%", "Deposit 50% of your carried money"));
		inventory.setItem(22, createDepositButton(Material.BARRIER, ChatColor.RED + "Back", "Return to the hub"));
		return inventory;
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clicked, ClickType type, InventoryClickEvent event) {
		switch (slot) {
			case 12 -> handleDeposit(player, DepositMode.QUARTER);
			case 13 -> handleDeposit(player, DepositMode.ALL);
			case 14 -> handleDeposit(player, DepositMode.HALF);
			case 22 -> guiManager.open(MenuId.HUB, player);
			default -> {
			}
		}
	}

	private void handleDeposit(Player player, DepositMode mode) {
		if (!spawnRegionService.isInSpawn(player.getLocation())) {
			player.sendMessage(ChatColor.RED + "You can only bank money at spawn.");
			return;
		}
		long carried = moneyService.getCarried(player.getUniqueId());
		if (carried <= 0) {
			player.sendMessage(ChatColor.RED + "You have no carried money to deposit.");
			return;
		}
		long depositAmount = mode.resolve(carried);
		if (depositAmount <= 0) {
			player.sendMessage(ChatColor.RED + "You have no carried money to deposit.");
			return;
		}
		if (!bankService.deposit(player, depositAmount)) {
			player.sendMessage(ChatColor.RED + "Unable to deposit right now.");
			return;
		}
		player.sendMessage(ChatColor.GREEN + "Deposited $" + formatMoney(Math.min(depositAmount, carried)) + ".");
		guiManager.open(MenuId.BANK, player);
	}

	private ItemStack createBalanceItem(Material material, String name, long amount) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(name);
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + "$" + formatMoney(amount));
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createDepositButton(Material material, String name, String loreText) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(name);
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + loreText);
		lore.add(ChatColor.RED + "Spawn only");
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private String formatMoney(long value) {
		return String.format("%,d", value);
	}

	private enum DepositMode {
		ALL {
			@Override
			long resolve(long carried) {
				return carried;
			}
		},
		QUARTER {
			@Override
			long resolve(long carried) {
				return carried / 4L;
			}
		},
		HALF {
			@Override
			long resolve(long carried) {
				return carried / 2L;
			}
		};

		abstract long resolve(long carried);
	}
}
