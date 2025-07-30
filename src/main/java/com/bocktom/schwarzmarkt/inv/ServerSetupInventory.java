package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.ServerSetupItem;
import com.bocktom.schwarzmarkt.util.DbItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.List;

public class ServerSetupInventory extends SetupInventory {

	public ServerSetupInventory(Player player) {
		super(player, "setup", MSG.get("setup.name"));
	}

	@Override
	protected List<Item> getItems() {
		List<DbItem> dbItems = Schwarzmarkt.db.getItems();

		List<Item> items = InvUtil.createItems(dbItems,
				dbItem -> new ServerSetupItem(dbItem.id, dbItem.item, dbItem.amount, this::tryAddItem, this::tryRemoveItem));

		// One fallback to always have space for new items
		int additions = Math.max(40 - items.size(), (items.size() % 8) + 8);
		for (int i = 0; i < additions; i++) {
			items.add(getFallback());
		}
		return items;
	}

	@Override
	protected void saveItems(List<DbItem> itemsAdded, List<DbItem> itemsUpdated) {
		Schwarzmarkt.db.updateItems(itemsAdded, itemsUpdated, itemsRemoved);
	}
}
