package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class CloseItem extends AbstractItem {

	private final ItemStack item;

	public CloseItem(ItemStack item) {
		this.item = item;
	}

	@Override
	public ItemProvider getItemProvider() {
		return new ItemBuilder(item);
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
		player.closeInventory();
	}
}
