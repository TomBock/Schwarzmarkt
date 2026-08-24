package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.List;

public class ScrollDownItem extends ScrollControlItem {

	private final ItemStack item;

	public ScrollDownItem(ItemStack item) {
		super(1);
		this.item = item;
	}

	@Override
	public ItemProvider getItemProvider(@NotNull Player viewer) {
		ItemStack item = this.item.clone();
		if(canScroll(1))
			item.setLore(List.of());
		return new ItemBuilder(item);
	}
}
