package com.bocktom.schwarzmarkt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {
	private static final LegacyComponentSerializer legacyAmpersand = LegacyComponentSerializer.legacyAmpersand();
	private static final LegacyComponentSerializer legacySection = LegacyComponentSerializer.legacySection();

	public static List<String> getLore(ItemMeta meta) {
		if(meta.getLore() == null)
			return new ArrayList<>();

		return meta.getLore();
	}

	public static void setLoreWithoutEvents(ItemMeta meta, List<String> lore) {
		if(lore == null || lore.isEmpty()) {
			meta.lore(new ArrayList<>());
			return;
		}

		meta.setLore(lore);

		// Remove paper added click events from the components
		meta.lore(removeClickEvents(meta.lore()));
	}

	public static List<Component> removeClickEvents(List<Component> components) {
		if(components == null || components.isEmpty())
			return components;

		List<Component> newComponents = new ArrayList<>();

		for (Component c : components) {
			c = c.clickEvent(null).children(c.children().stream().map(child -> {
				child = child.clickEvent(null);
				return child;
			}).toList());
			newComponents.add(c);
		}
		return newComponents;
	}

	public static String removeLegacyColorCodes(String input) {
		return input.replaceAll("(?i)[&§][0-9a-fk-or]", "");
	}
}
