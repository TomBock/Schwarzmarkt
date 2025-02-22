package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.*;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

import java.util.*;

public class SetupInventory {

	private List<ItemStack> itemsAdded = new ArrayList<>();
	private List<Integer> itemsRemoved = new ArrayList<>();

	public SetupInventory(Player player) {
		Map<Integer, ItemStack> itemMap = Schwarzmarkt.db.getItems();

		List<Item> items = InvUtil.createItems(itemMap,
				entry -> new PickableItem(entry.getKey(), entry.getValue(), this::tryAddItem, this::tryRemoveItem));

		int rows = Math.max((int) Math.ceil(items.size() / 8.0), 5);
		int emptySlots = (rows+1) * 8 - items.size(); // always one more row to have empty space

		while(emptySlots-- > 0) {
			items.add(PickableItem.empty(this::tryAddItem, this::tryRemoveItem));
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
				.setTitle(MSG.get("setup.name"))
				.setGui(gui)
				.addCloseHandler(this::onClose)
				.build();

		window.open();
	}

	private boolean tryAddItem(IdItem item) {
		return itemsAdded.add(item.item);
	}

	private boolean tryRemoveItem(IdItem item) {
		if(item.id >= 0)
			itemsRemoved.add(item.id);

		// Check if its a new one
		itemsAdded.remove(item.item);
		return true;
	}

	private void onClose() {
		Schwarzmarkt.db.updateItems(itemsAdded, itemsRemoved);
	}
}
