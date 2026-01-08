package com.daytonjwatson.ledger.gui.menus;

import com.daytonjwatson.ledger.economy.MoneyService;
import com.daytonjwatson.ledger.gui.GuiManager;
import com.daytonjwatson.ledger.gui.LedgerHolder;
import com.daytonjwatson.ledger.gui.LedgerMenu;
import com.daytonjwatson.ledger.gui.MenuId;
import com.daytonjwatson.ledger.spawn.SpawnRegionService;
import com.daytonjwatson.ledger.tools.ToolSpec;
import com.daytonjwatson.ledger.tools.ToolVendorService;
import com.daytonjwatson.ledger.tools.ToolVendorService.ToolTier;
import com.daytonjwatson.ledger.tools.ToolVendorService.ToolType;
import com.daytonjwatson.ledger.tools.ToolVendorService.ToolVariant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ToolsMenu implements LedgerMenu {
	private static final int MENU_SIZE = 54;
	private static final int[] OFFER_SLOTS = {
		10, 11, 12, 13,
		19, 20, 21, 22,
		28, 29, 30, 31,
		37, 38, 39, 40
	};
	private static final Map<ToolType, Integer> TAB_SLOTS = Map.of(
		ToolType.PICKAXE, 1,
		ToolType.AXE, 3,
		ToolType.SHOVEL, 5,
		ToolType.SWORD, 7
	);

	private final GuiManager guiManager;
	private final MoneyService moneyService;
	private final ToolVendorService toolVendorService;
	private final SpawnRegionService spawnRegionService;
	private final ItemStack fillerItem;
	private final Map<UUID, ToolType> selections = new ConcurrentHashMap<>();

	public ToolsMenu(GuiManager guiManager, MoneyService moneyService, ToolVendorService toolVendorService, SpawnRegionService spawnRegionService) {
		this.guiManager = guiManager;
		this.moneyService = moneyService;
		this.toolVendorService = toolVendorService;
		this.spawnRegionService = spawnRegionService;
		this.fillerItem = createFillerItem();
	}

	@Override
	public MenuId id() {
		return MenuId.TOOLS;
	}

	@Override
	public Inventory build(Player player) {
		Inventory inventory = Bukkit.createInventory(new LedgerHolder(id()), MENU_SIZE, GuiManager.MENU_TITLE_PREFIX + "Tools");
		ToolType selected = selections.getOrDefault(player.getUniqueId(), ToolType.PICKAXE);
		for (int slot = 0; slot < 9; slot++) {
			inventory.setItem(slot, fillerItem);
		}
		for (ToolType type : ToolType.values()) {
			Integer slot = TAB_SLOTS.get(type);
			if (slot != null) {
				inventory.setItem(slot, createTabItem(type, type == selected));
			}
		}
		for (int slot = 9; slot < 45; slot++) {
			inventory.setItem(slot, fillerItem);
		}
		List<ToolSpec> offers = buildOffers(selected);
		for (int index = 0; index < offers.size() && index < OFFER_SLOTS.length; index++) {
			int slot = OFFER_SLOTS[index];
			inventory.setItem(slot, createOfferItem(player, offers.get(index)));
		}
		for (int slot = 45; slot < MENU_SIZE; slot++) {
			inventory.setItem(slot, fillerItem);
		}
		inventory.setItem(45, createButton(Material.ARROW, ChatColor.RED + "Back", ChatColor.GRAY + "Return to the hub"));
		inventory.setItem(49, createUnlocksItem(player));
		inventory.setItem(53, createButton(Material.CLOCK, ChatColor.YELLOW + "Refresh", ChatColor.GRAY + "Reload this menu"));
		return inventory;
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clicked, ClickType type) {
		if (slot == 45) {
			guiManager.open(MenuId.HUB, player);
			return;
		}
		if (slot == 53) {
			guiManager.open(MenuId.TOOLS, player);
			return;
		}
		ToolType tab = resolveTab(slot);
		if (tab != null) {
			selections.put(player.getUniqueId(), tab);
			guiManager.open(MenuId.TOOLS, player);
			return;
		}
		ToolSpec spec = resolveOffer(player, slot);
		if (spec == null) {
			return;
		}
		if (!spawnRegionService.isInSpawn(player.getLocation())) {
			player.sendMessage(ChatColor.RED + "You can only buy tools at spawn.");
			return;
		}
		if (!toolVendorService.isTierUnlocked(player, spec.getTier())) {
			player.sendMessage(ChatColor.RED + "That vendor tier is locked. Purchase the upgrade first.");
			return;
		}
		long price = toolVendorService.getBuyPrice(spec);
		long banked = moneyService.getBanked(player.getUniqueId());
		if (banked < price) {
			player.sendMessage(ChatColor.RED + "Not enough banked money. Cost $" + formatMoney(price) + ".");
			return;
		}
		if (!moneyService.removeBanked(player, price)) {
			player.sendMessage(ChatColor.RED + "Unable to withdraw banked money.");
			return;
		}
		ItemStack item = toolVendorService.buildTool(spec);
		if (item != null) {
			player.getInventory().addItem(item);
		}
		player.sendMessage(ChatColor.GREEN + "Tool purchased for $" + formatMoney(price) + ".");
		guiManager.open(MenuId.TOOLS, player);
	}

	private ToolType resolveTab(int slot) {
		for (Map.Entry<ToolType, Integer> entry : TAB_SLOTS.entrySet()) {
			if (entry.getValue() == slot) {
				return entry.getKey();
			}
		}
		return null;
	}

	private ToolSpec resolveOffer(Player player, int slot) {
		int index = -1;
		for (int i = 0; i < OFFER_SLOTS.length; i++) {
			if (OFFER_SLOTS[i] == slot) {
				index = i;
				break;
			}
		}
		if (index < 0) {
			return null;
		}
		ToolType selected = selections.getOrDefault(player.getUniqueId(), ToolType.PICKAXE);
		List<ToolSpec> offers = buildOffers(selected);
		if (index >= offers.size()) {
			return null;
		}
		return offers.get(index);
	}

	private List<ToolSpec> buildOffers(ToolType type) {
		List<ToolSpec> offers = new ArrayList<>();
		ToolVariant[] variants = {ToolVariant.STANDARD, ToolVariant.EFFICIENCY, ToolVariant.SILK_TOUCH};
		for (ToolTier tier : ToolTier.values()) {
			for (ToolVariant variant : variants) {
				offers.add(new ToolSpec(type, tier, variant));
			}
		}
		return offers;
	}

	private ItemStack createTabItem(ToolType type, boolean selected) {
		ItemStack item = new ItemStack(type.getMaterialForTier(ToolTier.IRON));
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.AQUA + formatType(type));
		List<String> lore = new ArrayList<>();
		if (selected) {
			lore.add(ChatColor.GREEN + "Selected");
			item.addUnsafeEnchantment(Enchantment.INFINITY, 1);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		} else {
			lore.add(ChatColor.YELLOW + "Click to view");
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createOfferItem(Player player, ToolSpec spec) {
		ItemStack item = new ItemStack(spec.getType().getMaterialForTier(spec.getTier()));
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.AQUA + formatTier(spec.getTier()) + " " + formatType(spec.getType()) + formatVariantSuffix(spec));
		List<String> lore = new ArrayList<>();
		long price = toolVendorService.getBuyPrice(spec);
		lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + "$" + formatMoney(price));
		boolean unlocked = toolVendorService.isTierUnlocked(player, spec.getTier());
		lore.add(ChatColor.GRAY + "Tier requirement: " + (unlocked ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Locked"));
		lore.add(ChatColor.GRAY + "Variant: " + ChatColor.YELLOW + getVariantText(spec));
		lore.add(ChatColor.DARK_GRAY + "Purchased with BANKED money");
		lore.add(ChatColor.RED + "Spawn only");
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createUnlocksItem(Player player) {
		ItemStack item = new ItemStack(Material.BOOK);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.GOLD + "Your Unlocks / Requirements");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Banked: " + ChatColor.GOLD + "$" + formatMoney(moneyService.getBanked(player.getUniqueId())));
		for (ToolTier tier : ToolTier.values()) {
			if (tier == ToolTier.WOOD || tier == ToolTier.STONE) {
				lore.add(ChatColor.GRAY + formatTier(tier) + ": " + ChatColor.GREEN + "Unlocked");
				continue;
			}
			boolean unlocked = toolVendorService.isTierUnlocked(player, tier);
			lore.add(ChatColor.GRAY + formatTier(tier) + ": " + (unlocked ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Locked"));
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createButton(Material material, String name, String loreText) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(name);
		List<String> lore = new ArrayList<>();
		lore.add(loreText);
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

	private String formatTier(ToolTier tier) {
		return switch (tier) {
			case WOOD -> "Wood";
			case STONE -> "Stone";
			case IRON -> "Iron";
			case DIAMOND -> "Diamond";
			case NETHERITE -> "Netherite";
		};
	}

	private String formatType(ToolType type) {
		return switch (type) {
			case PICKAXE -> "Pickaxe";
			case AXE -> "Axe";
			case SHOVEL -> "Shovel";
			case SWORD -> "Sword";
		};
	}

	private String getVariantText(ToolSpec spec) {
		return switch (spec.getVariant()) {
			case STANDARD -> "Standard";
			case EFFICIENCY -> "Efficiency " + getEfficiencyLevel(spec.getTier());
			case SILK_TOUCH -> "Silk Touch";
		};
	}

	private String formatVariantSuffix(ToolSpec spec) {
		return switch (spec.getVariant()) {
			case STANDARD -> "";
			case EFFICIENCY -> ChatColor.WHITE + " (" + ChatColor.YELLOW + "Efficiency" + ChatColor.WHITE + ")";
			case SILK_TOUCH -> ChatColor.WHITE + " (" + ChatColor.YELLOW + "Silk Touch" + ChatColor.WHITE + ")";
		};
	}

	private int getEfficiencyLevel(ToolTier tier) {
		return switch (tier) {
			case WOOD -> 2;
			case STONE -> 2;
			case IRON -> 3;
			case DIAMOND -> 4;
			case NETHERITE -> 5;
		};
	}

	private String formatMoney(long value) {
		return String.format("%,d", value);
	}
}
