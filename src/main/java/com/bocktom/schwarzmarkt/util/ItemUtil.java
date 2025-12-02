package com.bocktom.schwarzmarkt.util;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemUtil {
	private static final LegacyComponentSerializer legacyAmpersand = LegacyComponentSerializer.legacyAmpersand();

	public static List<String> getLegacyLore(ItemMeta meta) {
		if(!meta.hasLore())
			return new ArrayList<>();

		return new ArrayList<>(Objects.requireNonNull(meta.lore()).stream().map(legacyAmpersand::serialize).toList());
	}

	public static void setLegacyLore(ItemMeta meta, List<String> lore) {
		meta.lore(new ArrayList<>(lore.stream().map(legacyAmpersand::deserialize).toList()));
	}

	public static String removeLegacyColorCodes(String input) {
		return input.replaceAll("(?i)[&§][0-9a-fk-or]", "");
	}
}
