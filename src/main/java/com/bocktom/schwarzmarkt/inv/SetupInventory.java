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

public class SetupInventory extends ConfigInventory {

	private final List<ItemStack> itemsAdded = new ArrayList<>();
	private final List<Integer> itemsRemoved = new ArrayList<>();

	public SetupInventory(Player player) {
		super(player, "setup", MSG.get("setup.name"));
	}

	@Override
	protected List<Item> getItems() {
		Map<Integer, ItemStack> itemMap = Schwarzmarkt.db.getItems();

		List<Item> items = InvUtil.createItems(itemMap,
				entry -> new PickableItem(entry.getKey(), entry.getValue(), this::tryAddItem, this::tryRemoveItem));

		// One fallback to always have space for new items
		items.add(getFallback());
		return items;
	}

	@Override
	protected Item getFallback() {
		return PickableItem.empty(this::tryAddItem, this::tryRemoveItem);
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

	@Override
	protected void onClose() {
		Schwarzmarkt.db.updateItems(itemsAdded, itemsRemoved);
	}
}
