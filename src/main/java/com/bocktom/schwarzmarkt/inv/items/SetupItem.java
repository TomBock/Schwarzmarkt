package com.bocktom.schwarzmarkt.inv.items;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class SetupItem extends PickableItem {

	public int amount = 1;
	protected int amountLoreIndex;
	private String amountLoreRaw;
	private int loreStartIndex;
	private int loreLength;

	public SetupItem(int id, ItemStack item, int amount, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, onAdded, tryRemove);
		this.amount = amount;

		addLore();
	}

	public static SetupItem empty(Function<PickableItem, Boolean> tryAdd, Function<PickableItem, Boolean> onRemoved) {
		return new ServerSetupItem(-1, new ItemStack(Material.AIR), 1, tryAdd, onRemoved);
	}

	@Override
	public void handleClick(@NotNull ClickType clicktype, @NotNull Player player, @NotNull InventoryClickEvent event) {
		if(clicktype.isLeftClick()) {
			super.handleClick(clicktype, player, event);
			return;
		}
		if(clicktype.isRightClick()) {
			handleRightClick(event);
		}
	}

	protected void handleRightClick(@NotNull InventoryClickEvent event) {
		if(item == null || item.getType() == Material.AIR)
			return;

		int change = event.isShiftClick() ? getSubtraction() : getAddition();

		if(amount + change < 0)
			return;
		amount += change;

		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();

		String formattedLore = amountLoreRaw;
		lore.set(amountLoreIndex, formattedLore.replace("%amount%", String.valueOf(amount)));

		meta.setLore(lore);
		item.setItemMeta(meta);
		notifyWindows();
	}

	protected abstract int getAddition();
	protected abstract int getSubtraction();

	@Override
	protected void handlePlace(@NotNull InventoryClickEvent event) {
		super.handlePlace(event);
		addLore();
	}

	@Override
	protected boolean handlePickup(@NotNull InventoryClickEvent event) {
		item = getCleanItem();
		return super.handlePickup(event);
	}

	protected void addLore() {
		ItemMeta meta = item.getItemMeta();
		if(meta == null)
			return;
		List<String> lore = meta.getLore();
		if(lore == null)
			lore = new ArrayList<>();

		List<String> raw = getRawLore();

		loreStartIndex = lore.size();
		loreLength = raw.size();

		for (String line : raw) {
			if(line.contains("%amount%")) {
				amountLoreIndex = lore.size();
				amountLoreRaw = line;
				lore.add(line.replace("%amount%", String.valueOf(amount)));
			} else {
				lore.add(line);
			}
		}

		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	protected void cleanLore() {
		ItemMeta meta = item.getItemMeta();
		if(meta == null)
			return;
		List<String> lore = meta.getLore();

		for (int length = loreLength; length > 0; length--) {
			if(lore.size() <= loreStartIndex)
				break;
			lore.remove(loreStartIndex);
		}

		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	protected abstract List<String> getRawLore();

	public ItemStack getCleanItem() {
		cleanLore();
		return item;
	}
}
