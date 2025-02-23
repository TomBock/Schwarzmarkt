package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.IdItem;
import com.bocktom.schwarzmarkt.inv.items.WinningsItem;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;

import java.util.List;
import java.util.Map;

public class WinningsInventory extends ConfigInventory {

	public WinningsInventory(Player player) {
		super(player, "winnings", MSG.get("winnings.name"));
	}

	@Override
	protected List<Item> getItems() {
		Map<Integer, ItemStack> itemStackMap = Schwarzmarkt.db.getWinnings(player.getUniqueId());
		return InvUtil.createItems(itemStackMap,
				entry -> new WinningsItem(entry.getKey(), entry.getValue(), null, this::tryItemRemove));
	}

	private boolean tryItemRemove(IdItem item) {
		boolean removed = Schwarzmarkt.db.removeWinnings(item.id);
		if(!removed) {
			player.sendMessage(MSG.get("error"));
		}
		return true;
	}
}
