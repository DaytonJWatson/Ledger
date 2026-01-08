package com.daytonjwatson.ledger.spawn;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SpawnInteractionListener implements Listener {
	private final SpawnRegionService spawnRegionService;

	public SpawnInteractionListener(SpawnRegionService spawnRegionService) {
		this.spawnRegionService = spawnRegionService;
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (spawnRegionService.isInSpawn(event.getBlock().getLocation())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "You cannot build at spawn.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (spawnRegionService.isInSpawn(event.getBlock().getLocation())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "You cannot break blocks at spawn.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (spawnRegionService.isInSpawn(event.getEntity().getLocation())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity target = event.getEntity();
		if (spawnRegionService.isInSpawn(target.getLocation())) {
			event.setCancelled(true);
			if (event.getDamager() instanceof Player player) {
				player.sendMessage(ChatColor.RED + "No combat at spawn.");
			}
		}
	}
}
