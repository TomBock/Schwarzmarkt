package com.bocktom.schwarzmarkt.util;

import org.bukkit.inventory.ItemStack;

public class PlayerDbItem extends DbItem {

	public boolean inAuction = false;

	public PlayerDbItem(int id, ItemStack item, boolean inAuction, int amount) {
		super(id, item, amount);
		this.inAuction = inAuction;
	}
}
