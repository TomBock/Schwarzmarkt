package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.CloseItem;
import com.bocktom.schwarzmarkt.inv.items.ScrollDownItem;
import com.bocktom.schwarzmarkt.inv.items.ScrollUpItem;
import com.bocktom.schwarzmarkt.util.Config;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public abstract class ConfigInventory {

	protected final List<Item> items;
	protected int currentItem;

	protected final Player player;

	public ConfigInventory(Player player, String configName, String title) {
		this.player = player;
		items = getItems();

		Config.InternalConfig config = Config.gui;
		String type = config.get.getString(configName + ".type");
		String[] structure = config.get.getStringList(configName + ".structure").toArray(String[]::new);

		Gui gui;
		if(Objects.equals(type, "scroll")) {

			// Prefill items list cause it needs to be created beforehand
			int invSize = structure.length * 9;
			List<Item> items = new ArrayList<>(this.items);

			currentItem = items.size();
			while(items.size() < invSize) {
				items.add(getNextItem());
			}

			ScrollGui.Builder<Item> guiBuilder = ScrollGui.items()
					.setStructure(structure)
					.addIngredient('i', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
					.setContent(items);

			getItemsFromConfig(configName, config, guiBuilder::addIngredient);
			gui = guiBuilder.build();
		} else {
			Gui.Builder.Normal guiBuilder = Gui.normal()
					.setStructure(structure)
					.addIngredient('i', this::getNextItem);

			getItemsFromConfig(configName, config, guiBuilder::addIngredient);
			gui = guiBuilder.build();
		}

		Window window = Window.single()
				.setViewer(player)
				.setTitle(title)
				.setGui(gui)
				.addCloseHandler(this::onClose)
				.build();

		window.open();
	}

	private void getItemsFromConfig(String configName, Config.InternalConfig inv, BiConsumer<Character, Item> handler) {
		for (String slot : inv.get.getConfigurationSection("slots").getKeys(false)) {
			ItemStack itemStack = inv.get.getItemStack("slots." + slot);
			if(itemStack == null) {
				Schwarzmarkt.plugin.getLogger().warning(configName + "-inventory: Invalid item configured in slot " + slot);
				itemStack = new ItemStack(Material.AIR);
			}

			Item item = switch (slot) {
				case "c" -> new CloseItem(itemStack);
				case "u" -> new ScrollUpItem(itemStack);
				case "d" -> new ScrollDownItem(itemStack);
				default -> new SimpleItem(itemStack);
			};
			handler.accept(slot.charAt(0), item);
		}
	}

	protected abstract List<Item> getItems();

	protected Item getFallback() {
		return new SimpleItem(new ItemStack(Material.AIR));
	}

	protected Item getNextItem() {
		if(currentItem >= items.size()) {
			return getFallback();
		}
		return items.get(currentItem++);
	}

	protected void onClose() {

	}
}
