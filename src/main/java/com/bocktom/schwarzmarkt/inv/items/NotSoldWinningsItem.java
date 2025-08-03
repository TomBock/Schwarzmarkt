package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

public class NotSoldWinningsItem extends WinningsItem {

	public NotSoldWinningsItem(int id, ItemStack item, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, onAdded, tryRemove);
	}
}
