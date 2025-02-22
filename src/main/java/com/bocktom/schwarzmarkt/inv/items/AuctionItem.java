package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.InvUtil;
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

public class AuctionItem extends IdItem {

	private final Consumer<AuctionItem> clickHandler;
	public final int currentBid;

	public AuctionItem(int id) {
		super(id, null);
		clickHandler = item -> {};
		currentBid = 0;
	}

	public AuctionItem(int id, ItemStack item, int currentBid, Consumer<AuctionItem> clickHandler) {
		super(id, item);
		this.clickHandler = clickHandler;
		this.currentBid = currentBid;

		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		// add lore
		if(lore == null)
			lore = new ArrayList<>();

		boolean isTitle = InvUtil.isTitleItem(item);
		if(isTitle)
			lore.clear();
		lore.addFirst("");
		if(currentBid > 0)
			lore.addFirst(MSG.get("auction.item.lore.currentbid", "%amount%", String.valueOf(currentBid)));
		if(isTitle)
			lore.addFirst(MSG.get("auction.item.lore.title"));
		lore.addFirst(MSG.get("auction.item.lore.info"));

		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
		clickHandler.accept(this);
	}
}
