package com.daytonjwatson.ledger.tools;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ToolMetaService {
	private final NamespacedKey repairKey;

	public ToolMetaService(JavaPlugin plugin) {
		this.repairKey = new NamespacedKey(plugin, "repairCount");
	}

	public int getRepairCount(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return 0;
		}
		Integer count = meta.getPersistentDataContainer().get(repairKey, PersistentDataType.INTEGER);
		return count == null ? 0 : count;
	}

	public void incrementRepairCount(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}
		int count = getRepairCount(item) + 1;
		meta.getPersistentDataContainer().set(repairKey, PersistentDataType.INTEGER, count);
		item.setItemMeta(meta);
	}
}
