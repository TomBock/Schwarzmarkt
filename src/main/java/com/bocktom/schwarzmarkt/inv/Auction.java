package com.bocktom.schwarzmarkt.inv;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Auction {

	public final int id;
	public final ItemStack item;
	public final int highestBid;
	public final UUID highestBidder;

	public Auction(int id, ItemStack item, int highestBid, UUID highestBidder) {
		this.id = id;
		this.item = item;
		this.highestBid = highestBid;
		this.highestBidder = highestBidder;
	}
}
