package com.daytonjwatson.ledger.economy;

import com.daytonjwatson.ledger.config.ConfigManager;
import com.daytonjwatson.ledger.util.AtomicFileWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MoneyService {
	private final JavaPlugin plugin;
	private final ConfigManager configManager;
	private final Map<UUID, PlayerBalance> balances = new ConcurrentHashMap<>();
	private final File playersFile;

	public MoneyService(JavaPlugin plugin, ConfigManager configManager) {
		this.plugin = plugin;
		this.configManager = configManager;
		this.playersFile = new File(plugin.getDataFolder(), "players.yml");
	}

	public void load() {
		YamlConfiguration yaml = loadWithBackup();
		if (yaml == null) {
			return;
		}
		ConfigurationSection playersSection = yaml.getConfigurationSection("players");
		if (playersSection == null) {
			return;
		}
		for (String key : playersSection.getKeys(false)) {
			ConfigurationSection entry = playersSection.getConfigurationSection(key);
			if (entry == null) {
				continue;
			}
			UUID uuid;
			try {
				uuid = UUID.fromString(key);
			} catch (IllegalArgumentException ex) {
				continue;
			}
			PlayerBalance balance = new PlayerBalance();
			balance.banked = entry.getLong("banked", 0L);
			balance.carried = entry.getLong("carried", 0L);
			balances.put(uuid, balance);
		}
	}

	public void save() {
		YamlConfiguration yaml = new YamlConfiguration();
		ConfigurationSection playersSection = yaml.createSection("players");
		for (Map.Entry<UUID, PlayerBalance> entry : balances.entrySet()) {
			ConfigurationSection playerSection = playersSection.createSection(entry.getKey().toString());
			playerSection.set("banked", entry.getValue().banked);
			playerSection.set("carried", entry.getValue().carried);
		}
		try {
			AtomicFileWriter.writeAtomically(playersFile, yaml.saveToString().getBytes());
		} catch (IOException e) {
			plugin.getLogger().warning("Failed to save players.yml: " + e.getMessage());
		}
	}

	private YamlConfiguration loadWithBackup() {
		File tmp = new File(playersFile.getParentFile(), playersFile.getName() + ".tmp");
		if (tmp.exists() && !tmp.delete()) {
			plugin.getLogger().warning("Unable to delete stale players temp file: " + tmp.getName());
		}
		YamlConfiguration primary = loadConfig(playersFile);
		if (primary != null && !primary.getKeys(false).isEmpty()) {
			return primary;
		}
		File backup = new File(playersFile.getParentFile(), playersFile.getName() + ".bak");
		YamlConfiguration backupConfig = loadConfig(backup);
		if (backupConfig != null) {
			return backupConfig;
		}
		return primary != null ? primary : new YamlConfiguration();
	}

	private YamlConfiguration loadConfig(File file) {
		if (!file.exists()) {
			return null;
		}
		try {
			String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			YamlConfiguration yaml = new YamlConfiguration();
			yaml.loadFromString(content);
			return yaml;
		} catch (IOException | InvalidConfigurationException e) {
			plugin.getLogger().warning("Failed to load " + file.getName() + ": " + e.getMessage());
			return null;
		}
	}

	public long getCarried(UUID uuid) {
		return getBalance(uuid).carried;
	}

	public long getBanked(UUID uuid) {
		return getBalance(uuid).banked;
	}

	public void addCarried(Player player, long amount) {
		if (amount <= 0) {
			return;
		}
		PlayerBalance balance = getBalance(player.getUniqueId());
		balance.carried += amount;
	}

	public boolean removeCarried(Player player, long amount) {
		if (amount <= 0) {
			return true;
		}
		PlayerBalance balance = getBalance(player.getUniqueId());
		if (balance.carried < amount) {
			return false;
		}
		balance.carried -= amount;
		return true;
	}

	public boolean removeBanked(Player player, long amount) {
		if (amount <= 0) {
			return true;
		}
		PlayerBalance balance = getBalance(player.getUniqueId());
		if (balance.banked < amount) {
			return false;
		}
		balance.banked -= amount;
		return true;
	}

	public void addBanked(Player player, long amount) {
		if (amount <= 0) {
			return;
		}
		PlayerBalance balance = getBalance(player.getUniqueId());
		balance.banked += amount;
	}

	public void applyDeathLoss(Player player) {
		PlayerBalance balance = getBalance(player.getUniqueId());
		double baseLoss = configManager.getConfig().getDouble("economy.loss.base", 0.30);
		double floor = configManager.getConfig().getDouble("economy.loss.floor", 0.10);
		double loss = Math.max(floor, baseLoss);
		long newCarried = Math.round(balance.carried * (1.0 - loss));
		balance.carried = Math.max(0, newCarried);
	}

	private PlayerBalance getBalance(UUID uuid) {
		return balances.computeIfAbsent(uuid, ignored -> new PlayerBalance());
	}

	private static class PlayerBalance {
		private long carried;
		private long banked;
	}
}
