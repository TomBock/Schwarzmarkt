package com.bocktom.schwarzmarkt.util;

import org.bukkit.inventory.ItemStack;

public class DbItem {
	public int id;
	public ItemStack item;
	public int amount;

	public DbItem(int id, ItemStack item, int amount) {
		this.id = id;
		this.item = item;
		this.amount = amount;
	}
}
