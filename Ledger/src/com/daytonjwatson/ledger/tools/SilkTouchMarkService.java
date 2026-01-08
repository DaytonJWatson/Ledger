package com.daytonjwatson.ledger.tools;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class SilkTouchMarkService {
	private static final double SELL_MULTIPLIER = 0.80;
	private final NamespacedKey silkTouchKey;

	public SilkTouchMarkService(JavaPlugin plugin) {
		this.silkTouchKey = new NamespacedKey(plugin, "silkTouchDrop");
	}

	public void markSilkTouchDrop(ItemStack item) {
		if (item == null) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}
		meta.getPersistentDataContainer().set(silkTouchKey, PersistentDataType.BYTE, (byte) 1);
		item.setItemMeta(meta);
	}

	public boolean isSilkTouchDrop(ItemStack item) {
		if (item == null) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return false;
		}
		Byte value = meta.getPersistentDataContainer().get(silkTouchKey, PersistentDataType.BYTE);
		return value != null && value == (byte) 1;
	}

	public double getSellMultiplier(ItemStack item) {
		return isSilkTouchDrop(item) ? SELL_MULTIPLIER : 1.0;
	}
}
