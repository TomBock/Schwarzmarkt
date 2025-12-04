package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.ItemUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import com.bocktom.schwarzmarkt.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerAuctionItem extends AuctionItem {

	public UUID ownerUuid;
	public String ownerName;
	public int minBid;

	public PlayerAuctionItem(int id, ItemStack item, UUID ownerId, String ownerName, int minBid, int deposit, int currentBid, int highestBid, Consumer<AuctionItem> clickHandler) {
		super(id, item, currentBid, clickHandler);

		this.ownerUuid = ownerId;
		this.ownerName = ownerName;
		this.minBid = minBid;
		fillItemLore(item, ownerId, minBid, currentBid, highestBid);
	}

	private void fillItemLore(ItemStack item, UUID ownerId, int minBid, int currentBid, int highestBid) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = ItemUtil.getLore(meta);

		boolean hasBids = highestBid > 0;

		boolean hideLine = false;
		List<String> raw = MSG.getList("auction.item.lore.player");
		for (String line : raw) {
			hideLine = false;
			if(line.contains("%meingebot%")) {
				if(currentBid > 0) {
					lore.add(line.replace("%meingebot%", String.valueOf(currentBid)));
					continue;
				}
				hideLine = true;
			}
			if(line.contains("%besitzer%")) {
				String playerName = StringUtil.isNullOrEmpty(ownerName) ? Bukkit.getOfflinePlayer(ownerId).getName() : ownerName;
				lore.add(line.replace("%besitzer%", playerName != null ? playerName : "Unbekannt"));
				continue;
			}

			if(line.contains("%gebotspanne%")) {
				if(hasBids) {
					lore.add(line.replace("%gebotspanne%", getBidRange(highestBid)));
					continue;

				} else {
					hideLine = true;
				}
			}
			if(line.contains("%mindestgebot%")) {
				if(!hasBids) {
					lore.add(line.replace("%mindestgebot%", String.valueOf(minBid)));
					continue;
				} else {
					hideLine = true;
				}
			}
			if(!hideLine) {
				lore.add(line);
			}
		}

		ItemUtil.setLoreWithoutEvents(meta, lore);
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

	@Override
	public boolean isServerAuction() {
		return false;
	}
}
