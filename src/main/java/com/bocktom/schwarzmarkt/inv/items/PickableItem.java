package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.util.InvUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class PickableItem extends IdItem {

	protected final Function<PickableItem, Boolean> tryAdd;
	protected final Function<PickableItem, Boolean> tryRemove;

	public PickableItem(int id, ItemStack item, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item);
		this.tryAdd = onAdded;
		this.tryRemove = tryRemove;
	}

	public static PickableItem empty(Function<PickableItem, Boolean> tryAdd, Function<PickableItem, Boolean> onRemoved) {
		return new PickableItem(-1, new ItemStack(Material.AIR), tryAdd, onRemoved);
	}

	@Override
	public void handleClick(@NotNull ClickType clicktype, @NotNull Player player, @NotNull InventoryClickEvent event) {
		//if(!InvUtil.isOnRightBorder(event.getSlot())) {

			if(InvUtil.isPlaceAction(event.getAction())) {

				handlePlace(event);

			} else if(InvUtil.isPickupAction(event.getAction())) {

				handlePickup(event);

			}
		//}
		notifyWindows();
	}


	protected void handlePlace(@NotNull InventoryClickEvent event) {
		ItemStack previous = item;
		item = event.getCursor();
		if(tryAdd != null && tryAdd.apply(this)) {
			event.setCancelled(true);
			event.setCursor(new ItemStack(Material.AIR));
		} else {
			item = previous;
		}
	}

	protected boolean handlePickup(@NotNull InventoryClickEvent event) {
		if(tryRemove != null && tryRemove.apply(this)) {
			event.setCancelled(false);
			id = -1;
			Bukkit.getScheduler().runTask(Schwarzmarkt.plugin, () -> item = new ItemStack(Material.AIR));
			return true;
		}
		return false;
	}
}
