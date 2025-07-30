package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.MSG;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerAuctionItem extends AuctionItem {

	public PlayerAuctionItem(int id, ItemStack item, UUID ownerId, int minId, int deposit, int currentBid, int highestBid, Consumer<AuctionItem> clickHandler) {
		super(id, item, currentBid, clickHandler);

		fillItemLore(item, ownerId, minId, currentBid, highestBid);
	}

	private void fillItemLore(ItemStack item, UUID ownerId, int minId, int currentBid, int highestBid) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		// add lore
		if (lore == null)
			lore = new ArrayList<>();

		lore.clear();

		boolean hasBids = highestBid > 0;

		List<String> raw = MSG.getList("auction.item.lore.player");
		for (String line : raw) {
			if(line.contains("%meingebot%")) {
				if(currentBid > 0) {
					lore.add(line.replace("%meingebot%", String.valueOf(currentBid)));
					continue;
				}
			}
			if(line.contains("%besitzer%")) {
				String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
				lore.add(line.replace("%besitzer%", ownerName != null ? ownerName : ""));
				continue;
			}

			if(hasBids && line.contains("%gebotspanne%")) {
				lore.add(line.replace("%gebotspanne%", getBidRange(highestBid)));
				continue;
			}
			if(!hasBids && line.contains("%mindestgebot%")) {
				lore.add(line.replace("%mindestgebot%", String.valueOf(minId)));
				continue;
			}
			lore.add(line);
		}

		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	private String getBidRange(int value) {
		if (value < 100) return "1-100";

		int magnitude = (int) Math.pow(10, (int) Math.log10(value) - 1);

		int step = magnitude * 10;

		int base = (value / step) * step;
		int upper = base + step;
		return base + "-" + upper;
	}
}
