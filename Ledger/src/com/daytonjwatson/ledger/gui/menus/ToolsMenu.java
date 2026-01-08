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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ToolsMenu implements LedgerMenu {
	private static final int MENU_SIZE = 54;
	private static final int[] OFFER_SLOTS = {
		10, 11, 12, 13, 14, 15, 16,
		19, 20, 21, 22, 23, 24, 25,
		28, 29, 30, 31, 32, 33, 34,
		37, 38, 39, 40, 41, 42, 43
	};
	private static final int PAGE_SIZE = OFFER_SLOTS.length;
	private static final Map<ToolCategory, Integer> TAB_SLOTS = Map.of(
		ToolCategory.PICKAXE, 1,
		ToolCategory.AXE, 3,
		ToolCategory.MISC, 4,
		ToolCategory.SHOVEL, 5,
		ToolCategory.SWORD, 7
	);
	private static final List<Material> MISC_ITEMS = List.of(
		Material.FLINT_AND_STEEL,
		Material.SHEARS,
		Material.BUCKET,
		Material.FISHING_ROD,
		Material.BRUSH,
		Material.BUNDLE,
		Material.COMPASS,
		Material.CLOCK,
		Material.SPYGLASS,
		Material.ELYTRA,
		Material.LEAD,
		Material.MAP,
		Material.ENDER_PEARL,
		Material.SADDLE,
		Material.CARROT_ON_A_STICK,
		Material.WARPED_FUNGUS_ON_A_STICK,
		Material.OAK_BOAT
	);

	private final GuiManager guiManager;
	private final MoneyService moneyService;
	private final ToolVendorService toolVendorService;
	private final SpawnRegionService spawnRegionService;
	private final ItemStack fillerItem;
	private final Map<UUID, ToolCategory> selections = new ConcurrentHashMap<>();
	private final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

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
		ToolCategory selected = selections.getOrDefault(player.getUniqueId(), ToolCategory.PICKAXE);
		for (int slot = 0; slot < 9; slot++) {
			inventory.setItem(slot, fillerItem);
		}
		for (ToolCategory category : ToolCategory.values()) {
			Integer slot = TAB_SLOTS.get(category);
			if (slot != null) {
				inventory.setItem(slot, createTabItem(category, category == selected));
			}
		}
		for (int slot = 9; slot < 45; slot++) {
			inventory.setItem(slot, fillerItem);
		}
		List<Offer> offers = buildOffers(selected);
		int page = clampPage(player.getUniqueId(), offers.size());
		int startIndex = page * PAGE_SIZE;
		for (int index = 0; index < PAGE_SIZE; index++) {
			int offerIndex = startIndex + index;
			if (offerIndex >= offers.size()) {
				break;
			}
			int slot = OFFER_SLOTS[index];
			inventory.setItem(slot, createOfferItem(player, offers.get(offerIndex)));
		}
		for (int slot = 45; slot < MENU_SIZE; slot++) {
			inventory.setItem(slot, fillerItem);
		}
		inventory.setItem(45, createButton(Material.ARROW, ChatColor.RED + "Back", ChatColor.GRAY + "Return to the hub"));
		inventory.setItem(47, createButton(Material.ARROW, ChatColor.YELLOW + "Previous Page", ChatColor.GRAY + "View more tools"));
		inventory.setItem(49, createUnlocksItem(player));
		inventory.setItem(51, createButton(Material.ARROW, ChatColor.YELLOW + "Next Page", ChatColor.GRAY + "View more tools"));
		inventory.setItem(53, createButton(Material.CLOCK, ChatColor.YELLOW + "Refresh", ChatColor.GRAY + "Reload this menu"));
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
			guiManager.open(MenuId.TOOLS, player);
			return;
		}
		if (slot == 51) {
			updatePage(player.getUniqueId(), 1);
			guiManager.open(MenuId.TOOLS, player);
			return;
		}
		if (slot == 53) {
			guiManager.open(MenuId.TOOLS, player);
			return;
		}
		ToolCategory tab = resolveTab(slot);
		if (tab != null) {
			selections.put(player.getUniqueId(), tab);
			pages.put(player.getUniqueId(), 0);
			guiManager.open(MenuId.TOOLS, player);
			return;
		}
		Offer offer = resolveOffer(player, slot);
		if (offer == null) {
			return;
		}
		if (!spawnRegionService.isInSpawn(player.getLocation())) {
			player.sendMessage(ChatColor.RED + "You can only buy tools at spawn.");
			return;
		}
		if (offer.isTool()) {
			handleToolPurchase(player, offer.getSpec());
			return;
		}
		handleMiscPurchase(player, offer.getMaterial());
	}

	private void handleToolPurchase(Player player, ToolSpec spec) {
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

	private void handleMiscPurchase(Player player, Material material) {
		long price = toolVendorService.getMiscPrice(material);
		long banked = moneyService.getBanked(player.getUniqueId());
		if (banked < price) {
			player.sendMessage(ChatColor.RED + "Not enough banked money. Cost $" + formatMoney(price) + ".");
			return;
		}
		if (!moneyService.removeBanked(player, price)) {
			player.sendMessage(ChatColor.RED + "Unable to withdraw banked money.");
			return;
		}
		player.getInventory().addItem(new ItemStack(material));
		player.sendMessage(ChatColor.GREEN + "Tool purchased for $" + formatMoney(price) + ".");
		guiManager.open(MenuId.TOOLS, player);
	}

	private ToolCategory resolveTab(int slot) {
		for (Map.Entry<ToolCategory, Integer> entry : TAB_SLOTS.entrySet()) {
			if (entry.getValue() == slot) {
				return entry.getKey();
			}
		}
		return null;
	}

	private Offer resolveOffer(Player player, int slot) {
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
		ToolCategory selected = selections.getOrDefault(player.getUniqueId(), ToolCategory.PICKAXE);
		List<Offer> offers = buildOffers(selected);
		int page = clampPage(player.getUniqueId(), offers.size());
		int offerIndex = page * PAGE_SIZE + index;
		if (offerIndex >= offers.size()) {
			return null;
		}
		return offers.get(offerIndex);
	}

	private List<Offer> buildOffers(ToolCategory category) {
		List<Offer> offers = new ArrayList<>();
		if (category.isMisc()) {
			for (Material material : MISC_ITEMS) {
				offers.add(Offer.misc(material));
			}
			return offers;
		}
		ToolVariant[] variants = {ToolVariant.STANDARD, ToolVariant.EFFICIENCY, ToolVariant.SILK_TOUCH};
		for (ToolTier tier : ToolTier.values()) {
			for (ToolVariant variant : variants) {
				offers.add(Offer.tool(new ToolSpec(category.getToolType(), tier, variant)));
			}
		}
		return offers;
	}

	private ItemStack createTabItem(ToolCategory category, boolean selected) {
		ItemStack item = new ItemStack(category.getIconMaterial());
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.YELLOW + category.getLabel());
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

	private ItemStack createOfferItem(Player player, Offer offer) {
		if (offer.isTool()) {
			return createToolOfferItem(player, offer.getSpec());
		}
		return createMiscOfferItem(offer.getMaterial());
	}

	private ItemStack createToolOfferItem(Player player, ToolSpec spec) {
		ItemStack item = new ItemStack(spec.getType().getMaterialForTier(spec.getTier()));
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		boolean unlocked = toolVendorService.isTierUnlocked(player, spec.getTier());
		ChatColor nameColor = unlocked ? ChatColor.GREEN : ChatColor.RED;
		meta.setDisplayName(nameColor + formatTier(spec.getTier()) + " " + formatType(spec.getType()) + formatVariantSuffix(spec));
		List<String> lore = new ArrayList<>();
		long price = toolVendorService.getBuyPrice(spec);
		lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + "$" + formatMoney(price));
		lore.add(ChatColor.GRAY + "Tier requirement: " + (unlocked ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Locked"));
		lore.add(ChatColor.GRAY + "Variant: " + ChatColor.YELLOW + getVariantText(spec));
		lore.add(ChatColor.DARK_GRAY + "Purchased with BANKED money");
		lore.add(ChatColor.RED + "Spawn only");
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createMiscOfferItem(Material material) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.setDisplayName(ChatColor.GREEN + formatMaterialName(material));
		List<String> lore = new ArrayList<>();
		long price = toolVendorService.getMiscPrice(material);
		lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + "$" + formatMoney(price));
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
		meta.setDisplayName(ChatColor.YELLOW + "Your Unlocks / Requirements");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Banked: " + ChatColor.GOLD + "$" + formatMoney(moneyService.getBanked(player.getUniqueId())));
		for (ToolTier tier : ToolTier.values()) {
			if (tier == ToolTier.WOOD || tier == ToolTier.STONE || tier == ToolTier.COPPER) {
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

	private int clampPage(UUID playerId, int totalOffers) {
		int maxPage = Math.max(0, (totalOffers - 1) / PAGE_SIZE);
		int page = pages.getOrDefault(playerId, 0);
		if (page < 0) {
			page = 0;
		}
		if (page > maxPage) {
			page = maxPage;
		}
		pages.put(playerId, page);
		return page;
	}

	private void updatePage(UUID playerId, int delta) {
		pages.put(playerId, Math.max(0, pages.getOrDefault(playerId, 0) + delta));
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
			case COPPER -> "Copper";
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

	private String formatMaterialName(Material material) {
		String value = material.name().toLowerCase().replace('_', ' ');
		String[] parts = value.split(" ");
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (part.isEmpty()) {
				continue;
			}
			builder.append(Character.toUpperCase(part.charAt(0)))
				.append(part.substring(1));
			if (i < parts.length - 1) {
				builder.append(' ');
			}
		}
		return builder.toString();
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
			case COPPER -> 2;
			case IRON -> 3;
			case DIAMOND -> 4;
			case NETHERITE -> 5;
		};
	}

	private String formatMoney(long value) {
		return String.format("%,d", value);
	}

	private enum ToolCategory {
		PICKAXE(ToolType.PICKAXE, "Pickaxe"),
		AXE(ToolType.AXE, "Axe"),
		MISC(null, "Misc"),
		SHOVEL(ToolType.SHOVEL, "Shovel"),
		SWORD(ToolType.SWORD, "Sword");

		private final ToolType toolType;
		private final String label;

		ToolCategory(ToolType toolType, String label) {
			this.toolType = toolType;
			this.label = label;
		}

		public boolean isMisc() {
			return toolType == null;
		}

		public ToolType getToolType() {
			return toolType;
		}

		public String getLabel() {
			return label;
		}

		public Material getIconMaterial() {
			if (toolType != null) {
				return toolType.getMaterialForTier(ToolTier.IRON);
			}
			return Material.FLINT_AND_STEEL;
		}
	}

	private static class Offer {
		private final ToolSpec spec;
		private final Material material;

		private Offer(ToolSpec spec, Material material) {
			this.spec = spec;
			this.material = material;
		}

		public static Offer tool(ToolSpec spec) {
			return new Offer(spec, null);
		}

		public static Offer misc(Material material) {
			return new Offer(null, material);
		}

		public boolean isTool() {
			return spec != null;
		}

		public ToolSpec getSpec() {
			return spec;
		}

		public Material getMaterial() {
			return material;
		}
	}
}
