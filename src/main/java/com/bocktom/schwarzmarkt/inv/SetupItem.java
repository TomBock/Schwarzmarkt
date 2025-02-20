package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.util.InvUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class SetupItem extends AbstractItem {

	private ItemStack item;

	public SetupItem(ItemStack item) {
		this.item = item;
	}

	public static SetupItem empty() {
		return new SetupItem(new ItemStack(Material.AIR));
	}

	@Override
	public ItemProvider getItemProvider() {
		return new ItemBuilder(item);
	}

	@Override
	public void handleClick(@NotNull ClickType clicktype, @NotNull Player player, @NotNull InventoryClickEvent event) {
		if(!InvUtil.isOnRightBorder(event.getSlot())) {

			if(InvUtil.isPlaceAction(event.getAction())) {
				event.setCancelled(true);
				item = event.getCursor();
				event.setCursor(new ItemStack(Material.AIR));
			} else if(InvUtil.isPickupAction(event.getAction())) {
				event.setCancelled(false);
			}
		}
		notifyWindows();
	}
}
