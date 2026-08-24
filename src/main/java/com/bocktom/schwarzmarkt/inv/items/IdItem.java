package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

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
	public ItemProvider getItemProvider(@NotNull Player viewer) {
		return new ItemBuilder(item);
	}
}
