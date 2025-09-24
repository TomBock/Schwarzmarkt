package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.inv.items.IdItem;
import com.bocktom.schwarzmarkt.inv.items.SetupItem;
import com.bocktom.schwarzmarkt.util.DbItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;

import java.util.*;

public abstract class SetupInventory extends ConfigInventory {

	protected HashMap<Integer, ItemStack> itemsRemoved = new HashMap<>();
	//protected ArrayList<IdItem> itemsRemoved = new ArrayList<>();

	public SetupInventory(Player player, UUID owner, String setup, String name) {
		super(player, owner, setup, name);
	}


	@Override
	protected Item getFallback() {
		return SetupItem.empty(this::tryAddItem, this::tryRemoveItem);
	}

	protected boolean tryAddItem(IdItem item) {
		return true;
	}

	protected boolean tryRemoveItem(IdItem item) {
		if(item.id > 0 && !itemsRemoved.containsKey(item.id))
			itemsRemoved.put(item.id, item.item.clone());
		return true;
	}

	@Override
	protected void onClose() {
		List<DbItem> itemsAdded = new ArrayList<>();
		List<DbItem> itemsUpdated = new ArrayList<>();
		sortItems(items, itemsUpdated, itemsAdded);

		saveItems(itemsAdded, itemsUpdated);
	}

	private void sortItems(List<Item> items, List<DbItem> itemsUpdated, List<DbItem> itemsAdded) {
		for (Item item : items) {
			SetupItem idItem = (SetupItem) item;

			if(idItem.item.getType() != Material.AIR) {

				ItemStack itemStack = idItem.getCleanItem();
				DbItem dbItem = new DbItem(idItem.id, itemStack, idItem.amount);

				if(idItem.id > 0 && !itemsRemoved.containsKey(idItem.id)) {
					itemsUpdated.add(dbItem);
				} else {
					itemsAdded.add(dbItem);
				}
			}
		}
	}

	protected abstract void saveItems(List<DbItem> itemsAdded, List<DbItem> itemsUpdated);
}
