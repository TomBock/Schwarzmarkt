package com.bocktom.schwarzmarkt.inv.items;

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

	public AuctionItem(int id) {
		super(id, null);
		clickHandler = item -> {};
	}

	public AuctionItem(int id, ItemStack item, Consumer<AuctionItem> clickHandler) {
		super(id, item);
		this.clickHandler = clickHandler;

		ItemMeta meta = item.getItemMeta();
		// add lore
		List<String> lore = meta.getLore();
		if(lore == null)
			lore = new ArrayList<>();

		lore.addFirst("");
		lore.addFirst(MSG.get("auction.item.lore"));
		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
		clickHandler.accept(this);
	}
}
