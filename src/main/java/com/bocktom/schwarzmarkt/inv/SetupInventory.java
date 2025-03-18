package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.*;
import com.bocktom.schwarzmarkt.util.DbItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;

import java.util.*;

public class SetupInventory extends ConfigInventory {

	private ArrayList<Integer> itemsRemoved = new ArrayList<>();

	public SetupInventory(Player player) {
		super(player, "setup", MSG.get("setup.name"));
	}

	@Override
	protected List<Item> getItems() {
		List<DbItem> dbItems = Schwarzmarkt.db.getItems();

		List<Item> items = InvUtil.createItems(dbItems,
				dbItem -> new SetupItem(dbItem.id, dbItem.item, dbItem.amount, this::tryAddItem, this::tryRemoveItem));

		// One fallback to always have space for new items
		int additions = Math.max(40 - items.size(), (items.size() % 8) + 8);
		for (int i = 0; i < additions; i++) {
			items.add(getFallback());
		}
		return items;
	}

	@Override
	protected Item getFallback() {
		return SetupItem.empty(this::tryAddItem, this::tryRemoveItem);
	}

	private boolean tryAddItem(IdItem item) {
		return true;
	}

	private boolean tryRemoveItem(IdItem item) {
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

		Schwarzmarkt.db.updateItems(itemsAdded, itemsUpdated, itemsRemoved);
	}
}
