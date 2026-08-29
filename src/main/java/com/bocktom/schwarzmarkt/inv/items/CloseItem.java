package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class CloseItem extends AbstractItem {

	private final ItemStack item;

	public CloseItem(ItemStack item) {
		this.item = item;
	}

	@Override
	public ItemProvider getItemProvider(@NotNull Player viewer) {
		return new ItemBuilder(item);
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
		player.closeInventory();
	}
}
