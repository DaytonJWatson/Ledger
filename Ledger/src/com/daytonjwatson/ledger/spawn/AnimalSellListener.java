package com.daytonjwatson.ledger.spawn;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class AnimalSellListener implements Listener {
	private final AnimalSellService animalSellService;

	public AnimalSellListener(AnimalSellService animalSellService) {
		this.animalSellService = animalSellService;
	}

	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		if (!(entity instanceof Animals)) {
			return;
		}
		if (!animalSellService.isInPen(entity.getLocation())) {
			return;
		}
		Player player = event.getPlayer();
		if (animalSellService.sellAnimal(player, entity.getType())) {
			event.setCancelled(true);
			entity.remove();
		}
	}
}
