package com.daytonjwatson.ledger.tools;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class SilkTouchMarkListener implements Listener {
	private final SilkTouchMarkService silkTouchMarkService;

	public SilkTouchMarkListener(SilkTouchMarkService silkTouchMarkService) {
		this.silkTouchMarkService = silkTouchMarkService;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Player player = event.getPlayer();
		ItemStack tool = player.getInventory().getItemInMainHand();
		if (tool == null || !tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
			return;
		}
		var drops = event.getBlock().getDrops(tool, player);
		if (drops.isEmpty()) {
			return;
		}
		event.setDropItems(false);
		for (ItemStack drop : drops) {
			silkTouchMarkService.markSilkTouchDrop(drop);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
		}
	}
}
