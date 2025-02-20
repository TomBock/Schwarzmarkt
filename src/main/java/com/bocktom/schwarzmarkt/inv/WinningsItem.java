package com.bocktom.schwarzmarkt.inv;

import com.bocktom.schwarzmarkt.util.Config;
import com.bocktom.schwarzmarkt.util.InvUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class WinningsItem extends AbstractItem {

	private final int key;
	private final ItemStack item;

	public WinningsItem(Integer key, ItemStack item) {
		this.item = item;
		this.key = key;
	}

	@Override
	public ItemProvider getItemProvider() {
		return new ItemBuilder(item);
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
		if(InvUtil.isPickupAction(event.getAction())) {
			if(event.getCurrentItem() != null) {

				event.setCancelled(false);
				Config.winnings.get.set(player.getUniqueId().toString() + "." + key, null);
				Config.winnings.save();
			}
		}
	}
}
