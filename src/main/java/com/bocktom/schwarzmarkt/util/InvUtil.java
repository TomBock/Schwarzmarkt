package com.bocktom.schwarzmarkt.util;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InvUtil {

	public static Item BORDER = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(""));
	public static Item AIR = new SimpleItem(new ItemBuilder(Material.AIR));

	public static List<Item> createItems(Map<Integer, ItemStack> itemStacks, Function<Map.Entry<Integer, ItemStack>, Item> itemCreator) {
		return itemStacks.entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.map(itemCreator)
				.collect(Collectors.toList());
	}

	public static <T> List<Item> createItems(List<T> auctions, Function<T, Item> itemCreator) {
		return auctions.stream()
				.map(itemCreator)
				.collect(Collectors.toList());
	}

	public static String getName(ItemStack item) {
		return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
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

	public static ItemStack createTitleItem(String title, String perm) {
		ItemStack item = new ItemStack(Material.NAME_TAG);
		item.editMeta(meta -> {
			meta.displayName(Component.text(ChatColor.translateAlternateColorCodes('&', title)));
			meta.lore(Collections.singletonList(Component.text("§7" + perm)));
		});
		return item;
	}

	public static boolean isTitleItem(ItemStack item) {
		return item.getType() == Material.NAME_TAG && item.hasItemMeta() && item.getItemMeta().hasLore();
	}

	public static String getTitlePerm(ItemStack item) {
		return item.getItemMeta().getLore().getLast();
	}
}
