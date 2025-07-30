package com.bocktom.schwarzmarkt.util;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class OwnedDbItem extends DbItem {

	public UUID ownerUuid;

	public OwnedDbItem(int id, UUID ownerUuid, ItemStack item, int amount) {
		super(id, item, amount);
		this.ownerUuid = ownerUuid;
	}
}
