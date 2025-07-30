package com.bocktom.schwarzmarkt.util;

import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.lang.invoke.CallSite;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InvUtil {

	private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

	public static Item BORDER = new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE));
	public static Item AIR = new SimpleItem(new ItemBuilder(Material.AIR));

	public static List<Item> createItems(Map<Integer, ItemStack> itemStacks, Function<Map.Entry<Integer, ItemStack>, Item> itemCreator) {
		return itemStacks.entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.map(itemCreator)
				.collect(Collectors.toList());
	}

	public static List<Item> createItemsNbt(Map<Integer, ReadWriteNBT> itemStacks, Function<Map.Entry<Integer, ReadWriteNBT>, Item> itemCreator) {
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

	public static Component getName(ItemStack item) {
		return item.getItemMeta().displayName() != null ? item.getItemMeta().displayName() : item.displayName();
	}

	public static boolean isPlaceAction(InventoryAction action) {
		return action == InventoryAction.PLACE_ALL
				|| action == InventoryAction.PLACE_ONE
				|| action == InventoryAction.PLACE_SOME
				|| action == InventoryAction.SWAP_WITH_CURSOR;
	}

	public static boolean isPickupAction(InventoryAction action) {
		return action == InventoryAction.PICKUP_ALL
				|| action == InventoryAction.PICKUP_HALF
				|| action == InventoryAction.PICKUP_ONE
				|| action == InventoryAction.PICKUP_SOME
				|| action == InventoryAction.MOVE_TO_OTHER_INVENTORY;
	}

	public static ItemStack createTitleItem(String title, String perm) {
		ItemStack item = new ItemStack(Material.NAME_TAG);
		item.editMeta(meta -> {
			meta.displayName(parseHexColors(title));
			meta.lore(Collections.singletonList(Component.text("§7" + perm)));
		});
		return item;
	}

	private static Component parseHexColors(String input) {
		Matcher matcher = HEX_PATTERN.matcher(input);
		StringBuffer result = new StringBuffer();

		while (matcher.find()) {
			String hexCode = matcher.group(1);
			matcher.appendReplacement(result, "<#" + hexCode + ">");
		}
		matcher.appendTail(result);

		return MiniMessage.miniMessage().deserialize(result.toString())
				.decoration(TextDecoration.ITALIC, false);
	}

	public static boolean isTitleItem(ItemStack item) {
		return item.getType() == Material.NAME_TAG && item.hasItemMeta() && item.getItemMeta().hasLore();
	}

	public static String getTitlePerm(ItemStack item) {
		return item.getItemMeta().getLore().getLast();
	}

	public static List<ItemStack> getWeighedRandomSelection(List<DbItem> items, int limit, Function<DbItem, ItemStack> itemMapper) {
		if (items.isEmpty()) return Collections.emptyList();

		// Compute total weight
		int totalWeight = items.stream().mapToInt(item -> item.amount).sum();

		List<ItemStack> selected = new ArrayList<>();
		Random random = new Random();

		while (selected.size() < limit && !items.isEmpty()) {
			int rand = random.nextInt(totalWeight); // Pick a random number within total weight
			int cumulativeWeight = 0;
			DbItem selectedItem = null;

			// Find the item corresponding to the random weight
			for (DbItem item : items) {
				cumulativeWeight += item.amount;
				if (rand < cumulativeWeight) {
					selectedItem = item;
					break;
				}
			}

			if (selectedItem != null) {
				selected.add(itemMapper.apply(selectedItem)); // Add the selected item
				totalWeight -= selectedItem.amount; // Reduce total weight
				items = new ArrayList<>(items); // Clone list to modify
				items.remove(selectedItem); // Remove the selected item to avoid duplicates
			}
		}

		return selected;
	}

	public static List<DbItem> getRandomSelection(List<OwnedDbItem> items, int limit) {
		if (items.isEmpty()) return Collections.emptyList();
		List<DbItem> selected = new ArrayList<>();

		// Select randomly from the list but only one per player (item.ownerUuid)
		Set<UUID> selectedOwners = new HashSet<>();
		Random random = new Random();

		int _exit = 100;
		int _exitCount = 0;
		while (selected.size() < limit && !items.isEmpty() && _exitCount++ < _exit) {
			int randIndex = random.nextInt(items.size());
			OwnedDbItem item = items.get(randIndex);

			if (!selectedOwners.contains(item.ownerUuid)) {
				selected.add(new DbItem(item.id, item.item, item.amount));
				selectedOwners.add(item.ownerUuid);
			}

			// Remove the item from the list to avoid duplicates
			items.remove(randIndex);
		}

		return selected;
	}
}
