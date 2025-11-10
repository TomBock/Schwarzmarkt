package com.bocktom.schwarzmarkt.util;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class OwnedDbItem extends DbItem {

	public UUID ownerUuid;
	public String ownerName;
	public int deposit;
	public int minBid;

	public OwnedDbItem(int id, UUID ownerUuid, String ownerName, ItemStack item, int minBid, int deposit) {
		super(id, item, minBid);
		this.ownerUuid = ownerUuid;
		this.ownerName = ownerName;
		this.minBid = minBid;
		this.deposit = deposit;
	}
}
