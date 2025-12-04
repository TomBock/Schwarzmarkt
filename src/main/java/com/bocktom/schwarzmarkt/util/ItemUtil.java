package com.bocktom.schwarzmarkt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
		meta.lore(removeClickEventsRec(meta.lore()));
	}

	public static List<Component> removeClickEventsRec(List<Component> components) {
		if(components == null || components.isEmpty())
			return components;

		List<Component> newComponents = new ArrayList<>();

		for (Component c : components) {

			// Skip empty text components that are added by paper with the click events
			if(c instanceof TextComponent textComponent && textComponent.children().isEmpty() && textComponent.content().isEmpty())
				continue;
			newComponents.add(c.children(removeClickEventsRec(c.children())).clickEvent(null));
		}
		return newComponents;
	}

	public static String removeLegacyColorCodes(String input) {
		return input.replaceAll("(?i)[&§][0-9a-fk-or]", "");
	}
}
