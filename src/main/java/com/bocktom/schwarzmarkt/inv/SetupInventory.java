package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.inv.items.IdItem;
import com.bocktom.schwarzmarkt.inv.items.SetupItem;
import com.bocktom.schwarzmarkt.util.DbItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;

import java.util.ArrayList;
import java.util.List;

public abstract class SetupInventory extends ConfigInventory {

	protected ArrayList<Integer> itemsRemoved = new ArrayList<>();

	public SetupInventory(Player player, String setup, String name) {
		super(player, setup, name);
	}


	@Override
	protected Item getFallback() {
		return SetupItem.empty(this::tryAddItem, this::tryRemoveItem);
	}

	protected boolean tryAddItem(IdItem item) {
		return true;
	}

	protected boolean tryRemoveItem(IdItem item) {
		if(item.id > 0 && !itemsRemoved.contains(item.id))
			itemsRemoved.add(item.id);
		return true;
	}

	@Override
	protected void onClose() {
		List<DbItem> itemsAdded = new ArrayList<>();
		List<DbItem> itemsUpdated = new ArrayList<>();
		for (Item item : items) {
			SetupItem idItem = (SetupItem) item;

			if(idItem.item.getType() != Material.AIR) {

				ItemStack itemStack = idItem.getCleanItem();
				DbItem dbItem = new DbItem(idItem.id, itemStack, idItem.amount);

				if(idItem.id > 0 && !itemsRemoved.contains(idItem.id)) {
					itemsUpdated.add(dbItem);
				} else {
					itemsAdded.add(dbItem);
				}
			}
		}


	}

	protected abstract void saveItems(List<DbItem> itemsAdded, List<DbItem> itemsUpdated);
}
