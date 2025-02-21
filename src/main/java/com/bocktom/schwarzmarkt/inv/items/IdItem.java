package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public abstract class IdItem extends AbstractItem {


	public int id;
	public ItemStack item;

	public IdItem(ItemStack item) {
		this.id = -1;
		this.item = item;
	}

	public IdItem(int id, ItemStack item) {
		this.id = id;
		this.item = item;
	}

	@Override
	public ItemProvider getItemProvider() {
		return new ItemBuilder(item);
	}
}
