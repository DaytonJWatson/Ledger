package com.daytonjwatson.ledger.mobs;

import com.daytonjwatson.ledger.economy.MoneyService;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MobKillListener implements Listener {
	private static final String SPAWNER_META = "profitSpawner";
	private final MobPayoutService mobPayoutService;
	private final MoneyService moneyService;
	private final Map<UUID, KillContext> killContexts = new HashMap<>();

	public MobKillListener(MobPayoutService mobPayoutService, MoneyService moneyService) {
		this.mobPayoutService = mobPayoutService;
		this.moneyService = moneyService;
	}

	@EventHandler
	public void onSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
			Entity entity = event.getEntity();
			entity.setMetadata(SPAWNER_META, new FixedMetadataValue(JavaPlugin.getProvidingPlugin(getClass()), true));
		}
	}

	@EventHandler
	public void onKill(EntityDeathEvent event) {
		Player killer = event.getEntity().getKiller();
		if (killer == null) {
			return;
		}
		EntityType type = event.getEntityType();
		String key = "entity:" + type.name();
		double payout = mobPayoutService.getPayout(key);
		if (payout <= 0.0) {
			return;
		}
		double multiplier = 1.0;
		if (event.getEntity().hasMetadata(SPAWNER_META)) {
			multiplier *= 0.20;
		}
		KillContext context = killContexts.computeIfAbsent(killer.getUniqueId(), ignored -> new KillContext());
		Chunk chunk = event.getEntity().getLocation().getChunk();
		long now = System.currentTimeMillis();
		if (context.lastChunk != null && context.lastChunk.equals(chunk) && now - context.lastKillTime < 45000) {
			multiplier *= 0.60;
		}
		context.lastChunk = chunk;
		context.lastKillTime = now;
		long earned = Math.round(payout * multiplier);
		if (earned <= 0) {
			return;
		}
		mobPayoutService.recordKill(key);
		moneyService.addCarried(killer, earned);
		killer.sendMessage(ChatColor.GREEN + "Mob payout: $" + earned);
	}

	private static class KillContext {
		private Chunk lastChunk;
		private long lastKillTime;
	}
}
