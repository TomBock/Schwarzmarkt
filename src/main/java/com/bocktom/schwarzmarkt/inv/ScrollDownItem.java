package com.bocktom.schwarzmarkt.inv;

import org.bukkit.Material;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.ScrollItem;

public class ScrollDownItem extends ScrollItem {

	public ScrollDownItem() {
		super(1);
	}

	@Override
	public ItemProvider getItemProvider(ScrollGui<?> gui) {
		ItemBuilder builder = new ItemBuilder(Material.FEATHER);
		builder.setDisplayName("§e§l▼");
		if(!gui.canScroll(1))
			builder.addLoreLines("§7Du kannst nicht weiter nach unten scrollen");
		return builder;
	}
}
