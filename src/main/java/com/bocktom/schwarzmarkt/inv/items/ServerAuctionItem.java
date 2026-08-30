package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.ItemUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.function.Consumer;

public class ServerAuctionItem extends AuctionItem {

	public ServerAuctionItem(int id) {
		super(id);
	}

	public ServerAuctionItem(int id, ItemStack item, int currentBid, Consumer<AuctionItem> clickHandler) {
		super(id, item, currentBid, clickHandler);

		fillItemLore(item, currentBid);
	}

	private void fillItemLore(ItemStack item, int currentBid) {
		ItemMeta meta = item.getItemMeta();

		List<String> lore = ItemUtil.getLore(meta);

		// A title carries its permission in the last lore line, which must never reach the
		// player - its lore is replaced wholesale by the title block from the config.
		boolean isTitle = InvUtil.isTitleItem(item);
		if(isTitle) {
			lore.clear();
		}

		List<String> raw = MSG.getList(isTitle ? "auction.item.lore.title" : "auction.item.lore.server");
		for (String line : raw) {
			if(line.contains("%meingebot%")) {
				if(currentBid > 0) {
					lore.add(line.replace("%meingebot%", String.valueOf(currentBid)));
					continue;
				}
			} else {
				lore.add(line);
			}
		}

		ItemUtil.setLoreWithoutEvents(meta, lore);
		item.setItemMeta(meta);
	}

	@Override
	public boolean isServerAuction() {
		return true;
	}
}
