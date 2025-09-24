package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.inv.items.IdItem;
import com.bocktom.schwarzmarkt.inv.items.NotSoldWinningsItem;
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
		super(player, player.getUniqueId(), "winnings", MSG.get("winnings.name"));
	}

	@Override
	protected List<Item> getItems() {
		Map<Integer, ItemStack> itemStackMap = Schwarzmarkt.db.getWinnings(player.getUniqueId());
		Map<Integer, ItemStack> nonSoldItems = Schwarzmarkt.db.getNotSold(player.getUniqueId());
		List<Item> winnings = InvUtil.createItems(itemStackMap,
				entry -> new WinningsItem(entry.getKey(), entry.getValue(), null, this::tryItemRemove));
		List<Item> notSold = InvUtil.createItems(nonSoldItems,
				entry -> new NotSoldWinningsItem(entry.getKey(), entry.getValue(), null, this::tryItemRemove));
		winnings.addAll(notSold);
		return winnings;
	}

	private boolean tryItemRemove(IdItem item) {
		// Not the cleanest but should work
		boolean removed;
		if(item instanceof NotSoldWinningsItem) {
			removed = Schwarzmarkt.db.removeNotSold(item.id);
		} else {
			removed = Schwarzmarkt.db.removeWinnings(item.id);
		}
		if(!removed) {
			player.sendMessage(MSG.get("error"));
			return false;
		}
		return true;
	}
}
