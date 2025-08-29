package com.bocktom.schwarzmarkt.inv;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerAuction extends Auction {

	public final UUID ownerId;
	public final int minBid;
	public final int deposit;
	public final int itemId;

	public PlayerAuction(int id, int itemId, ItemStack item, UUID ownerId, int minBid, int deposit, int highestBid, UUID highestBidder) {
		super(id, item, highestBid, highestBidder);
		this.ownerId = ownerId;
		this.itemId = itemId;
		this.minBid = minBid;
		this.deposit = deposit;
	}

	@Override
	public boolean isServerAuction() {
		return false;
	}

	@Override
	public boolean isPlayerAuction() {
		return true;
	}
}
