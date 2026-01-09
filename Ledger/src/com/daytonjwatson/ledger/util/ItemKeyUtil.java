package com.daytonjwatson.ledger.util;

import org.bukkit.Material;

import java.util.Locale;

public final class ItemKeyUtil {
	private ItemKeyUtil() {
	}

	public static String toKey(Material material) {
		if (material == null) {
			return null;
		}
		return material.name().toUpperCase(Locale.ROOT);
	}

	public static String normalizeKey(String key) {
		if (key == null) {
			return null;
		}
		String trimmed = key.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		Material material = Material.matchMaterial(trimmed);
		if (material != null) {
			return toKey(material);
		}
		int namespaceIndex = trimmed.indexOf(':');
		if (namespaceIndex >= 0 && namespaceIndex < trimmed.length() - 1) {
			String namespace = trimmed.substring(0, namespaceIndex).toLowerCase(Locale.ROOT);
			if (namespace.equals("minecraft")) {
				String withoutNamespace = trimmed.substring(namespaceIndex + 1);
				Material namespacedMaterial = Material.matchMaterial(withoutNamespace);
				if (namespacedMaterial != null) {
					return toKey(namespacedMaterial);
				}
				trimmed = withoutNamespace;
			}
		}
		return trimmed.toUpperCase(Locale.ROOT);
	}
}
