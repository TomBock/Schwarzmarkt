package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.util.InvUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.InvUI;

import java.util.function.Function;

/**
 * An item the player can physically place into and take out of the gui.
 * <p>
 * InvUI 2 no longer hands the {@code InventoryClickEvent} to items - {@link Click} only
 * carries the player and the click type, and every click on an item is cancelled by the
 * library. Place and pickup are therefore driven off the player's cursor here instead of
 * off {@code InventoryAction}, and the item is moved explicitly rather than by letting
 * vanilla run through an uncancelled event.
 */
public class PickableItem extends IdItem {
	public static final NamespacedKey SLOT_KEY = new NamespacedKey(InvUI.getInstance().getPlugin(), "slot");

	protected final Function<PickableItem, Boolean> tryAdd;
	protected final Function<PickableItem, Boolean> tryRemove;

	public PickableItem(int id, ItemStack item, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item);
		this.tryAdd = onAdded;
		this.tryRemove = tryRemove;
	}

	@Override
	public void handleClick(@NotNull ClickType clicktype, @NotNull Player player, @NotNull Click click) {

		ItemStack cursor = player.getItemOnCursor();
		boolean hasCursor = cursor != null && cursor.getType() != Material.AIR;

		if(hasCursor && InvUtil.isPlaceClick(clicktype, isPartialClickAllowed())) {

			handlePlace(player, cursor);
			notifyWindows();

		} else if(!hasCursor && InvUtil.isPickupClick(clicktype, isPartialClickAllowed())) {

			handlePickup(player);
			notifyWindows();
		}
	}

	/**
	 * Strips the slot marker that InvUI stamps onto rendered items.
	 * <p>
	 * {@code AbstractWindow} writes a "slot" key into the item's persistent data - InvUI 1
	 * via ItemMeta, InvUI 2 via {@code ItemStack#editPersistentDataContainer} - namespaced
	 * with the plugin InvUI was initialised with, so it shows up as
	 * {@code phoenixschwarzmarkt:slot}. Without stripping it the marker travels with the
	 * item into player inventories and, from there, back into the database.
	 */
	protected ItemStack stripInternalData(ItemStack stack) {
		if(stack == null || stack.getType() == Material.AIR)
			return stack;

		ItemMeta meta = stack.getItemMeta();
		if(meta == null)
			return stack;

		meta.getPersistentDataContainer().remove(SLOT_KEY);
		stack.setItemMeta(meta);
		return stack;
	}

	protected boolean isPartialClickAllowed() {
		return true;
	}

	protected void handlePlace(@NotNull Player player, @NotNull ItemStack cursor) {
		ItemStack previous = stripInternalData(getCleanItem());
		// Stripped on the way in as well, so the marker never reaches the database
		item = stripInternalData(cursor.clone());
		if(tryAdd != null && tryAdd.apply(this)) {
			player.setItemOnCursor(null);
			if(previous != null && previous.getType() != Material.AIR) {
				Bukkit.getScheduler().runTask(Schwarzmarkt.plugin, () -> {
					player.getInventory().addItem(previous);
					notifyWindows();
				});
			}
		} else {
			item = previous;
		}
	}

	protected boolean handlePickup(@NotNull Player player) {
		if(tryRemove != null && tryRemove.apply(this)) {
			// Subclasses clean the item (e.g. strip setup lore) before delegating here
			ItemStack picked = stripInternalData(item == null ? null : item.clone());
			id = -1;
			Bukkit.getScheduler().runTask(Schwarzmarkt.plugin, () -> {
				if(picked != null && picked.getType() != Material.AIR) {
					player.setItemOnCursor(picked);
				}
				item = new ItemStack(Material.AIR);
				notifyWindows();
			});
			return true;
		}
		return false;
	}

	public ItemStack getCleanItem() {
		return item;
	}
}
