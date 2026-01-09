package com.daytonjwatson.ledger.mining;

import com.daytonjwatson.ledger.upgrades.UpgradeService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoSmeltListener implements Listener {
	private final UpgradeService upgradeService;

	public AutoSmeltListener(UpgradeService upgradeService) {
		this.upgradeService = upgradeService;
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		int refinementLevel = upgradeService.getHighestRefinementLevel(player.getUniqueId());
		if (refinementLevel <= 0) {
			return;
		}
		double chance = getAutosmeltChance(refinementLevel);
		if (chance <= 0.0 || ThreadLocalRandom.current().nextDouble() > chance) {
			return;
		}
		ItemStack tool = player.getInventory().getItemInMainHand();
		if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
			return;
		}
		var drops = event.getBlock().getDrops(tool, player);
		if (drops.isEmpty()) {
			return;
		}
		Material blockType = event.getBlock().getType();
		boolean smeltedAny = false;
		List<ItemStack> outputDrops = new ArrayList<>();
		for (ItemStack drop : drops) {
			Material output = getRefinementOutput(blockType, drop.getType());
			if (output == null) {
				outputDrops.add(drop);
				continue;
			}
			ItemStack smelted = new ItemStack(output, drop.getAmount());
			outputDrops.add(smelted);
			smeltedAny = true;
		}
		if (!smeltedAny) {
			return;
		}
		event.setDropItems(false);
		for (ItemStack drop : outputDrops) {
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
		}
	}

	private double getAutosmeltChance(int refinementLevel) {
		return switch (Math.max(1, Math.min(5, refinementLevel))) {
			case 1 -> 0.20;
			case 2 -> 0.35;
			case 3 -> 0.50;
			case 4 -> 0.65;
			default -> 0.80;
		};
	}

	private Material getRefinementOutput(Material blockType, Material dropType) {
		Material output = getRefinementOutput(blockType);
		if (output != null) {
			return output;
		}
		return getRefinementOutput(dropType);
	}

	private Material getRefinementOutput(Material input) {
		if (input == null) {
			return null;
		}
		return switch (input) {
			case IRON_ORE, DEEPSLATE_IRON_ORE, RAW_IRON -> Material.IRON_INGOT;
			case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE, RAW_GOLD -> Material.GOLD_INGOT;
			case COPPER_ORE, DEEPSLATE_COPPER_ORE, RAW_COPPER -> Material.COPPER_INGOT;
			case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
			default -> null;
		};
	}
}
