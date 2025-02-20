package com.bocktom.schwarzmarkt.util;

import com.bocktom.schwarzmarkt.inv.SetupItem;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.SuppliedItem;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InvUtil {

	public static Item BORDER = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""));
	public static Item AIR = new SimpleItem(new ItemBuilder(Material.AIR));

	public static List<Item> createItems(List<ItemStack> itemStacks) {
		if(itemStacks == null)
			return List.of();
		return itemStacks.stream()
				.filter(Objects::nonNull)
				.map(SetupItem::new)
				.collect(Collectors.toList());
	}

	public static boolean isPlaceAction(InventoryAction action) {
		return action == InventoryAction.PLACE_ALL
				|| action == InventoryAction.PLACE_ONE
				|| action == InventoryAction.PLACE_SOME
				|| action == InventoryAction.PLACE_FROM_BUNDLE
				|| action == InventoryAction.PLACE_ALL_INTO_BUNDLE
				|| action == InventoryAction.PLACE_SOME_INTO_BUNDLE
				|| action == InventoryAction.SWAP_WITH_CURSOR;
	}

	public static boolean isPickupAction(InventoryAction action) {
		return action == InventoryAction.PICKUP_ALL
				|| action == InventoryAction.PICKUP_HALF
				|| action == InventoryAction.PICKUP_ONE
				|| action == InventoryAction.PICKUP_SOME
				|| action == InventoryAction.COLLECT_TO_CURSOR
				|| action == InventoryAction.MOVE_TO_OTHER_INVENTORY;
	}

	public static boolean isOnRightBorder(int slot) {
		return slot % 9 == 8;
	}
}
