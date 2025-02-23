package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.ScrollItem;

import java.util.List;

public class ScrollUpItem extends ScrollItem {

	private final ItemStack item;

	public ScrollUpItem(ItemStack item) {
		super(-1);
		this.item = item;
	}

	@Override
	public ItemProvider getItemProvider(ScrollGui<?> gui) {
		ItemStack item = this.item.clone();
		if(gui.canScroll(-1))
			item.setLore(List.of());
		return new ItemBuilder(item);
	}
}
