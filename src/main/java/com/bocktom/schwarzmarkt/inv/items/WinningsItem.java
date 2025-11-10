package com.bocktom.schwarzmarkt.inv.items;

import com.bocktom.schwarzmarkt.Schwarzmarkt;
import com.bocktom.schwarzmarkt.util.InvUtil;
import com.bocktom.schwarzmarkt.util.MSG;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class WinningsItem extends PickableItem {

	private boolean isTitle;
	private boolean isTitleAssigned;

	private String titlePerm;
	private String title;

	public WinningsItem(int id, ItemStack item, Function<PickableItem, Boolean> onAdded, Function<PickableItem, Boolean> tryRemove) {
		super(id, item, onAdded, tryRemove);

		isTitle = InvUtil.isTitleItem(item);
		if(isTitle) {
			ItemMeta meta = item.getItemMeta();
			List<String> lore = meta.getLore();
			title = meta.getDisplayName();
			titlePerm = lore.getLast().replace("§7", "");
			lore.clear();
			lore.add(MSG.get("winnings.title.lore"));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	}

	@Override
	protected boolean handlePickup(@NotNull InventoryClickEvent event) {
		if(isTitle) {
			return handleTitle(event);
		} else {
			return super.handlePickup(event);
		}
	}

	private boolean handleTitle(@NotNull InventoryClickEvent event) {
		event.setCancelled(true);
		Player player = (Player) event.getWhoClicked();

		if(isTitleAssigned) {
			player.sendMessage(MSG.get("winnings.title.assigned"));
			return false;
		}

		grantPermission(player).thenAccept(result -> {
			if(!result) {
				player.sendMessage(MSG.get("error"));
				return;
			}

			if(!super.handlePickup(event))
				return;

			player.sendMessage(MSG.get("winnings.title.onclick", "%titel%", title));
			isTitleAssigned = true;
			event.setCancelled(true);

			// Double removal of titles for duplicate item glitch
			// see https://discord.com/channels/506865081162661919/1436816608344539156
			//Bukkit.getScheduler().runTaskLater(Schwarzmarkt.plugin, () -> {
			//	player.getInventory().removeItem(item);
			//}, 10L);
		});
		return true;
	}

	private CompletableFuture<Boolean> grantPermission(Player player) {
		User permUser = Schwarzmarkt.perms.getUserManager().getUser(player.getUniqueId());
		if(permUser == null)
			return CompletableFuture.completedFuture(false);

		permUser.data().add(Node.builder(titlePerm).value(true).build());
		CompletableFuture<Void> future = Schwarzmarkt.perms.getUserManager().saveUser(permUser);
		return future.thenApply(v -> true);
	}

	@Override
	protected boolean isPartialClickAllowed() {
		return false;
	}
}
