package com.daytonjwatson.ledger.gui.menus;

import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.gui.GuiManager;
import com.daytonjwatson.ledger.gui.LedgerHolder;
import com.daytonjwatson.ledger.gui.LedgerMenu;
import com.daytonjwatson.ledger.gui.MenuId;
import com.daytonjwatson.ledger.upgrades.UpgradeDefinition;
import com.daytonjwatson.ledger.upgrades.UpgradeService;
import com.daytonjwatson.ledger.upgrades.UpgradeService.PurchaseResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class UpgradesMenu implements LedgerMenu {
	private static final int MENU_SIZE = 54;
	private static final int PAGE_SIZE = 36;
	private static final int ENTRY_START = 9;
	private final GuiManager guiManager;
	private final UpgradeService upgradeService;
	private final MoneyService moneyService;
	private final ItemStack headerItem;

	public UpgradesMenu(GuiManager guiManager, UpgradeService upgradeService, MoneyService moneyService) {
		this.guiManager = guiManager;
		this.upgradeService = upgradeService;
		this.moneyService = moneyService;
		this.headerItem = createHeaderItem();
	}

	@Override
	public MenuId id() {
		return MenuId.UPGRADES;
	}

	@Override
	public Inventory build(Player player) {
		Inventory inventory = Bukkit.createInventory(new LedgerHolder(id()), MENU_SIZE, GuiManager.MENU_TITLE_PREFIX + "Upgrades");
		for (int slot = 0; slot <= 8; slot++) {
			inventory.setItem(slot, headerItem);
		}
		List<UpgradeDefinition> definitions = getOrderedDefinitions();
		int page = clampPage(player.getUniqueId(), definitions.size());
		int startIndex = page * PAGE_SIZE;
		for (int index = 0; index < PAGE_SIZE; index++) {
			int definitionIndex = startIndex + index;
			if (definitionIndex >= definitions.size()) {
				break;
			}
			int slot = ENTRY_START + index;
			inventory.setItem(slot, createUpgradeItem(player, definitions.get(definitionIndex)));
		}
		inventory.setItem(45, createButton(Material.BARRIER, ChatColor.RED, "Back"));
		inventory.setItem(47, createButton(Material.ARROW, ChatColor.YELLOW, "Previous Page"));
		inventory.setItem(49, createPlayerInfoItem(player));
		inventory.setItem(51, createButton(Material.ARROW, ChatColor.YELLOW, "Next Page"));
		inventory.setItem(53, createButton(Material.CLOCK, ChatColor.YELLOW, "Refresh"));
		return inventory;
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clicked, ClickType type, InventoryClickEvent event) {
		if (slot == 45) {
			guiManager.open(MenuId.HUB, player);
			return;
		}
		if (slot == 47) {
			updatePage(player.getUniqueId(), -1);
			guiManager.open(MenuId.UPGRADES, player);
			return;
		}
		if (slot == 51) {
			updatePage(player.getUniqueId(), 1);
			guiManager.open(MenuId.UPGRADES, player);
			return;
		}
		if (slot == 53) {
			guiManager.open(MenuId.UPGRADES, player);
			return;
		}
		if (slot < ENTRY_START || slot >= ENTRY_START + PAGE_SIZE) {
			return;
		}
		List<UpgradeDefinition> definitions = getOrderedDefinitions();
		int page = clampPage(player.getUniqueId(), definitions.size());
		int index = page * PAGE_SIZE + (slot - ENTRY_START);
		if (index < 0 || index >= definitions.size()) {
			return;
		}
		UpgradeDefinition definition = definitions.get(index);
		PurchaseResult result = upgradeService.purchaseUpgrade(player, definition.getId());
		player.sendMessage(result.getMessage());
		if (result.isSuccess()) {
			guiManager.open(MenuId.UPGRADES, player);
		}
	}

	private List<UpgradeDefinition> getOrderedDefinitions() {
		List<UpgradeDefinition> definitions = new ArrayList<>(upgradeService.getDefinitions().values());
		definitions.sort(Comparator.comparing((UpgradeDefinition definition) -> definition.getUnlocksVendorTier() > 0 ? 0 : 1)
			.thenComparing(definition -> definition.getType() == UpgradeDefinition.Type.CHOICE ? 0 : 1)
			.thenComparing(definition -> definition.getType() == UpgradeDefinition.Type.LEVEL ? 0 : 1)
			.thenComparing(definition -> definition.getId().toLowerCase(Locale.ROOT)));
		return definitions;
	}

	private int clampPage(UUID playerId, int totalDefinitions) {
		int maxPage = Math.max(0, (totalDefinitions - 1) / PAGE_SIZE);
		int page = guiManager.getUpgradePage(playerId);
		if (page < 0) {
			page = 0;
		}
		if (page > maxPage) {
			page = maxPage;
		}
		guiManager.setUpgradePage(playerId, page);
		return page;
	}

	private void updatePage(UUID playerId, int delta) {
		guiManager.setUpgradePage(playerId, guiManager.getUpgradePage(playerId) + delta);
	}

	private ItemStack createUpgradeItem(Player player, UpgradeDefinition definition) {
		UUID uuid = player.getUniqueId();
		int level = upgradeService.getLevel(uuid, definition.getId());
		boolean maxed = definition.getType() == UpgradeDefinition.Type.LEVEL && level >= definition.getMaxLevel();
		boolean metPrereqs = upgradeService.meetsPrerequisites(uuid, definition.getId());
		long nextCost = definition.getNextCost(level);
		boolean complete = maxed || (definition.getType() == UpgradeDefinition.Type.CHOICE && level > 0);
		boolean purchasable = metPrereqs && !complete;

		ItemStack item = new ItemStack(resolveIcon(definition));
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		ChatColor nameColor = complete ? ChatColor.GRAY : (purchasable ? ChatColor.GREEN : ChatColor.RED);
		meta.setDisplayName(nameColor + definition.getName());

		List<String> lore = new ArrayList<>();
		lore.addAll(wrapDescription(definition.getDescription()));
		lore.add(" ");
		lore.add(ChatColor.GRAY + "Current state");
		if (definition.getType() == UpgradeDefinition.Type.LEVEL) {
			lore.add(ChatColor.GRAY + "Level: " + level + " / " + definition.getMaxLevel());
		} else {
			lore.add(ChatColor.GRAY + "Choice: " + definition.getSpecializationChoice());
		}
		lore.add(ChatColor.GRAY + "Next cost: $" + nextCost);
		lore.add(ChatColor.GRAY + "Next effect: " + upgradeService.getNextEffect(definition, uuid, level + 1));
		if (definition.getUnlocksVendorTier() > 0) {
			lore.add(ChatColor.GRAY + "Unlocks vendor tier: " + definition.getUnlocksVendorTier());
		}
		if (definition.getSpecializationRequirement() != null) {
			lore.add(ChatColor.GRAY + "Requires specialization: " + definition.getSpecializationRequirement());
		}
		if (!metPrereqs && !definition.getPrerequisites().isEmpty()) {
			lore.add(ChatColor.RED + "Missing prerequisites:");
			for (String prerequisite : definition.getPrerequisites()) {
				if (!upgradeService.hasUpgrade(uuid, prerequisite)) {
					lore.add(ChatColor.RED + " - " + prerequisite);
				}
			}
		}
		lore.add(" ");
		if (purchasable) {
			lore.add(ChatColor.GREEN + "Click to purchase");
		} else if (complete) {
			lore.add(ChatColor.GRAY + "Complete");
		} else {
			lore.add(ChatColor.RED + "Locked");
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private Material resolveIcon(UpgradeDefinition definition) {
		if (definition.getUnlocksVendorTier() > 0) {
			return Material.ANVIL;
		}
		if (definition.getSpecializationChoice() != null) {
			String choice = definition.getSpecializationChoice().toUpperCase(Locale.ROOT);
			if ("MINER".equals(choice)) {
				return Material.IRON_PICKAXE;
			}
			if ("FARMER".equals(choice)) {
				return Material.WHEAT;
			}
			if ("HUNTER".equals(choice)) {
				return Material.IRON_SWORD;
			}
		}
		String id = definition.getId().toLowerCase(Locale.ROOT);
		if (id.contains("barter")) {
			return Material.EMERALD;
		}
		if (id.contains("insurance")) {
			return Material.SHIELD;
		}
		if (id.contains("logistics")) {
			return Material.CHEST_MINECART;
		}
		if (definition.getRefinementLevel() > 0) {
			return Material.FURNACE;
		}
		return Material.BOOK;
	}

	private ItemStack createPlayerInfoItem(Player player) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.AQUA + "Player Info");
		List<String> lore = new ArrayList<>();
		UUID uuid = player.getUniqueId();
		lore.add(ChatColor.GRAY + "Banked: " + ChatColor.GOLD + "$" + formatMoney(moneyService.getBanked(uuid)));
		String specialization = upgradeService.getSpecializationChoice(uuid);
		lore.add(ChatColor.GRAY + "Specialization: " + (specialization == null ? "None" : specialization));
		lore.add(ChatColor.GRAY + "Vendor tiers:");
		lore.add(formatVendorTier(uuid, 2));
		lore.add(formatVendorTier(uuid, 3));
		lore.add(formatVendorTier(uuid, 4));
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private String formatVendorTier(UUID uuid, int tier) {
		boolean unlocked = upgradeService.hasVendorTierUnlocked(uuid, tier);
		return ChatColor.GRAY + "Tier " + tier + ": " + (unlocked ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Locked");
	}

	private ItemStack createButton(Material material, ChatColor color, String name) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(color + name);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createHeaderItem() {
		ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(" ");
			item.setItemMeta(meta);
		}
		return item;
	}

	private List<String> wrapDescription(String description) {
		String sanitized = description == null ? "" : description;
		if (sanitized.isBlank()) {
			return List.of(ChatColor.GRAY + sanitized);
		}
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : sanitized.split("\\s+")) {
			if (current.length() + word.length() + 1 > 36) {
				lines.add(ChatColor.GRAY + current.toString());
				current.setLength(0);
			}
			if (current.length() > 0) {
				current.append(' ');
			}
			current.append(word);
		}
		if (current.length() > 0) {
			lines.add(ChatColor.GRAY + current.toString());
		}
		return lines;
	}

	private String formatMoney(long value) {
		return String.format("%,d", value);
	}
}
