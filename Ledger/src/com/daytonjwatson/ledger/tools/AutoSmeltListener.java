package com.daytonjwatson.ledger.tools;

import com.daytonjwatson.ledger.upgrades.UpgradeService;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class AutoSmeltListener implements Listener {
	private final UpgradeService upgradeService;

	public AutoSmeltListener(UpgradeService upgradeService) {
		this.upgradeService = upgradeService;
	}

	@EventHandler
	public void onBlockDrop(BlockDropItemEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}
		int level = Math.min(3, upgradeService.getHighestRefinementLevel(player.getUniqueId()));
		if (level <= 0) {
			return;
		}
		double chance = getSmeltChance(level);
		if (ThreadLocalRandom.current().nextDouble() > chance) {
			return;
		}
		for (Item itemEntity : event.getItems()) {
			ItemStack stack = itemEntity.getItemStack();
			Material smelted = getSmeltedOutput(stack.getType());
			if (smelted == null) {
				continue;
			}
			itemEntity.setItemStack(new ItemStack(smelted, stack.getAmount()));
		}
	}

	private double getSmeltChance(int level) {
		return switch (Math.max(1, Math.min(3, level))) {
			case 1 -> 0.25;
			case 2 -> 0.5;
			default -> 0.75;
		};
	}

	private Material getSmeltedOutput(Material input) {
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
