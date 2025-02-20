package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.util.Config;
import com.bocktom.schwarzmarkt.util.InvUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WinningsInventory {

	private static String title = "Schwarzmarkt - Gewinne";

	public WinningsInventory(Player player) {

		//noinspection unchecked
		Map<Integer, ItemStack> itemStackMap = loadWinnings(player);
		Map<Integer, Item> itemMap = InvUtil.createItems(itemStackMap, entry -> new WinningsItem(entry.getKey(), entry.getValue()));

		List<Item> items = new ArrayList<>();

		for (Map.Entry<Integer, Item> item : itemMap.entrySet()) {
			items.add(item.getValue());
		}
		for (int i = 0; i < 9; i++) {
			items.add(new WinningsItem(i, new ItemStack(Material.AIR)));
		}

		Gui gui = Gui.normal()
				.setStructure(
						"3 4 5",
						"2 0 1",
						"6 7 8")
				.addIngredient('0', items.get(0))
				.addIngredient('1', items.get(1))
				.addIngredient('2', items.get(2))
				.addIngredient('3', items.get(3))
				.addIngredient('4', items.get(4))
				.addIngredient('5', items.get(5))
				.addIngredient('6', items.get(6))
				.addIngredient('7', items.get(7))
				.addIngredient('8', items.get(8))
				.build();

		Window window = Window.single()
				.setViewer(player)
				.setTitle(title)
				.setGui(gui)
				.addCloseHandler(this::onClose)
				.build();

		window.open();
	}

	private Map<Integer, ItemStack> loadWinnings(Player player) {
		Map<Integer, ItemStack> winnings = new HashMap<>();

		ConfigurationSection config = Config.winnings.get.getConfigurationSection(player.getUniqueId().toString());
		if (config == null) return winnings;

		for (String key : config.getKeys(false)) {
			try {
				int slot = Integer.parseInt(key);
				ItemStack item = config.getItemStack(key);
				winnings.put(slot, item);
			} catch (NumberFormatException e) {
				Schwarzmarkt.plugin.getLogger().warning("Invalid slot key in config: " + key);
			}
		}
		return winnings;
	}

	private void onClose() {

	}
}
