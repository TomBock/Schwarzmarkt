package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SetupItem extends PickableItem {

	public static SetupItem empty(Function<PickableItem, Boolean> tryAdd, Function<PickableItem, Boolean> onRemoved) {
		return new SetupItem(-1, new ItemStack(Material.AIR), tryAdd, onRemoved);
	}

	public int amount = 1;
	private int amountLoreIndex;

	public SetupItem(int id, ItemStack item, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, onAdded, tryRemove);
	}

	public SetupItem(int id, ItemStack item, int amount, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, onAdded, tryRemove);
		this.amount = amount;
		item.setAmount(amount);

		addLore();
	}

	@Override
	public void handleClick(@NotNull ClickType clicktype, @NotNull Player player, @NotNull InventoryClickEvent event) {
		if(clicktype.isLeftClick()) {
			super.handleClick(clicktype, player, event);
			return;
		}
		if(clicktype.isRightClick()) {
			if(item == null || item.getType() == Material.AIR)
				return;

			int change = event.isShiftClick() ? -1 : +1;
			amount += change;

			if(amount == 0)
				amount = 1;

			ItemMeta meta = item.getItemMeta();
			List<String> lore = meta.getLore();
			lore.set(amountLoreIndex, "§7Anzahl: §e" + amount);
			meta.setLore(lore);
			item.setItemMeta(meta);
			item.setAmount(amount);
			notifyWindows();
		}
	}

	@Override
	protected void handlePlace(@NotNull InventoryClickEvent event) {
		super.handlePlace(event);
		addLore();
	}

	@Override
	protected boolean handlePickup(@NotNull InventoryClickEvent event) {
		item = getCleanItem();
		item.setAmount(1);
		return super.handlePickup(event);
	}

	private void addLore() {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		if(lore == null)
			lore = new ArrayList<>();
		lore.add("");
		lore.add("§7Anzahl: §e" + amount);
		amountLoreIndex = lore.size() - 1;
		lore.add("§7Rechtsklick: §7+1");
		lore.add("§7Shift+Rechtsklick: §7-1");
		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	private void cleanLore() {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		lore.remove(amountLoreIndex + 2);
		lore.remove(amountLoreIndex + 1);
		lore.remove(amountLoreIndex);
		lore.remove(amountLoreIndex - 1);
		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	public ItemStack getCleanItem() {
		cleanLore();
		item.setAmount(1);
		return item;
	}
}
