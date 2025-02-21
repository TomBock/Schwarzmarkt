package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class CloseItem extends AbstractItem {

	@Override
	public ItemProvider getItemProvider() {
		return new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§cSchließen");
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
		player.closeInventory();
	}
}
