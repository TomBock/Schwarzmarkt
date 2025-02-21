package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.util.InvUtil;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class PickableItem extends IdItem {

	private final Function<PickableItem, Boolean> tryAdd;
	private final Function<PickableItem, Boolean> tryRemove;

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
		if(!InvUtil.isOnRightBorder(event.getSlot())) {

			if(InvUtil.isPlaceAction(event.getAction())) {

				ItemStack previous = item;
				item = event.getCursor();
				if(tryAdd != null && tryAdd.apply(this)) {
					event.setCancelled(true);
					event.setCursor(new ItemStack(Material.AIR));
				} else {
					item = previous;
				}

			} else if(InvUtil.isPickupAction(event.getAction())) {
				if(tryRemove != null && tryRemove.apply(this)) {
					event.setCancelled(false);
					id = -1;
				}
			}
		}
		notifyWindows();
	}
}
