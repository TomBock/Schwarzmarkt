package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.PlayerSetupItem;
import com.bocktom.schwarzmarkt.util.DbItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.List;

public class PlayerSetupInventory extends SetupInventory {

	public PlayerSetupInventory(Player player) {
		super(player, "setup", MSG.get("setup.name"));
	}

	@Override
	protected List<Item> getItems() {
		List<DbItem> dbItems = Schwarzmarkt.db.getPlayerItems(player.getUniqueId());

		List<Item> items = InvUtil.createItems(dbItems,
				dbItem -> new PlayerSetupItem(dbItem.id, dbItem.item, dbItem.amount, this::tryAddItem, this::tryRemoveItem));

		// One fallback to always have space for new items
		int additions = Math.max(40 - items.size(), (items.size() % 8) + 8);
		for (int i = 0; i < additions; i++) {
			items.add(getFallback());
		}
		return items;
	}

	@Override
	protected void saveItems(List<DbItem> itemsAdded, List<DbItem> itemsUpdated) {
		Schwarzmarkt.db.updatePlayerItems(player.getUniqueId(), itemsAdded, itemsUpdated, itemsRemoved);
	}
}
