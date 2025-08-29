package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class AuctionItem extends IdItem {

	private final Consumer<AuctionItem> clickHandler;
	public final int currentBid;
	public abstract boolean isServerAuction(); // Explicit since its used regularly
	public boolean isPlayerAuction() { return !isServerAuction(); } // cuz im lazy

	public AuctionItem(int id) {
		super(id, null);
		clickHandler = item -> {};
		currentBid = 0;
	}

	public AuctionItem(int id, ItemStack item, int currentBid, Consumer<AuctionItem> clickHandler) {
		super(id, item);
		this.clickHandler = clickHandler;
		this.currentBid = currentBid;
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
		clickHandler.accept(this);
	}
}
