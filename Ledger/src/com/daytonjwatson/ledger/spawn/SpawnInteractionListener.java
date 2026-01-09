package com.daytonjwatson.ledger.spawn;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.gui.GuiManager;
import com.daytonjwatson.ledger.gui.MenuId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SpawnInteractionListener implements Listener {
	private final SpawnRegionService spawnRegionService;
	private final GuiManager guiManager;
	private final Set<Material> hubBlocks;

	public SpawnInteractionListener(SpawnRegionService spawnRegionService, GuiManager guiManager, ConfigManager configManager) {
		this.spawnRegionService = spawnRegionService;
		this.guiManager = guiManager;
		this.hubBlocks = loadHubBlocks(configManager);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if(event.getPlayer().isOp()) return;
		
		if (spawnRegionService.isInSpawn(event.getBlock().getLocation())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "You cannot build at spawn.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if(event.getPlayer().isOp()) return;
		
		if (spawnRegionService.isInSpawn(event.getBlock().getLocation())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "You cannot break blocks at spawn.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if(event.getEntity() instanceof Player && event.getEntity().isOp()) return;
		
		if (spawnRegionService.isInSpawn(event.getEntity().getLocation())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Player && event.getEntity().isOp()) return;
		
		Entity target = event.getEntity();
		if (spawnRegionService.isInSpawn(target.getLocation())) {
			event.setCancelled(true);
			if (event.getDamager() instanceof Player player) {
				player.sendMessage(ChatColor.RED + "No combat at spawn.");
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (hubBlocks.isEmpty()) {
			return;
		}
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (event.getClickedBlock() == null) {
			return;
		}
		if (!spawnRegionService.isInSpawn(event.getClickedBlock().getLocation())) {
			return;
		}
		if (!hubBlocks.contains(event.getClickedBlock().getType())) {
			return;
		}
		event.setCancelled(true);
		guiManager.open(MenuId.HUB, event.getPlayer());
	}

	private Set<Material> loadHubBlocks(ConfigManager configManager) {
		if (configManager == null || configManager.getSpawn() == null) {
			return Set.of();
		}
		List<String> entries = configManager.getSpawn().getStringList("hubBlocks");
		if (entries == null || entries.isEmpty()) {
			return Set.of();
		}
		Set<Material> materials = new HashSet<>();
		for (String entry : entries) {
			if (entry == null || entry.isBlank()) {
				continue;
			}
			Material material = Material.matchMaterial(entry.trim());
			if (material != null) {
				materials.add(material);
			}
		}
		return Set.copyOf(materials);
	}
}
