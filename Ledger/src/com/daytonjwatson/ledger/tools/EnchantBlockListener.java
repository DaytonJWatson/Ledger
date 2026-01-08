package com.daytonjwatson.ledger.tools;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;

public class EnchantBlockListener implements Listener {
	@EventHandler
	public void onEnchant(EnchantItemEvent event) {
		event.setCancelled(true);
		event.getEnchanter().sendMessage(ChatColor.RED + "Enchanting is disabled.");
	}

	@EventHandler
	public void onAnvil(PrepareAnvilEvent event) {
		if (event.getViewers().isEmpty()) {
			return;
		}
		if (event.getInventory() instanceof AnvilInventory) {
			event.setResult(null);
			for (var viewer : event.getViewers()) {
				if (viewer instanceof Player player) {
					player.sendMessage(ChatColor.RED + "Anvil combining is disabled.");
				}
			}
		}
	}
}
