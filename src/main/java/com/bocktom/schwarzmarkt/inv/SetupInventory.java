package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.util.InvUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;

public class SetupInventory {

	public static String title = "Schwarzmarkt - SETUP";

	private final Player player;

	public SetupInventory(Player player) {
		List<ItemStack> itemStacks = (List<ItemStack>) Schwarzmarkt.config.items.getList("items");

		List<Item> items = InvUtil.createItems(itemStacks);

		int size = 8 * 5;
		int emptySlots = size - items.size();
		emptySlots += 8 - (emptySlots % 8); // fill line

		for (int i = 0; i < emptySlots; i++) {
			items.add(SetupItem.empty());
		}

		Gui gui = ScrollGui.items()
				.setStructure(
						"x x x x x x x x u",
						"x x x x x x x x #",
						"x x x x x x x x e",
						"x x x x x x x x #",
						"x x x x x x x x d")
				.addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
				.addIngredient('#', InvUtil.BORDER)
				.addIngredient('a', InvUtil.AIR)
				.addIngredient('u', new ScrollUpItem())
				.addIngredient('d', new ScrollDownItem())
				.addIngredient('e', new CloseItem())
				.setContent(items)
				.build();

		Window window = Window.single()
				.setViewer(player)
				.setTitle(title)
				.setGui(gui)
				.addCloseHandler(this::onClose)
				.build();

		window.open();
		this.player = player;
	}

	private void onClose() {
		Inventory inv = player.getOpenInventory().getTopInventory();
		List<ItemStack> items = new ArrayList<>();

		for (int i = 0; i < inv.getSize(); i++) {
			ItemStack item = inv.getItem(i);

			// Skip GUI
			if(InvUtil.isOnRightBorder(i))
				continue;

			if(item == null || item.getType() == Material.AIR)
				continue;

			items.add(item);
		}
		Schwarzmarkt.config.items.set("items", items);
		Schwarzmarkt.config.save();
	}
}
