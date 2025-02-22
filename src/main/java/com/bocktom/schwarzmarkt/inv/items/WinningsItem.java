package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class WinningsItem extends PickableItem {

	private boolean isTitle;
	private String titlePerm;
	private String title;

	public WinningsItem(int id, ItemStack item, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, onAdded, tryRemove);

		isTitle = InvUtil.isTitleItem(item);
		if(isTitle) {
			ItemMeta meta = item.getItemMeta();
			List<String> lore = meta.getLore();
			title = meta.getDisplayName();
			titlePerm = lore.getLast();
			lore.clear();
			lore.add(MSG.get("winnings.title.lore"));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	}

	@Override
	protected void handlePickup(@NotNull InventoryClickEvent event) {
		super.handlePickup(event);

		if(isTitle && !event.isCancelled()) {
			event.setCancelled(true);

			Player player = (Player) event.getWhoClicked();
			PermissionAttachment perm = player.addAttachment(Schwarzmarkt.plugin);
			perm.setPermission(titlePerm, true);
			player.sendMessage(MSG.get("winnings.title.onclick", "%titel%", title));

			// Remove item
			Bukkit.getScheduler().runTask(Schwarzmarkt.plugin, () -> {
				event.getClickedInventory().setItem(event.getSlot(), null);
				player.updateInventory(); // Force update
			});
		}
	}
}
