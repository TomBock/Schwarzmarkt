package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.util.InvUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.InvUI;

import java.util.function.Function;

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
	public void handleClick(@NotNull ClickType clicktype, @NotNull Player player, @NotNull InventoryClickEvent event) {

		if(InvUtil.isPlaceAction(event.getAction(), isPartialClickAllowed())) {

			handlePlace(event);
			notifyWindows();

		} else if(InvUtil.isPickupAction(event.getAction(), isPartialClickAllowed())) {

			handlePickup(event);
			notifyWindows();

			removeSlotDataFromClonedItem(event);
		}
	}

	/**
	 * Removes slot data that InvUI leaves in. Also, items are cloned by InvUI on notifyWindows,
	 * so we need to load the item directly from the inventory slot.
	 */
	private void removeSlotDataFromClonedItem(@NotNull InventoryClickEvent event) {
		ItemStack invUiClonedItem = event.getInventory().getItem(event.getSlot());
		if(invUiClonedItem != null) {
			ItemMeta meta = invUiClonedItem.getItemMeta();
			meta.getPersistentDataContainer().remove(SLOT_KEY);
			invUiClonedItem.setItemMeta(meta);
		}
	}

	protected boolean isPartialClickAllowed() {
		return true;
	}

	protected void handlePlace(@NotNull InventoryClickEvent event) {
		ItemStack previous = getCleanItem();
		item = event.getCursor();
		if(tryAdd != null && tryAdd.apply(this)) {
			event.setCancelled(true);
			event.setCursor(new ItemStack(Material.AIR));
			if(previous != null && previous.getType() != Material.AIR) {
				Bukkit.getScheduler().runTask(Schwarzmarkt.plugin, () -> {
					event.getWhoClicked().getInventory().addItem(previous);
					notifyWindows();
				});
			}
		} else {
			item = previous;
		}
	}

	protected boolean handlePickup(@NotNull InventoryClickEvent event) {
		if(tryRemove != null && tryRemove.apply(this)) {
			event.setCancelled(false);
			id = -1;
			Bukkit.getScheduler().runTask(Schwarzmarkt.plugin, () -> {
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
