package com.bocktom.schwarzmarkt.util;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerDbItem extends OwnedDbItem {

	public boolean inAuction = false;

	public PlayerDbItem(int id, UUID ownerUuid, String ownerName, ItemStack item, int minBid, int deposit, boolean inAuction) {
		super(id, ownerUuid, ownerName, item, minBid, deposit);
		this.inAuction = inAuction;
	}
}
