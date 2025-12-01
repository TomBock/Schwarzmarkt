package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.ItemUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

		List<String> lore = ItemUtil.getLegacyLore(meta);

		if(InvUtil.isTitleItem(item)) {
			lore.clear();
		}

		List<String> raw = MSG.getList("auction.item.lore.server");
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

		ItemUtil.setLegacyLore(meta, lore);
		item.setItemMeta(meta);
	}

	@Override
	public boolean isServerAuction() {
		return true;
	}
}
